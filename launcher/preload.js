const { contextBridge, ipcRenderer } = require('electron');

const call = async (channel, ...args) => {
  const res = await ipcRenderer.invoke(channel, ...args);
  if (!res.ok) throw new Error(res.error);
  return res.data;
};
const on = (channel, cb) => {
  const fn = (_e, data) => cb(data);
  ipcRenderer.on(channel, fn);
  return () => ipcRenderer.removeListener(channel, fn);
};

contextBridge.exposeInMainWorld('aqua', {
  win: (action) => ipcRenderer.send('win', action),
  info: () => call('app:info'),
  open: (what, id) => call('app:open', what, id),
  external: (url) => call('app:external', url),
  pickJava: () => call('app:pickJava'),

  getState: () => call('state:get'),
  restart: () => call('app:restart'),
  clearCache: () => call('app:clearCache'),
  news: () => call('news:get'),
  saveLocker: (l) => call('locker:save', l),
  getSkin: () => call('skin:get'),
  setSkin: (b64, model) => call('skin:set', b64, model),
  setSkinModel: (m) => call('skin:model', m),
  clearSkin: () => call('skin:clear'),
  skinFromName: (n) => call('skin:fromName', n),
  uploadSkinMojang: () => call('skin:mojang'),
  saveSettings: (s) => call('settings:save', s),
  onState: (cb) => on('state', cb),

  loginMicrosoft: () => call('account:microsoft'),
  addOffline: (name) => call('account:offline', name),
  selectAccount: (id) => call('account:select', id),
  removeAccount: (id) => call('account:remove', id),

  versions: () => call('versions:list'),
  loaderVersions: (loader, mc) => call('versions:loaders', loader, mc),

  saveInstance: (data) => call('instance:save', data),
  selectInstance: (id) => call('instance:select', id),
  removeInstance: (id, files) => call('instance:remove', id, files),

  launch: (id, server) => call('game:launch', id, server),
  pingServer: (address) => call('servers:ping', address),
  saveServers: (list) => call('servers:save', list),
  clientStatus: () => call('client:status'),
  buildClient: () => call('client:build'),
  kill: () => call('game:kill'),
  onProgress: (cb) => on('game:progress', cb),
  onLog: (cb) => on('game:log', cb),
  onGameState: (cb) => on('game:state', cb),
  onStarted: (cb) => on('game:started', cb),

  aquaSignIn: (mode, name, password) => call('aqua:signin', mode, name, password),
  friends: () => call('friends:list'),
  friendAction: (kind, name) => call('friends:action', kind, name),
  onIncoming: (cb) => on('friends:incoming', cb),
  updateState: () => call('update:state'),
  checkUpdate: () => call('update:check'),
  installUpdate: () => call('update:install'),
  updatePage: () => call('update:page'),
  onUpdate: (cb) => on('update', cb),
  searchMods: (q) => call('mods:search', q),
  installMod: (q) => call('mods:install', q),
  installedMods: (id, type) => call('mods:installed', id, type),
  toggleMod: (id, file, type) => call('mods:toggle', id, file, type),
  removeMod: (id, file, type) => call('mods:remove', id, file, type),
});
