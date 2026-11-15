/*
 * Aqua Network для Cloudflare Workers — бесплатно, не засыпает, данные в базе D1.
 * Привязки: D1 база "DB"; секрет (необязательно) CF_API_KEY — ключ CurseForge.
 */
const SLOTS = ['cape', 'hat', 'face', 'wings', 'back', 'pet', 'aura', 'icon'];
const NAME_RE = /^[A-Za-z0-9_]{3,16}$/;
const UUID_RE = /^[0-9a-f]{8}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{12}$/i;
const CORS = { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Headers': 'Content-Type, Authorization', 'Access-Control-Allow-Methods': 'GET, POST, OPTIONS' };

const json = (data, status = 200) => new Response(JSON.stringify(data), { status, headers: { 'Content-Type': 'application/json', ...CORS } });
const err = (status, error, message) => json({ error, message }, status);
const norm = (u) => String(u || '').toLowerCase().replace(/[^0-9a-f]/g, '').replace(/^(.{8})(.{4})(.{4})(.{4})(.{12})$/, '$1-$2-$3-$4-$5');
const hex = (buf) => [...new Uint8Array(buf)].map((b) => b.toString(16).padStart(2, '0')).join('');
const rand = (n) => hex(crypto.getRandomValues(new Uint8Array(n)));
const sha256 = async (s) => hex(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(s)));
async function hashPass(pass, salt) {
  const key = await crypto.subtle.importKey('raw', new TextEncoder().encode(pass), 'PBKDF2', false, ['deriveBits']);
  return hex(await crypto.subtle.deriveBits({ name: 'PBKDF2', hash: 'SHA-256', salt: new TextEncoder().encode(salt), iterations: 100000 }, key, 256));
}

let ready = false;
async function init(db) {
  if (ready) return;
  await db.batch([
    db.prepare('CREATE TABLE IF NOT EXISTS users (uuid TEXT PRIMARY KEY, name TEXT, cosmetics TEXT, skin_hash TEXT, model TEXT, skin TEXT, seen INTEGER)'),
    db.prepare('CREATE TABLE IF NOT EXISTS accounts (key TEXT PRIMARY KEY, name TEXT, type TEXT, uuid TEXT, salt TEXT, hash TEXT, friends TEXT, incoming TEXT, outgoing TEXT, status TEXT, seen INTEGER)'),
    db.prepare('CREATE TABLE IF NOT EXISTS tokens (hash TEXT PRIMARY KEY, account TEXT, created INTEGER)'),
  ]);
  ready = true;
}

const L = (s) => { try { return JSON.parse(s || '[]'); } catch { return []; } };
async function getAcc(db, key) {
  const a = await db.prepare('SELECT * FROM accounts WHERE key = ?').bind(key).first();
  if (!a) return null;
  return { ...a, friends: L(a.friends), incoming: L(a.incoming), outgoing: L(a.outgoing) };
}
const putAcc = (db, a) => db.prepare(`INSERT INTO accounts (key, name, type, uuid, salt, hash, friends, incoming, outgoing, status, seen) VALUES (?,?,?,?,?,?,?,?,?,?,?)
  ON CONFLICT(key) DO UPDATE SET name=excluded.name, type=excluded.type, uuid=excluded.uuid, salt=excluded.salt, hash=excluded.hash,
  friends=excluded.friends, incoming=excluded.incoming, outgoing=excluded.outgoing, status=excluded.status, seen=excluded.seen`)
  .bind(a.name.toLowerCase(), a.name, a.type, a.uuid || null, a.salt || null, a.hash || null, JSON.stringify(a.friends || []),
    JSON.stringify(a.incoming || []), JSON.stringify(a.outgoing || []), a.status || '', a.seen || Date.now());

async function issueToken(db, acc) {
  const tok = rand(32);
  await db.prepare('INSERT INTO tokens (hash, account, created) VALUES (?,?,?)').bind(await sha256(tok), acc.name.toLowerCase(), Date.now()).run();
  return tok;
}
async function authAcc(db, req) {
  const h = req.headers.get('Authorization') || '';
  if (!h.startsWith('Bearer ')) return null;
  const row = await db.prepare('SELECT account FROM tokens WHERE hash = ?').bind(await sha256(h.slice(7))).first();
  return row ? getAcc(db, row.account) : null;
}
const online = (a) => a && Date.now() - (a.seen || 0) < 150000;
async function view(db, key) {
  const a = await getAcc(db, key);
  if (!a) return { name: key, online: false };
  return { name: a.name, type: a.type, online: online(a), status: online(a) ? a.status || '' : '', seen: a.seen || 0, owner: /^mrulian_gg$/i.test(a.name) };
}

async function proxy(url, headers) {
  const r = await fetch(url, { headers, redirect: 'follow' });
  const out = new Response(r.body, r);
  Object.entries(CORS).forEach(([k, v]) => out.headers.set(k, v));
  return out;
}

export default {
  async fetch(req, env) {
    if (req.method === 'OPTIONS') return new Response(null, { status: 204, headers: CORS });
    const db = env.DB;
    if (!db) return err(500, 'nodb', 'D1 database binding "DB" is missing');
    await init(db);
    const url = new URL(req.url);
    const p = url.pathname;
    const body = async () => { try { return await req.json(); } catch { return {}; } };
    try {
      if (p === '/' || p === '/api/status') {
        const c = await db.prepare('SELECT COUNT(*) AS n FROM accounts').first();
        return json({ name: 'Aqua Network', ok: true, accounts: c ? c.n : 0, curseforge: !!env.CF_API_KEY });
      }

      // ---------- присутствие в игре, косметика, скины ----------
      if (p === '/api/presence' && req.method === 'POST') {
        const b = await body();
        if (!UUID_RE.test(b.uuid || '') || !/^[A-Za-z0-9_]{1,16}$/.test(b.name || '')) return err(400, 'bad', 'bad player');
        const id = norm(b.uuid);
        const cos = {};
        for (const s of SLOTS) if (b.cosmetics && typeof b.cosmetics[s] === 'string') cos[s] = b.cosmetics[s].slice(0, 32);
        let skin = null, skinHash = null, model = null;
        if (typeof b.skin === 'string' && b.skin.length < 200000 && atob(b.skin).slice(1, 4) === 'PNG') {
          skin = b.skin; skinHash = await sha256(b.skin); model = b.model === 'slim' ? 'slim' : 'classic';
        }
        await db.prepare(`INSERT INTO users (uuid, name, cosmetics, skin_hash, model, skin, seen) VALUES (?,?,?,?,?,?,?)
          ON CONFLICT(uuid) DO UPDATE SET name=excluded.name, cosmetics=excluded.cosmetics, seen=excluded.seen,
          skin_hash=COALESCE(excluded.skin_hash, users.skin_hash), model=COALESCE(excluded.model, users.model), skin=COALESCE(excluded.skin, users.skin)`)
          .bind(id, b.name, JSON.stringify(cos), skinHash, model, skin, Date.now()).run();
        return json({ ok: true });
      }
      if (p === '/api/users' && req.method === 'POST') {
        const b = await body();
        const raws = (b.uuids || []).slice(0, 100).filter((u) => UUID_RE.test(u));
        const out = {};
        if (raws.length) {
          const ids = raws.map(norm);
          const rows = await db.prepare(`SELECT uuid, name, cosmetics, skin_hash, model FROM users WHERE uuid IN (${ids.map(() => '?').join(',')})`).bind(...ids).all();
          const byId = Object.fromEntries((rows.results || []).map((r) => [r.uuid, r]));
          for (const raw of raws) {
            const r = byId[norm(raw)];
            if (r) out[raw] = { name: r.name, cosmetics: JSON.parse(r.cosmetics || '{}'), skin: r.skin_hash, model: r.model || 'classic', owner: /^mrulian_gg$/i.test(r.name) };
          }
        }
        return json({ users: out });
      }
      const sk = p.match(/^\/api\/skin\/([0-9a-f-]{32,36})$/i);
      if (sk) {
        const r = await db.prepare('SELECT skin FROM users WHERE uuid = ?').bind(norm(sk[1])).first();
        if (!r || !r.skin) return err(404, 'noskin', 'no skin');
        const bin = Uint8Array.from(atob(r.skin), (c) => c.charCodeAt(0));
        return new Response(bin, { headers: { 'Content-Type': 'image/png', 'Cache-Control': 'public, max-age=300', ...CORS } });
      }

      // ---------- аккаунты Aqua ----------
      if (p === '/api/account/register' && req.method === 'POST') {
        const b = await body();
        if (!NAME_RE.test(b.name || '')) return err(400, 'name', 'Nickname: 3–16 latin letters, digits or _');
        if (typeof b.password !== 'string' || b.password.length < 6 || b.password.length > 100) return err(400, 'password', 'Password must be at least 6 characters');
        if (await getAcc(db, b.name.toLowerCase())) return err(409, 'taken', 'This nickname is already taken');
        const salt = rand(16);
        const acc = { name: b.name, type: 'aqua', salt, hash: await hashPass(b.password, salt), friends: [], incoming: [], outgoing: [], seen: Date.now() };
        await putAcc(db, acc).run();
        return json({ token: await issueToken(db, acc), name: acc.name, type: acc.type });
      }
      if (p === '/api/account/login' && req.method === 'POST') {
        const b = await body();
        const acc = await getAcc(db, String(b.name || '').toLowerCase());
        if (!acc || acc.type !== 'aqua' || (await hashPass(String(b.password || ''), acc.salt)) !== acc.hash) return err(401, 'auth', 'Wrong nickname or password');
        acc.seen = Date.now();
        await putAcc(db, acc).run();
        return json({ token: await issueToken(db, acc), name: acc.name, type: acc.type });
      }
      if (p === '/api/account/ms' && req.method === 'POST') {
        const b = await body();
        const r = await fetch('https://api.minecraftservices.com/minecraft/profile', { headers: { Authorization: 'Bearer ' + String(b.accessToken || '') } });
        if (r.status !== 200) return err(401, 'mstoken', 'Microsoft token is invalid');
        const prof = await r.json();
        const old = await getAcc(db, prof.name.toLowerCase());
        // ник Microsoft всегда принадлежит владельцу лицензии
        const acc = old && old.type === 'ms' && old.uuid === prof.id ? old
          : { name: prof.name, type: 'ms', uuid: prof.id, friends: old ? old.friends : [], incoming: old ? old.incoming : [], outgoing: old ? old.outgoing : [] };
        acc.name = prof.name; acc.seen = Date.now(); acc.salt = null; acc.hash = null;
        await putAcc(db, acc).run();
        return json({ token: await issueToken(db, acc), name: acc.name, type: acc.type });
      }

      // ---------- друзья ----------
      if (p.startsWith('/api/friends')) {
        const me = await authAcc(db, req);
        if (!me) return err(401, 'auth', 'Sign in to Aqua Network');
        const myKey = me.name.toLowerCase();
        if (p === '/api/friends/status' && req.method === 'POST') {
          const b = await body();
          me.seen = Date.now(); me.status = String(b.status || '').slice(0, 60);
          await putAcc(db, me).run();
          return json({ ok: true, incoming: me.incoming.length });
        }
        if (p === '/api/friends' && req.method === 'GET') {
          me.seen = Date.now();
          await putAcc(db, me).run();
          const friends = await Promise.all(me.friends.map((k) => view(db, k)));
          friends.sort((a, b) => b.online - a.online);
          return json({ me: me.name, friends, incoming: await Promise.all(me.incoming.map((k) => view(db, k))), outgoing: await Promise.all(me.outgoing.map((k) => view(db, k))) });
        }
        const b = await body();
        const otherKey = String(b.name || '').toLowerCase();
        const other = await getAcc(db, otherKey);
        if (!other) return err(404, 'nouser', 'No Aqua player with this nickname');
        if (otherKey === myKey) return err(400, 'self', 'You cannot add yourself');
        const drop = (arr, k) => { const i = arr.indexOf(k); if (i >= 0) arr.splice(i, 1); };
        const add = (arr, k) => { if (!arr.includes(k)) arr.push(k); };
        if (p === '/api/friends/request') {
          if (!me.friends.includes(otherKey)) {
            if (me.incoming.includes(otherKey)) { drop(me.incoming, otherKey); drop(other.outgoing, myKey); add(me.friends, otherKey); add(other.friends, myKey); }
            else { add(me.outgoing, otherKey); add(other.incoming, myKey); }
          }
        } else if (p === '/api/friends/accept') {
          if (!me.incoming.includes(otherKey)) return err(400, 'norequest', 'No request from this player');
          drop(me.incoming, otherKey); drop(other.outgoing, myKey); add(me.friends, otherKey); add(other.friends, myKey);
        } else if (p === '/api/friends/decline') {
          drop(me.incoming, otherKey); drop(other.outgoing, myKey); drop(me.outgoing, otherKey); drop(other.incoming, myKey);
        } else if (p === '/api/friends/remove') {
          drop(me.friends, otherKey); drop(other.friends, myKey);
        } else return err(404, 'notfound', 'not found');
        await db.batch([putAcc(db, me), putAcc(db, other)]);
        return json({ ok: true });
      }

      // ---------- CurseForge (ключ хранится только здесь) ----------
      if (p.startsWith('/cf/')) {
        if (!env.CF_API_KEY) return err(503, 'nocf', 'CurseForge key is not configured on this server');
        return proxy('https://api.curseforge.com/' + p.slice(4) + url.search, { 'x-api-key': env.CF_API_KEY, Accept: 'application/json' });
      }
      if (p === '/cfdl') {
        const target = url.searchParams.get('url') || '';
        if (!/^https:\/\/(edge|mediafilez|media)\.forgecdn\.net\//.test(target)) return err(400, 'badurl', 'bad url');
        return proxy(target, env.CF_API_KEY ? { 'x-api-key': env.CF_API_KEY } : {});
      }
      return err(404, 'notfound', 'not found');
    } catch (e) {
      return err(500, 'server', String(e && e.message || e));
    }
  },
};
