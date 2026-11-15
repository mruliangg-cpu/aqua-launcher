/* Автообновление Aqua Launcher (как в Lunar Client).
   Пока лаунчер открыт, он сам проверяет GitHub Releases (при запуске, каждые 10 минут и при возврате в окно),
   показывает «Доступно обновление», скачивает его и перезапускается уже новой версией.
   • Установленная версия (Setup) — через electron-updater (NSIS, дифф-загрузка по blockmap).
   • Переносная версия (Portable) — своя загрузка нового .exe и замена файла после закрытия. */
const { app, shell, net } = require('electron');
const fs = require('fs');
const os = require('os');
const path = require('path');
const { spawn } = require('child_process');

const REPO = 'mruliangg-cpu/aqua-launcher';
const RELEASES = `https://github.com/${REPO}/releases/latest`;
const CHECK_EVERY = 10 * 60 * 1000;

const newer = (a, b) => {
  const pa = String(a).replace(/^v/, '').split('.').map(Number), pb = String(b).replace(/^v/, '').split('.').map(Number);
  for (let i = 0; i < 3; i++) { if ((pa[i] || 0) !== (pb[i] || 0)) return (pa[i] || 0) > (pb[i] || 0); }
  return false;
};

function init(send, enabled) {
  const portableFile = process.env.PORTABLE_EXECUTABLE_FILE || null;
  const portable = !!(portableFile || process.env.PORTABLE_EXECUTABLE_DIR);
  const state = { status: 'idle', version: null, percent: 0, portable, error: null, current: app.getVersion() };
  const emit = (patch) => { Object.assign(state, patch); send('update', { ...state }); };
  if (!app.isPackaged) return { state, check: async () => state, install: () => {}, page: () => shell.openExternal(RELEASES), focus: () => {} };

  // ---------------- установленная версия: electron-updater
  let au = null;
  if (!portable) {
    try { au = require('electron-updater').autoUpdater; } catch { /* нет модуля */ }
    if (au) {
      au.autoDownload = true;              // качаем в фоне сразу, как только нашли
      au.autoInstallOnAppQuit = false;     // ставим только по кнопке, пока лаунчер открыт
      au.on('checking-for-update', () => { if (!['downloading', 'ready'].includes(state.status)) emit({ status: 'checking', error: null }); });
      au.on('update-available', (i) => emit({ status: 'downloading', version: i.version, percent: 0 }));
      au.on('update-not-available', () => emit({ status: 'latest' }));
      au.on('download-progress', (p) => emit({ status: 'downloading', percent: Math.round(p.percent || 0) }));
      au.on('update-downloaded', (i) => emit({ status: 'ready', version: i.version, percent: 100 }));
      au.on('error', (e) => emit({ status: 'error', error: String(e && e.message || e).slice(0, 200) }));
    }
  }

  // ---------------- переносная версия: свой загрузчик
  let latest = null; // { version, url, size }
  async function checkPortable() {
    emit({ status: 'checking', error: null });
    const r = await net.fetch(`https://api.github.com/repos/${REPO}/releases/latest`, { headers: { 'User-Agent': 'AquaLauncher', Accept: 'application/vnd.github+json' } });
    if (!r.ok) throw new Error('GitHub HTTP ' + r.status);
    const j = await r.json();
    const v = String(j.tag_name || '').replace(/^v/, '');
    const asset = (j.assets || []).find((a) => /Portable.*\.exe$/i.test(a.name));
    if (!v || !asset || !newer(v, app.getVersion())) { emit({ status: 'latest' }); return; }
    latest = { version: v, url: asset.browser_download_url, size: asset.size };
    emit({ status: 'available', version: v, percent: 0 });
  }

  async function installPortable() {
    if (!latest || !portableFile) { shell.openExternal(RELEASES); return; }
    emit({ status: 'downloading', percent: 0 });
    const tmp = portableFile + '.update';
    try {
      const r = await net.fetch(latest.url, { headers: { 'User-Agent': 'AquaLauncher' } });
      if (!r.ok || !r.body) throw new Error('HTTP ' + r.status);
      const total = Number(r.headers.get('content-length')) || latest.size || 0;
      const out = fs.createWriteStream(tmp);
      const reader = r.body.getReader();
      let got = 0, last = 0;
      for (;;) {
        const { done, value } = await reader.read();
        if (done) break;
        got += value.length;
        if (!out.write(Buffer.from(value))) await new Promise((ok) => out.once('drain', ok));
        const p = total ? Math.floor(got / total * 100) : 0;
        if (p !== last) { last = p; emit({ status: 'downloading', percent: p }); }
      }
      await new Promise((ok, bad) => out.end((e) => (e ? bad(e) : ok())));
      if (total && fs.statSync(tmp).size !== total) throw new Error('download incomplete');
    } catch (e) {
      try { fs.unlinkSync(tmp); } catch { /* ignore */ }
      emit({ status: 'error', error: String(e.message || e).slice(0, 200) });
      return;
    }
    emit({ status: 'ready', percent: 100 });
    // После выхода лаунчера скрипт заменяет .exe (повторяя, пока файл занят) и запускает новую версию.
    const script = path.join(os.tmpdir(), `aqua-update-${Date.now()}.cmd`);
    fs.writeFileSync(script, [
      '@echo off',
      'set n=0',
      ':retry',
      'timeout /t 1 /nobreak >nul',
      `move /y "${tmp}" "${portableFile}" >nul 2>&1`,
      'if not errorlevel 1 goto ok',
      'set /a n+=1',
      'if %n% lss 90 goto retry',
      'goto end',
      ':ok',
      `start "" "${portableFile}"`,
      ':end',
      'del "%~f0"',
    ].join('\r\n'));
    spawn('cmd.exe', ['/c', script], { detached: true, stdio: 'ignore', windowsHide: true }).unref();
    setTimeout(() => app.quit(), 400);
  }

  let busy = false;
  const check = async () => {
    if (busy || ['downloading', 'ready'].includes(state.status)) return state;
    busy = true;
    try {
      if (portable) await checkPortable();
      else if (au) await au.checkForUpdates();
    } catch (e) { emit({ status: 'error', error: String(e.message || e).slice(0, 200) }); }
    finally { busy = false; }
    return state;
  };

  let lastCheck = 0;
  const auto = () => { if (enabled() && Date.now() - lastCheck > 60 * 1000) { lastCheck = Date.now(); check(); } };
  setTimeout(auto, 5000);
  setInterval(auto, CHECK_EVERY);

  return {
    state,
    check,
    focus: () => { if (Date.now() - lastCheck > 5 * 60 * 1000) auto(); }, // вернулись в окно — быстрая проверка
    install: () => {
      if (portable) { if (state.status === 'available') installPortable(); return; }
      if (au && state.status === 'ready') au.quitAndInstall(true, true); // тихая установка + перезапуск
    },
    page: () => shell.openExternal(RELEASES),
  };
}

module.exports = { init, RELEASES };
