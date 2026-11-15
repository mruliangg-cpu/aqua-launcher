const fs = require('fs');
const { safeStorage } = require('electron');
const paths = require('./paths');

const DEFAULTS = {
  accounts: [],
  selectedAccount: null,
  instances: [],
  selectedInstance: null,
  ownershipVerified: false,
  servers: [
    { name: 'Donut SMP', address: 'donutsmp.net' },
    { name: 'Hypixel', address: 'mc.hypixel.net' },
    { name: 'Minemen Club', address: 'minemen.club' },
    { name: 'CubeCraft', address: 'play.cubecraft.net' },
    { name: 'Wynncraft', address: 'play.wynncraft.com' },
    { name: 'BedWars Practice', address: 'bedwarspractice.club' },
    { name: '2b2t', address: '2b2t.org' },
  ],
  loadout: { cape: 'aqua', hat: 'none', face: 'none', wings: 'none', back: 'none', pet: 'turtle', aura: 'bubbles', icon: 'turtle' },
  loadoutStamp: '1',
  skin: { model: 'classic', has: false },
  settings: {
    language: 'en',
    theme: 'aqua',
    uiZoom: 1,
    reduceMotion: false,
    uiSounds: true,
    bgQuality: 'high',
    startWithSystem: false,
    hardwareAcceleration: true,
    confirmDelete: true,
    afterLaunch: 'keep',
    reopenAfterExit: true,
    consoleOnLaunch: false,
    autoJoin: '',
    priority: 'normal',
    gameLanguage: 'auto',
    guiScale: 0,
    maxFps: 240,
    renderDistance: 12,
    jvmPreset: 'performance',
    downloadThreads: 16,
    autoUpdate: true,
    networkUrl: 'https://aqua-network.mruliangg.workers.dev',
    shareSkin: true,
    ramMax: 4096,
    ramMin: 1024,
    javaPath: '',
    jvmArgs: '',
    width: 1280,
    height: 720,
    fullscreen: false,
    closeOnLaunch: false,
    animations: true,
    curseforgeKey: '',
    showSnapshots: true,
    showOld: true,
  },
};

let state = null;

function load() {
  if (state) return state;
  try {
    const raw = JSON.parse(fs.readFileSync(paths.store, 'utf8'));
    state = { ...DEFAULTS, ...raw, settings: { ...DEFAULTS.settings, ...(raw.settings || {}) } };
    if (!String(state.settings.networkUrl || '').trim()) state.settings.networkUrl = DEFAULTS.settings.networkUrl; // сервер Aqua Network по умолчанию
  } catch {
    state = JSON.parse(JSON.stringify(DEFAULTS));
  }
  return state;
}

function save() {
  fs.writeFileSync(paths.store + '.tmp', JSON.stringify(state, null, 2));
  fs.renameSync(paths.store + '.tmp', paths.store);
}

/** Шифруем refresh-токены Microsoft средствами ОС (DPAPI / Keychain / libsecret). */
function seal(text) {
  if (!text) return null;
  try {
    if (safeStorage.isEncryptionAvailable()) return 'enc:' + safeStorage.encryptString(text).toString('base64');
  } catch { /* fallback */ }
  return 'b64:' + Buffer.from(text).toString('base64');
}

function unseal(value) {
  if (!value) return null;
  if (value.startsWith('enc:')) return safeStorage.decryptString(Buffer.from(value.slice(4), 'base64'));
  if (value.startsWith('b64:')) return Buffer.from(value.slice(4), 'base64').toString();
  return value;
}

/** Состояние для интерфейса — без секретов. */
function publicState() {
  const s = load();
  return {
    ...s,
    accounts: s.accounts.map(({ refresh, mclc, aquaToken, ...a }) => ({ ...a, aqua: !!aquaToken || a.type === 'microsoft' })),
  };
}

module.exports = { load, save, seal, unseal, publicState, get state() { return load(); } };
