/*
 * Aqua Network — сервер для игроков Aqua Launcher.
 *  • кто играет через лаунчер (значок черепахи у ника и в TAB)
 *  • косметика и скины игроков
 *  • прокси CurseForge: ваш API-ключ хранится только здесь (переменная CF_API_KEY)
 * Без зависимостей: node server.js  (PORT, CF_API_KEY, DATA_DIR)
 */
const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const PORT = Number(process.env.PORT) || 8080;
const CF_KEY = process.env.CF_API_KEY || '';
const DATA = process.env.DATA_DIR || path.join(__dirname, 'data');
fs.mkdirSync(path.join(DATA, 'skins'), { recursive: true });

const DB_FILE = path.join(DATA, 'users.json');
const ACC_FILE = path.join(DATA, 'accounts.json');
let users = {};
try { users = JSON.parse(fs.readFileSync(DB_FILE, 'utf8')); } catch { users = {}; }
// аккаунты Aqua: { lower(name): { name, type: 'aqua'|'ms', salt, hash, uuid, friends:[], incoming:[], outgoing:[], status, seen, tokens:[] } }
let accounts = {};
try { accounts = JSON.parse(fs.readFileSync(ACC_FILE, 'utf8')); } catch { accounts = {}; }
let dirty = false;
setInterval(() => {
  if (!dirty) return;
  fs.writeFile(DB_FILE, JSON.stringify(users), () => {});
  fs.writeFile(ACC_FILE, JSON.stringify(accounts), () => {});
  dirty = false;
}, 10000);

const NAME_RE = /^[A-Za-z0-9_]{3,16}$/;
const hashPass = (pass, salt) => crypto.scryptSync(pass, salt, 32).toString('hex');
const tokenHash = (tok) => crypto.createHash('sha256').update(tok).digest('hex');
function issueToken(acc) {
  const tok = crypto.randomBytes(32).toString('hex');
  acc.tokens = [...(acc.tokens || []).slice(-9), tokenHash(tok)];
  dirty = true;
  return tok;
}
function authAccount(req) {
  const h = req.headers.authorization || '';
  const tok = h.startsWith('Bearer ') ? h.slice(7) : '';
  if (!tok) return null;
  const th = tokenHash(tok);
  return Object.values(accounts).find((a) => (a.tokens || []).includes(th)) || null;
}
const isOnline = (a) => a && Date.now() - (a.seen || 0) < 90000;
const friendView = (lower) => {
  const a = accounts[lower];
  if (!a) return { name: lower, online: false };
  return { name: a.name, type: a.type, online: isOnline(a), status: isOnline(a) ? (a.status || '') : '', seen: a.seen || 0, owner: /^mrulian_gg$/i.test(a.name) };
};
function msProfile(accessToken) {
  return new Promise((resolve, reject) => {
    https.get('https://api.minecraftservices.com/minecraft/profile', { headers: { Authorization: 'Bearer ' + accessToken } }, (r) => {
      let b = ''; r.on('data', (c) => { b += c; }); r.on('end', () => {
        if (r.statusCode !== 200) return reject(new Error('Microsoft token is invalid'));
        try { resolve(JSON.parse(b)); } catch (e) { reject(e); }
      });
    }).on('error', reject);
  });
}

const UUID_RE = /^[0-9a-f]{8}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{12}$/i;
const norm = (u) => String(u || '').toLowerCase().replace(/[^0-9a-f]/g, '').replace(/^(.{8})(.{4})(.{4})(.{4})(.{12})$/, '$1-$2-$3-$4-$5');
const SLOTS = ['cape', 'hat', 'face', 'wings', 'back', 'pet', 'aura', 'icon'];

// простое ограничение частоты запросов по IP
const hits = new Map();
function limited(ip) {
  const now = Date.now(), h = hits.get(ip) || { n: 0, t: now };
  if (now - h.t > 60000) { h.n = 0; h.t = now; }
  h.n++; hits.set(ip, h);
  return h.n > 240;
}

function send(res, code, body, type = 'application/json') {
  res.writeHead(code, { 'Content-Type': type, 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Headers': 'Content-Type' });
  res.end(typeof body === 'string' || Buffer.isBuffer(body) ? body : JSON.stringify(body));
}

function readBody(req, limit = 300000) {
  return new Promise((resolve, reject) => {
    let size = 0; const chunks = [];
    req.on('data', (c) => { size += c.length; if (size > limit) { reject(new Error('too large')); req.destroy(); } else chunks.push(c); });
    req.on('end', () => { try { resolve(JSON.parse(Buffer.concat(chunks).toString('utf8') || '{}')); } catch (e) { reject(e); } });
    req.on('error', reject);
  });
}

function publicUser(u) {
  return { name: u.name, cosmetics: u.cosmetics || {}, skin: u.skinHash || null, model: u.model || 'classic', owner: /^mrulian_gg$/i.test(u.name || '') };
}

function proxy(res, url, headers) {
  const r = https.get(url, { headers }, (up) => {
    if (up.statusCode >= 300 && up.statusCode < 400 && up.headers.location) { up.resume(); return proxy(res, up.headers.location, {}); }
    res.writeHead(up.statusCode, { 'Content-Type': up.headers['content-type'] || 'application/octet-stream', 'Access-Control-Allow-Origin': '*',
      ...(up.headers['content-length'] ? { 'Content-Length': up.headers['content-length'] } : {}) });
    up.pipe(res);
  });
  r.on('error', () => send(res, 502, { error: 'upstream' }));
}

const server = http.createServer(async (req, res) => {
  const ip = req.headers['x-forwarded-for'] || req.socket.remoteAddress;
  if (req.method === 'OPTIONS') return send(res, 204, '');
  if (limited(ip)) return send(res, 429, { error: 'rate limit' });
  const url = new URL(req.url, 'http://x');
  try {
    if (url.pathname === '/' || url.pathname === '/api/status') {
      const online = Object.values(users).filter((u) => Date.now() - u.seen < 5 * 60000).length;
      return send(res, 200, { name: 'Aqua Network', ok: true, players: Object.keys(users).length, online, curseforge: !!CF_KEY });
    }

    if (url.pathname === '/api/presence' && req.method === 'POST') {
      const b = await readBody(req);
      if (!UUID_RE.test(b.uuid || '') || !/^[A-Za-z0-9_]{1,16}$/.test(b.name || '')) return send(res, 400, { error: 'bad player' });
      const id = norm(b.uuid);
      const u = users[id] || {};
      u.name = b.name;
      u.seen = Date.now();
      u.cosmetics = {};
      for (const s of SLOTS) if (b.cosmetics && typeof b.cosmetics[s] === 'string') u.cosmetics[s] = b.cosmetics[s].slice(0, 32);
      if (typeof b.skin === 'string' && b.skin.length < 200000) {
        const png = Buffer.from(b.skin, 'base64');
        if (png.slice(1, 4).toString() === 'PNG') {
          fs.writeFileSync(path.join(DATA, 'skins', id + '.png'), png);
          u.skinHash = crypto.createHash('sha1').update(png).digest('hex');
          u.model = b.model === 'slim' ? 'slim' : 'classic';
        }
      }
      users[id] = u; dirty = true;
      return send(res, 200, { ok: true });
    }

    if (url.pathname === '/api/users' && req.method === 'POST') {
      const b = await readBody(req, 20000);
      const out = {};
      for (const raw of (b.uuids || []).slice(0, 200)) {
        const id = norm(raw);
        if (users[id]) out[raw] = publicUser(users[id]);
      }
      return send(res, 200, { users: out });
    }

    const skin = url.pathname.match(/^\/api\/skin\/([0-9a-f-]{32,36})$/i);
    if (skin) {
      const file = path.join(DATA, 'skins', norm(skin[1]) + '.png');
      if (!fs.existsSync(file)) return send(res, 404, { error: 'no skin' });
      return send(res, 200, fs.readFileSync(file), 'image/png');
    }

    // ---------- аккаунты Aqua и друзья ----------
    if (url.pathname === '/api/account/register' && req.method === 'POST') {
      const b = await readBody(req, 5000);
      if (!NAME_RE.test(b.name || '')) return send(res, 400, { error: 'name', message: 'Nickname: 3–16 latin letters, digits or _' });
      if (typeof b.password !== 'string' || b.password.length < 6 || b.password.length > 100) return send(res, 400, { error: 'password', message: 'Password must be at least 6 characters' });
      const key = b.name.toLowerCase();
      if (accounts[key]) return send(res, 409, { error: 'taken', message: 'This nickname is already taken' });
      const salt = crypto.randomBytes(16).toString('hex');
      const acc = { name: b.name, type: 'aqua', salt, hash: hashPass(b.password, salt), friends: [], incoming: [], outgoing: [], seen: Date.now() };
      accounts[key] = acc;
      return send(res, 200, { token: issueToken(acc), name: acc.name, type: acc.type });
    }
    if (url.pathname === '/api/account/login' && req.method === 'POST') {
      const b = await readBody(req, 5000);
      const acc = accounts[String(b.name || '').toLowerCase()];
      if (!acc || acc.type !== 'aqua' || hashPass(String(b.password || ''), acc.salt) !== acc.hash) return send(res, 401, { error: 'auth', message: 'Wrong nickname or password' });
      acc.seen = Date.now();
      return send(res, 200, { token: issueToken(acc), name: acc.name, type: acc.type });
    }
    if (url.pathname === '/api/account/ms' && req.method === 'POST') {
      const b = await readBody(req, 10000);
      const prof = await msProfile(String(b.accessToken || ''));
      const key = prof.name.toLowerCase();
      // ник Microsoft всегда принадлежит владельцу лицензии
      let acc = accounts[key];
      if (!acc || acc.type !== 'ms' || acc.uuid !== prof.id) acc = accounts[key] = { name: prof.name, type: 'ms', uuid: prof.id, friends: acc ? acc.friends || [] : [], incoming: acc ? acc.incoming || [] : [], outgoing: acc ? acc.outgoing || [] : [] };
      acc.name = prof.name;
      acc.seen = Date.now();
      return send(res, 200, { token: issueToken(acc), name: acc.name, type: acc.type });
    }
    if (url.pathname.startsWith('/api/friends')) {
      const me = authAccount(req);
      if (!me) return send(res, 401, { error: 'auth', message: 'Sign in to Aqua Network' });
      const myKey = me.name.toLowerCase();
      me.friends = me.friends || []; me.incoming = me.incoming || []; me.outgoing = me.outgoing || [];
      if (req.method === 'POST' && url.pathname === '/api/friends/status') {
        const b = await readBody(req, 2000);
        me.seen = Date.now(); me.status = String(b.status || '').slice(0, 60); dirty = true;
        return send(res, 200, { ok: true, incoming: me.incoming.length });
      }
      if (req.method === 'GET' && url.pathname === '/api/friends') {
        me.seen = Date.now(); dirty = true;
        return send(res, 200, { me: me.name, friends: me.friends.map(friendView).sort((a, b) => b.online - a.online), incoming: me.incoming.map(friendView), outgoing: me.outgoing.map(friendView) });
      }
      const b = await readBody(req, 2000);
      const otherKey = String(b.name || '').toLowerCase();
      const other = accounts[otherKey];
      if (!other) return send(res, 404, { error: 'nouser', message: 'No Aqua player with this nickname' });
      if (otherKey === myKey) return send(res, 400, { error: 'self', message: 'You cannot add yourself' });
      other.friends = other.friends || []; other.incoming = other.incoming || []; other.outgoing = other.outgoing || [];
      const drop = (arr, k) => { const i = arr.indexOf(k); if (i >= 0) arr.splice(i, 1); };
      if (url.pathname === '/api/friends/request') {
        if (me.friends.includes(otherKey)) return send(res, 200, { ok: true, already: true });
        if (me.incoming.includes(otherKey)) { // встречная заявка — сразу дружим
          drop(me.incoming, otherKey); drop(other.outgoing, myKey);
          me.friends.push(otherKey); other.friends.push(myKey);
        } else if (!me.outgoing.includes(otherKey)) { me.outgoing.push(otherKey); other.incoming.push(myKey); }
      } else if (url.pathname === '/api/friends/accept') {
        if (!me.incoming.includes(otherKey)) return send(res, 400, { error: 'norequest', message: 'No request from this player' });
        drop(me.incoming, otherKey); drop(other.outgoing, myKey);
        if (!me.friends.includes(otherKey)) me.friends.push(otherKey);
        if (!other.friends.includes(myKey)) other.friends.push(myKey);
      } else if (url.pathname === '/api/friends/decline') {
        drop(me.incoming, otherKey); drop(other.outgoing, myKey); drop(me.outgoing, otherKey); drop(other.incoming, myKey);
      } else if (url.pathname === '/api/friends/remove') {
        drop(me.friends, otherKey); drop(other.friends, myKey);
      } else return send(res, 404, { error: 'not found' });
      dirty = true;
      return send(res, 200, { ok: true });
    }

    // ---------- CurseForge (ключ хранится только на сервере) ----------
    if (url.pathname.startsWith('/cf/')) {
      if (!CF_KEY) return send(res, 503, { error: 'CurseForge key is not configured on this server' });
      return proxy(res, 'https://api.curseforge.com/' + url.pathname.slice(4) + url.search, { 'x-api-key': CF_KEY, Accept: 'application/json' });
    }
    if (url.pathname === '/cfdl') {
      const target = url.searchParams.get('url') || '';
      if (!/^https:\/\/(edge|mediafilez|media)\.forgecdn\.net\//.test(target)) return send(res, 400, { error: 'bad url' });
      return proxy(res, target, CF_KEY ? { 'x-api-key': CF_KEY } : {});
    }

    send(res, 404, { error: 'not found' });
  } catch (e) {
    send(res, 400, { error: String(e.message || e) });
  }
});

server.listen(PORT, () => console.log(`Aqua Network запущен на порту ${PORT}${CF_KEY ? ' (CurseForge включён)' : ''}`));
