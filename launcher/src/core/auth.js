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
  const mc = await xbox.getMinecraft();
  if (mc.isDemo()) throw new Error(L('На этом аккаунте Microsoft нет купленной Minecraft: Java Edition.', 'This Microsoft account does not own Minecraft: Java Edition.'));
  return { profile: mc.profile, mclc: mc.mclc(), refresh: xbox.save() };
}

async function loginMicrosoft() {
  const auth = new Auth('select_account');
  const xbox = await auth.launch('electron', WINDOW);
  const res = await fromXbox(xbox);
  const s = store.state;
  const id = res.profile.id;
  const account = {
    id, type: 'microsoft', name: res.profile.name, uuid: id,
    refresh: store.seal(res.refresh), mclc: res.mclc, updated: Date.now(),
  };
  s.accounts = s.accounts.filter((a) => a.id !== id);
  s.accounts.push(account);
  s.selectedAccount = id;
  s.ownershipVerified = true; // подтвердили лицензию — офлайн-профили разблокированы
  store.save();
  return account;
}

function addOffline(name, aquaToken) {
  const s = store.state;
  if (!s.ownershipVerified) throw new Error(L('Сначала один раз войдите через Microsoft с купленной Minecraft — после этого вход по нику станет доступен.', 'Sign in once with a Microsoft account that owns Minecraft — then nickname login unlocks.'));
  if (!/^[A-Za-z0-9_]{3,16}$/.test(name)) throw new Error(L('Ник: 3–16 символов, латиница, цифры и _', 'Nickname: 3–16 characters, latin letters, digits and _'));
  const uuid = offlineUuid(name);
  const id = 'offline-' + uuid;
  s.accounts = s.accounts.filter((a) => a.id !== id);
  s.accounts.push({ id, type: 'offline', name, uuid, ...(aquaToken ? { aquaToken } : {}) });
  s.selectedAccount = id;
  store.save();
  return id;
}

/** Готовый объект авторизации для MCLC (с обновлением токена Microsoft). */
async function authorizationFor(account) {
  if (account.type === 'offline') {
    return {
      access_token: crypto.randomBytes(16).toString('hex'),
      client_token: account.uuid,
      uuid: account.uuid.replace(/-/g, ''),
      name: account.name,
      user_properties: '{}',
      meta: { type: 'legacy', demo: false },
    };
  }
  const fresh = account.mclc && account.mclc.meta && account.mclc.meta.exp && account.mclc.meta.exp > Date.now() + 5 * 60e3;
  if (fresh) return account.mclc;
  const refreshToken = store.unseal(account.refresh);
  if (!refreshToken) throw new Error(L('Сессия Microsoft истекла — войдите заново.', 'Microsoft session expired — please sign in again.'));
  const auth = new Auth('select_account');
  const xbox = await auth.refresh(refreshToken);
  const res = await fromXbox(xbox);
  account.mclc = res.mclc;
  account.refresh = store.seal(res.refresh);
  account.name = res.profile.name;
  account.updated = Date.now();
  store.save();
  return res.mclc;
}

module.exports = { loginMicrosoft, addOffline, authorizationFor, offlineUuid };
