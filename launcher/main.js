const { app, BrowserWindow, ipcMain, shell, dialog } = require('electron');
const L = require('./src/core/i18n');
const path = require('path');
const fs = require('fs');

app.setName('Aqua Launcher');

// аппаратное ускорение можно выключить в настройках (нужен перезапуск)
try {
  const early = require('./src/core/store').load();
  if (early.settings.hardwareAcceleration === false) app.disableHardwareAcceleration();
} catch { /* первый запуск */ }

let win;

function createWindow() {
  win = new BrowserWindow({
    width: 1220,
    height: 760,
    minWidth: 1000,
    minHeight: 640,
    frame: false,
    show: false,
    backgroundColor: '#021526',
    icon: path.join(__dirname, 'build', 'icon.png'),
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: false,
    },
  });
  win.loadFile(path.join(__dirname, 'src', 'ui', 'index.html'));
  win.once('ready-to-show', () => {
    try { win.webContents.setZoomFactor(Number(require('./src/core/store').state.settings.uiZoom) || 1); } catch { /* ok */ }
    win.show();
  });
  win.webContents.setWindowOpenHandler(({ url }) => { shell.openExternal(url); return { action: 'deny' }; });
}

app.whenReady().then(() => {
  // модули ядра требуют готового app (пути, safeStorage)
  const paths = require('./src/core/paths');
  const store = require('./src/core/store');
  const versions = require('./src/core/versions');
  const auth = require('./src/core/auth');
  const mods = require('./src/core/mods');
  const launcher = require('./src/core/launch');
  const java = require('./src/core/java');

  const s = store.load();
  // старое имя сборки по умолчанию → Aqua PvP
  for (const i of s.instances) if (i.name === 'Ender PvP') { i.name = 'Aqua PvP'; store.save(); }
  if (!s.instances.length) {
    s.instances.push({
      id: 'aqua-pvp', name: 'Aqua PvP', icon: '🐢', mc: '1.21.1', loader: 'fabric', loaderVersion: 'latest',
      fpsBoost: true, enderClient: true, created: Date.now(), lastPlayed: 0,
    });
    s.selectedInstance = 'aqua-pvp';
    store.save();
  }

  createWindow();

  const send = (channel, data) => { if (win && !win.isDestroyed()) win.webContents.send(channel, data); };
  const pushState = () => send('state', store.publicState());
  // автообновление (GitHub Releases)
  const updater = require('./src/core/updater').init(send, () => store.state.settings.autoUpdate !== false);
  const handle = (name, fn) => ipcMain.handle(name, async (_e, ...args) => {
    try { return { ok: true, data: await fn(...args) }; } catch (e) { return { ok: false, error: e && e.message ? e.message : String(e) }; }
  });

  // ---------- окно / система ----------
  ipcMain.on('win', (_e, action) => {
    if (!win) return;
    if (action === 'min') win.minimize();
    else if (action === 'max') (win.isMaximized() ? win.unmaximize() : win.maximize());
    else if (action === 'close') win.close();
  });
  handle('update:state', () => updater.state);
  handle('update:check', () => updater.check());
  handle('update:install', () => { updater.install(); return true; });
  handle('update:page', () => { updater.page(); return true; });
  handle('app:info', () => ({ version: app.getVersion(), totalMem: java.totalMemoryMb(), platform: process.platform, root: paths.root,
    enderJar: require('./src/core/aquaclient').status().ready }));
  handle('app:open', (what, id) => {
    const dir = what === 'instance' ? paths.instanceDir(id) : what === 'mods' ? path.join(paths.instanceDir(id), 'mods') : paths.root;
    fs.mkdirSync(dir, { recursive: true });
    return shell.openPath(dir);
  });
  handle('app:external', (url) => { if (/^https:\/\//.test(url)) shell.openExternal(url); });
  handle('app:pickJava', async () => {
    const r = await dialog.showOpenDialog(win, { title: 'Выберите java / javaw', properties: ['openFile'] });
    return r.canceled ? null : r.filePaths[0];
  });

  // ---------- состояние ----------
  handle('state:get', () => store.publicState());
  handle('settings:save', (settings) => {
    Object.assign(store.state.settings, settings);
    store.save();
    const st = store.state.settings;
    try { app.setLoginItemSettings({ openAtLogin: !!st.startWithSystem }); } catch { /* не поддерживается */ }
    if (win) win.webContents.setZoomFactor(Number(st.uiZoom) || 1);
    pushState();
  });
  handle('app:restart', () => { app.relaunch(); app.exit(0); });
  handle('app:clearCache', () => { fs.rmSync(paths.cache, { recursive: true, force: true }); fs.mkdirSync(paths.cache, { recursive: true }); return true; });

  // ---------- новости Minecraft ----------
  handle('news:get', async () => {
    const { cachedJson } = require('./src/core/net');
    const data = await cachedJson('https://launchercontent.mojang.com/v2/javaPatchNotes.json', path.join(paths.cache, 'patchnotes.json'), 3600e3);
    return (data.entries || []).slice(0, 6).map((e) => ({
      title: e.title, version: e.version, type: e.type, date: e.date,
      image: e.image && e.image.url ? 'https://launchercontent.mojang.com' + e.image.url : null, text: e.shortText || '',
    }));
  });

  // ---------- гардероб ----------
  handle('locker:save', (loadout) => {
    const slots = ['cape', 'hat', 'face', 'wings', 'back', 'pet', 'aura', 'icon'];
    for (const k of slots) if (typeof loadout[k] === 'string') store.state.loadout[k] = loadout[k].slice(0, 32);
    store.state.loadoutStamp = String(Date.now());
    store.save(); pushState();
    return true;
  });

  // ---------- скины ----------
  const skinFile = path.join(paths.root, 'skin.png');
  const checkSkin = (buf) => {
    if (buf.slice(1, 4).toString() !== 'PNG') throw new Error(L('Это не PNG-файл', 'This is not a PNG file'));
    const w = buf.readUInt32BE(16), h = buf.readUInt32BE(20);
    if (w !== 64 || (h !== 64 && h !== 32)) throw new Error(L(`Скин должен быть 64×64 или 64×32 (а не ${w}×${h})`, `Skin must be 64×64 or 64×32 (got ${w}×${h})`));
  };
  handle('skin:get', () => ({ ...store.state.skin, data: store.state.skin.has && fs.existsSync(skinFile) ? fs.readFileSync(skinFile).toString('base64') : null }));
  handle('skin:set', (b64, model) => {
    const buf = Buffer.from(b64, 'base64');
    checkSkin(buf);
    fs.writeFileSync(skinFile, buf);
    store.state.skin = { has: true, model: model === 'slim' ? 'slim' : 'classic' };
    store.save(); pushState();
    return true;
  });
  handle('skin:model', (model) => { store.state.skin.model = model === 'slim' ? 'slim' : 'classic'; store.save(); pushState(); return true; });
  handle('skin:clear', () => { fs.rmSync(skinFile, { force: true }); store.state.skin = { has: false, model: 'classic' }; store.save(); pushState(); return true; });
  handle('skin:fromName', async (name) => {
    if (!/^[A-Za-z0-9_]{1,16}$/.test(name || '')) throw new Error(L('Неверный ник', 'Invalid nickname'));
    const r = await fetch(`https://mc-heads.net/skin/${encodeURIComponent(name)}`);
    if (!r.ok) throw new Error(L('Скин не найден', 'Skin not found'));
    const buf = Buffer.from(await r.arrayBuffer());
    checkSkin(buf);
    return buf.toString('base64');
  });
  handle('skin:mojang', async () => {
    const st = store.state;
    const acc = st.accounts.find((a) => a.id === st.selectedAccount);
    if (!acc || acc.type !== 'microsoft') throw new Error(L('Загрузка в Minecraft доступна только для аккаунта Microsoft', 'Uploading to Minecraft needs a Microsoft account'));
    if (!fs.existsSync(skinFile)) throw new Error(L('Сначала выберите скин', 'Choose a skin first'));
    const tokenObj = await auth.authorizationFor(acc);
    const form = new FormData();
    form.append('variant', st.skin.model === 'slim' ? 'slim' : 'classic');
    form.append('file', new Blob([fs.readFileSync(skinFile)], { type: 'image/png' }), 'skin.png');
    const r = await fetch('https://api.minecraftservices.com/minecraft/profile/skins', { method: 'POST', headers: { Authorization: `Bearer ${tokenObj.access_token}` }, body: form });
    if (!r.ok) throw new Error(L(`Mojang отклонил скин (${r.status})`, `Mojang rejected the skin (${r.status})`));
    return true;
  });

  // ---------- аккаунты ----------
  handle('account:microsoft', async () => { await auth.loginMicrosoft(); pushState(); return true; });
  handle('account:offline', (name) => { auth.addOffline(name); pushState(); return true; });
  handle('account:select', (id) => { store.state.selectedAccount = id; store.save(); pushState(); });
  handle('account:remove', (id) => {
    const st = store.state;
    st.accounts = st.accounts.filter((a) => a.id !== id);
    if (st.selectedAccount === id) st.selectedAccount = st.accounts[0] ? st.accounts[0].id : null;
    store.save(); pushState();
  });

  // ---------- версии ----------
  handle('versions:list', () => versions.listVersions());
  handle('versions:loaders', (loader, mc) => versions.loaderVersions(loader, mc));

  // ---------- сборки ----------
  handle('instance:save', (data) => {
    const st = store.state;
    const clean = {
      name: String(data.name || 'Новая сборка').slice(0, 40), icon: data.icon || '🐟', mc: data.mc,
      loader: data.loader || 'vanilla', loaderVersion: data.loaderVersion || 'latest',
      fpsBoost: !!data.fpsBoost, enderClient: true, ram: data.ram ? Number(data.ram) : null,
    };
    if (!clean.mc) throw new Error(L('Выберите версию Minecraft', 'Choose a Minecraft version'));
    if (data.id) {
      const inst = st.instances.find((i) => i.id === data.id);
      if (!inst) throw new Error(L('Сборка не найдена', 'Profile not found'));
      if (inst.mc !== clean.mc || inst.loader !== clean.loader) inst.preparedFor = null;
      Object.assign(inst, clean);
    } else {
      const base = clean.name.toLowerCase().replace(/[^a-z0-9а-я]+/gi, '-').replace(/^-|-$/g, '') || 'instance';
      const id = `${base}-${Date.now().toString(36)}`;
      st.instances.push({ id, ...clean, created: Date.now(), lastPlayed: 0 });
      st.selectedInstance = id;
    }
    store.save(); pushState();
    return true;
  });
  handle('instance:select', (id) => { store.state.selectedInstance = id; store.save(); pushState(); });
  handle('instance:remove', (id, deleteFiles) => {
    const st = store.state;
    st.instances = st.instances.filter((i) => i.id !== id);
    if (st.selectedInstance === id) st.selectedInstance = st.instances[0] ? st.instances[0].id : null;
    if (deleteFiles) fs.rmSync(paths.instanceDir(id), { recursive: true, force: true });
    store.save(); pushState();
  });

  // ---------- запуск ----------
  handle('game:launch', async (id, server) => {
    await launcher.launch(id, (channel, data) => {
      send('game:' + channel, data);
      const st = store.state.settings;
      if (channel === 'started' && win) {
        if (st.afterLaunch === 'minimize' || st.closeOnLaunch) win.minimize();
        else if (st.afterLaunch === 'hide') win.hide();
      }
      if (channel === 'state' && data && !data.running && win && st.reopenAfterExit) {
        if (!win.isVisible()) win.show();
        if (win.isMinimized()) win.restore();
        win.focus();
      }
    }, server);
    pushState();
  });
  handle('game:kill', () => launcher.kill());

  // ---------- друзья (Aqua Network) ----------
  const friends = require('./src/core/friends');
  handle('aqua:signin', async (mode, name, password) => { const n = await friends.signIn(mode, name, password); pushState(); return n; });
  handle('friends:list', () => friends.list());
  handle('friends:action', (kind, name) => friends.action(kind, name));
  let lastIncoming = 0;
  const beat = async () => {
    const st = store.state;
    const inst = st.instances.find((i) => i.id === st.selectedInstance);
    const status = launcher.isRunning() && inst ? `🎮 ${inst.name} · ${inst.mc}` : '🐢 Aqua Launcher';
    const r = await friends.heartbeat(status);
    if (r && r.incoming !== lastIncoming) { lastIncoming = r.incoming; send('friends:incoming', r.incoming); }
  };
  setInterval(beat, 60000);
  setTimeout(beat, 4000);

  // ---------- серверы ----------
  const servers = require('./src/core/servers');
  handle('servers:ping', (address) => servers.ping(address));
  handle('servers:save', (list) => {
    store.state.servers = (list || []).filter((x) => x && x.address).map((x) => ({ name: String(x.name || x.address).slice(0, 32), address: String(x.address).trim() }));
    store.save(); pushState();
  });

  // ---------- Aqua Client ----------
  const aquaClient = require('./src/core/aquaclient');
  handle('client:status', () => aquaClient.status());
  handle('client:build', () => aquaClient.ensureJar((l) => send('game:log', l), (p) => send('game:progress', p), true).then((jar) => { send('game:progress', null); return jar; }, (e) => { send('game:progress', null); throw e; }));

  // ---------- моды ----------
  handle('mods:search', (q) => mods.search(q));
  handle('mods:install', (q) => mods.install(q, (line) => send('game:log', line)));
  handle('mods:installed', (id, type) => mods.listInstalled(id, type));
  handle('mods:toggle', (id, file, type) => mods.toggle(id, file, type));
  handle('mods:remove', (id, file, type) => mods.remove(id, file, type));
});

app.on('window-all-closed', () => app.quit());
