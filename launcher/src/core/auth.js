const crypto = require('crypto');
const L = require('./i18n');
const { Auth } = require('msmc');
const store = require('./store');

const WINDOW = { width: 520, height: 680, title: 'Вход в аккаунт Microsoft', backgroundColor: '#04203a', autoHideMenuBar: true };

/** UUID офлайн-игрока — так же, как его считает сам Minecraft. */
function offlineUuid(name) {
  const md5 = crypto.createHash('md5').update('OfflinePlayer:' + name).digest();
  md5[6] = (md5[6] & 0x0f) | 0x30;
  md5[8] = (md5[8] & 0x3f) | 0x80;
  const h = md5.toString('hex');
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`;
}

async function fromXbox(xbox) {
  let mc = null, err = null;
  try { mc = await xbox.getMinecraft(); } catch (e) { err = e; }
  if (mc && !mc.isDemo()) return { profile: mc.profile, mclc: mc.mclc(), refresh: xbox.save(), demo: false };
  // Нет купленной игры → официальный демо-режим Minecraft (как в обычном лаунчере Mojang).
  try { return await demoFrom(xbox); } catch { throw err || new Error(L('Не удалось войти в Minecraft через этот аккаунт Microsoft.', 'Could not sign in to Minecraft with this Microsoft account.')); }
}

/** Вход в демо-версию: аккаунт Microsoft без покупки получает токен Minecraft и играет с флагом --demo. */
async function demoFrom(xbox) {
  const identityToken = await xbox.xAuth('rp://api.minecraftservices.com/');
  const r = await fetch('https://api.minecraftservices.com/authentication/login_with_xbox', {
    method: 'POST', headers: { 'Content-Type': 'application/json', Accept: 'application/json' }, body: JSON.stringify({ identityToken }),
  });
  if (!r.ok) throw new Error('login_with_xbox ' + r.status);
  const j = await r.json();
  const uuid = offlineUuid('AquaDemo:' + j.username);
  const mclc = {
    access_token: j.access_token, client_token: crypto.randomUUID(), uuid: uuid.replace(/-/g, ''), name: 'Player', user_properties: {},
    meta: { type: 'msa', demo: true, exp: Date.now() + Math.max(600, (j.expires_in || 86400) - 300) * 1000 },
  };
  return { profile: { id: uuid, name: 'Player' }, mclc, refresh: xbox.save(), demo: true };
}

async function loginMicrosoft() {
  const auth = new Auth('select_account');
  const xbox = await auth.launch('electron', WINDOW);
  const res = await fromXbox(xbox);
  const s = store.state;
  const id = res.profile.id;
  const account = {
    id: res.demo ? 'demo-' + id : id, type: 'microsoft', name: res.demo ? 'Demo' : res.profile.name, uuid: id,
    refresh: store.seal(res.refresh), mclc: res.mclc, updated: Date.now(), ...(res.demo ? { demo: true } : {}),
  };
  s.accounts = s.accounts.filter((a) => a.id !== account.id);
  s.accounts.push(account);
  s.selectedAccount = account.id;
  if (!res.demo) s.ownershipVerified = true; // подтвердили лицензию — офлайн-профили разблокированы
  store.save();
  return account;
}

/** Готовый объект авторизации для MCLC (с обновлением токена Microsoft). */
async function authorizationFor(account) {
  if (account.type !== 'microsoft') throw new Error(L('Войдите через Microsoft.', 'Please sign in with Microsoft.'));
  const fresh = account.mclc && account.mclc.meta && account.mclc.meta.exp && account.mclc.meta.exp > Date.now() + 5 * 60e3;
  if (fresh) return account.mclc;
  const refreshToken = store.unseal(account.refresh);
  if (!refreshToken) throw new Error(L('Сессия Microsoft истекла — войдите заново.', 'Microsoft session expired — please sign in again.'));
  const auth = new Auth('select_account');
  const xbox = await auth.refresh(refreshToken);
  const res = await fromXbox(xbox);
  account.mclc = res.mclc;
  account.refresh = store.seal(res.refresh);
  if (!res.demo) account.name = res.profile.name;
  account.updated = Date.now();
  store.save();
  return res.mclc;
}

module.exports = { loginMicrosoft, authorizationFor, offlineUuid };
