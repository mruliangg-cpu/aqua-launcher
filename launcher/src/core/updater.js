/* Автообновление Aqua Launcher (как в Lunar): проверка GitHub Releases, загрузка в фоне, «Перезапустить».
   Установленная версия (Setup) обновляется сама; переносная (Portable) — показывает ссылку на новую версию. */
const { app, shell } = require('electron');
const REPO = 'mruliangg-cpu/aqua-launcher';
const RELEASES = `https://github.com/${REPO}/releases/latest`;

function init(send, enabled) {
  const portable = !!process.env.PORTABLE_EXECUTABLE_DIR;
  const state = { status: 'idle', version: null, percent: 0, portable, error: null };
  const emit = (patch) => { Object.assign(state, patch); send('update', { ...state }); };
  if (!app.isPackaged) return { state, check: async () => state, install: () => {}, page: () => shell.openExternal(RELEASES) };

  let au = null;
  try { au = require('electron-updater').autoUpdater; } catch { /* нет модуля — только ручная проверка */ }
  if (au) {
    au.autoDownload = !portable;
    au.autoInstallOnAppQuit = true;
    au.on('checking-for-update', () => emit({ status: 'checking', error: null }));
    au.on('update-available', (i) => emit({ status: portable ? 'available' : 'downloading', version: i.version, percent: 0 }));
    au.on('update-not-available', () => emit({ status: 'latest' }));
    au.on('download-progress', (p) => emit({ status: 'downloading', percent: Math.round(p.percent || 0) }));
    au.on('update-downloaded', (i) => emit({ status: 'ready', version: i.version, percent: 100 }));
    au.on('error', (e) => emit({ status: 'error', error: String(e && e.message || e).slice(0, 200) }));
  }
  const check = async () => {
    if (!au) return state;
    try { await au.checkForUpdates(); } catch (e) { emit({ status: 'error', error: String(e.message || e).slice(0, 200) }); }
    return state;
  };
  if (enabled()) {
    setTimeout(check, 8000);
    setInterval(() => { if (enabled() && state.status !== 'ready' && state.status !== 'downloading') check(); }, 30 * 60 * 1000);
  }
  return {
    state,
    check,
    install: () => { if (au && state.status === 'ready') au.quitAndInstall(false, true); },
    page: () => shell.openExternal(RELEASES),
  };
}

module.exports = { init, RELEASES };
