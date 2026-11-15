/* Aqua Network: аккаунты Aqua (ник + пароль, ник уникален) и друзья. */
const store = require('./store');
const auth = require('./auth');
const L = require('./i18n');

const base = () => (store.state.settings.networkUrl || '').trim().replace(/\/+$/, '');
const MSG = {
  taken: () => L('Этот ник уже занят — выберите другой', 'This nickname is already taken — pick another one'),
  name: () => L('Ник: 3–16 символов, латиница, цифры и _', 'Nickname: 3–16 characters, latin letters, digits and _'),
  password: () => L('Пароль — минимум 6 символов', 'Password must be at least 6 characters'),
  auth: () => L('Неверный ник или пароль', 'Wrong nickname or password'),
  nouser: () => L('Игрока с таким ником нет в Aqua', 'No Aqua player with this nickname'),
  self: () => L('Нельзя добавить самого себя', 'You cannot add yourself'),
  norequest: () => L('Заявки от этого игрока нет', 'No request from this player'),
};

async function api(path, { method = 'GET', body, token } = {}) {
  if (!base()) throw new Error(L('Сначала укажите адрес Aqua Network в Настройках', 'Set your Aqua Network URL in Settings first'));
  let r;
  try {
    r = await fetch(base() + path, {
      method, body: body ? JSON.stringify(body) : undefined,
      headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) },
    });
  } catch {
    throw new Error(L('Aqua Network недоступен — проверьте интернет и адрес сервера', 'Aqua Network is unreachable — check the internet and server URL'));
  }
  const j = await r.json().catch(() => ({}));
  if (!r.ok) {
    const e = new Error(MSG[j.error] ? MSG[j.error]() : j.message || `HTTP ${r.status}`);
    e.status = r.status;
    throw e;
  }
  return j;
}


async function tokenFor(acc, fresh = false) {
  if (!acc) throw new Error(L('Сначала войдите в аккаунт', 'Sign in first'));
  if (acc.aquaToken && !fresh) return store.unseal(acc.aquaToken);
  if (acc.type === 'microsoft' && !acc.demo) {
    const mc = await auth.authorizationFor(acc);
    const j = await api('/api/account/ms', { method: 'POST', body: { accessToken: mc.access_token } });
    acc.aquaToken = store.seal(j.token);
    store.save();
    return j.token;
  }
  const e = new Error(L('Войдите в аккаунт Aqua (ник + пароль), чтобы добавлять друзей', 'Sign in to an Aqua account (nickname + password) to add friends'));
  e.needLogin = true;
  throw e;
}

function selected() { const s = store.state; return s.accounts.find((a) => a.id === s.selectedAccount); }

async function withToken(fn) {
  const acc = selected();
  try {
    return await fn(await tokenFor(acc));
  } catch (e) {
    if (e.status === 401 && acc && acc.type === 'microsoft' && !acc.demo) return fn(await tokenFor(acc, true));
    if (e.status === 401 && acc) { acc.aquaToken = null; store.save(); }
    throw e;
  }
}

const list = () => withToken((token) => api('/api/friends', { token }));
const action = (kind, name) => withToken((token) => api('/api/friends/' + kind, { method: 'POST', body: { name }, token }));
/** Уровень игрока по времени в игре и числу запусков. */
function level() {
  const st = store.state.stats || { playMs: 0, launches: 0 };
  const xp = Math.floor(st.playMs / 60000) * 2 + st.launches * 15;
  const lvl = Math.floor(Math.sqrt(xp / 10)) + 1;
  const cur = (lvl - 1) ** 2 * 10, next = lvl ** 2 * 10;
  return { level: lvl, xp, progress: (xp - cur) / Math.max(1, next - cur), hours: st.playMs / 3600000 };
}

async function leaderboard() {
  try { return await api('/api/leaderboard'); } catch { return null; }
}

async function heartbeat(status) {
  if (!base()) return null;
  const acc = selected();
  if (!acc || (!acc.aquaToken && (acc.type !== 'microsoft' || acc.demo))) return null;
  try { return await withToken((token) => api('/api/friends/status', { method: 'POST', body: { status, level: level().level }, token })); } catch { return null; }
}

/** Регистрация или вход аккаунта Aqua (ник + пароль) → профиль с этим ником. */
async function signIn(mode, name, password) {
  const j = await api(mode === 'register' ? '/api/account/register' : '/api/account/login', { method: 'POST', body: { name, password } });
  auth.addOffline(j.name, store.seal(j.token));
  return j.name;
}

module.exports = { signIn, list, action, heartbeat, level, leaderboard };
