const fs = require('fs');
const L = require('./i18n');
const path = require('path');
const { Client } = require('minecraft-launcher-core');
const paths = require('./paths');
const store = require('./store');
const versions = require('./versions');
const java = require('./java');
const auth = require('./auth');
const mods = require('./mods');

const os = require('os');
const nbt = require('./nbt');

/** Флаги Java по пресету из настроек. */
function jvmFlags(preset, major) {
  if (preset === 'off') return ['-Dfile.encoding=UTF-8'];
  if (preset === 'zgc' && major >= 17) return ['-XX:+UseZGC', ...(major >= 21 ? ['-XX:+ZGenerational'] : []), '-XX:+AlwaysPreTouch', '-Dfile.encoding=UTF-8'];
  if (preset === 'balanced') return ['-XX:+UseG1GC', '-XX:MaxGCPauseMillis=100', '-Dfile.encoding=UTF-8'];
  return PERF_FLAGS;
}

/** Файлы для игры: гардероб и скин для Aqua Client, настройки по умолчанию, серверы лаунчера. */
function prepareGameFiles(inst, log) {
  const s = store.state, st = s.settings;
  const dir = paths.instanceDir(inst.id), cfg = path.join(dir, 'config');
  fs.mkdirSync(cfg, { recursive: true });
  fs.writeFileSync(path.join(cfg, 'aquaclient-loadout.json'), JSON.stringify({ stamp: String(s.loadoutStamp), ...s.loadout, skinModel: s.skin.model, shareSkin: st.shareSkin }, null, 2));
  const skinSrc = path.join(paths.root, 'skin.png'), skinDst = path.join(cfg, 'aquaclient-skin.png');
  if (s.skin.has && fs.existsSync(skinSrc)) fs.copyFileSync(skinSrc, skinDst); else fs.rmSync(skinDst, { force: true });
  const options = path.join(dir, 'options.txt');
  if (!fs.existsSync(options)) {
    const lang = st.gameLanguage === 'auto' ? (st.language === 'ru' ? 'ru_ru' : 'en_us') : st.gameLanguage;
    fs.writeFileSync(options, [`lang:${lang}`, `guiScale:${st.guiScale}`, `maxFps:${st.maxFps}`, `renderDistance:${st.renderDistance}`, 'fullscreen:false'].join('\n') + '\n');
  }
  try {
    const added = nbt.mergeServers(path.join(dir, 'servers.dat'), s.servers || []);
    if (added) log(L(`[Aqua] Добавлено серверов в игру: ${added}`, `[Aqua] Servers added to the game: ${added}`));
  } catch (e) { log('[Aqua] servers.dat: ' + e.message); }
}

/** JVM-флаги для стабильного FPS без фризов от сборщика мусора. */
const PERF_FLAGS = [
  '-XX:+UnlockExperimentalVMOptions', '-XX:+UseG1GC', '-XX:G1NewSizePercent=20', '-XX:G1ReservePercent=20',
  '-XX:MaxGCPauseMillis=50', '-XX:G1HeapRegionSize=32M', '-XX:+DisableExplicitGC', '-XX:+ParallelRefProcEnabled',
  '-Dfile.encoding=UTF-8',
];

let running = null; // { instanceId, client }

function isRunning() { return !!running; }

async function launch(instanceId, emit, joinServer) {
  if (running) throw new Error(L('Игра уже запущена', 'The game is already running'));
  const s = store.state;
  const inst = s.instances.find((i) => i.id === instanceId);
  if (!inst) throw new Error(L('Сборка не найдена', 'Profile not found'));
  const account = s.accounts.find((a) => a.id === s.selectedAccount);
  if (!account) throw new Error(L('Добавьте аккаунт (кнопка внизу слева)', 'Add an account first (bottom-left button)'));

  const log = (line) => emit('log', line);
  const progress = (p) => emit('progress', p);
  running = { instanceId, client: null };
  emit('state', { running: true, instanceId });

  try {
    progress({ task: L('Проверка версии', 'Checking version'), done: 0, total: 1 });
    const vjson = await versions.versionJson(inst.mc);
    const major = (vjson.javaVersion && vjson.javaVersion.majorVersion) || 8;

    const javaPath = s.settings.javaPath && fs.existsSync(s.settings.javaPath)
      ? s.settings.javaPath
      : await java.ensureJava(major, progress);
    log(`[Aqua] Java ${major}: ${javaPath}`);

    const opts = {
      root: paths.shared,
      version: { number: inst.mc, type: vjson.type === 'release' ? 'release' : vjson.type },
      memory: { max: `${inst.ram || s.settings.ramMax}M`, min: `${Math.min(s.settings.ramMin, inst.ram || s.settings.ramMax)}M` },
      javaPath,
      overrides: { gameDirectory: paths.instanceDir(inst.id), maxSockets: Number(s.settings.downloadThreads) || 16, detached: false },
      customArgs: [...jvmFlags(s.settings.jvmPreset, major), ...(s.settings.networkUrl ? [`-Daqua.api=${s.settings.networkUrl}`] : []),
        ...(s.settings.jvmArgs ? s.settings.jvmArgs.split(/\s+/).filter(Boolean) : [])],
      window: s.settings.fullscreen ? { fullscreen: true } : { width: s.settings.width, height: s.settings.height },
    };

    if (!joinServer && s.settings.autoJoin) joinServer = s.settings.autoJoin.includes(':') ? s.settings.autoJoin : s.settings.autoJoin + ':25565';
    if (joinServer) {
      const [host, port] = joinServer.split(':');
      // 1.20+ понимает quickPlay, старые версии — --server/--port
      const modern = /^1\.(2[0-9]|[3-9]\d)/.test(inst.mc) || /^2\d\./.test(inst.mc) || /w\d\d/.test(inst.mc);
      if (modern) opts.quickPlay = { type: 'multiplayer', identifier: joinServer };
      else opts.customLaunchArgs = ['--server', host, '--port', port || '25565'];
      log(L(`[Aqua] Автовход на сервер ${joinServer}`, `[Aqua] Auto-joining ${joinServer}`));
    }

    if (inst.loader === 'fabric' || inst.loader === 'quilt') {
      progress({ task: L(`Установка ${inst.loader === 'fabric' ? 'Fabric' : 'Quilt'}`, `Installing ${inst.loader === 'fabric' ? 'Fabric' : 'Quilt'}`), done: 0, total: 1 });
      const { id, loaderVersion } = await versions.installProfile(inst.loader, inst.mc, inst.loaderVersion);
      opts.version.custom = id;
      log(`[Aqua] ${inst.loader} ${loaderVersion}`);
    } else if (inst.loader === 'forge' || inst.loader === 'neoforge') {
      const { file, loaderVersion } = await versions.forgeInstaller(inst.loader, inst.mc, inst.loaderVersion, progress);
      opts.forge = file;
      log(`[Aqua] ${inst.loader} ${loaderVersion}`);
    }

    progress({ task: L('Моды и оптимизация', 'Mods and optimization'), done: 0, total: 1 });
    await mods.prepareInstance(inst, log, progress);
    prepareGameFiles(inst, log);

    progress({ task: L('Вход в аккаунт', 'Signing in'), done: 0, total: 1 });
    opts.authorization = await auth.authorizationFor(account);

    const client = new Client();
    running.client = client;
    const labels = {
      assets: 'Загрузка ресурсов', natives: 'Нативные библиотеки', classes: 'Библиотеки', 'classes-custom': 'Библиотеки загрузчика',
      'classes-maven-custom': 'Библиотеки загрузчика', 'version-jar': 'Клиент Minecraft', forge: 'Установка Forge', 'assets-copy': 'Копирование ресурсов',
    };
    client.on('progress', (e) => progress({ task: labels[e.type] || e.type, done: e.task, total: e.total }));
    client.on('download-status', (e) => {
      if (e.type === 'version-jar') progress({ task: labels['version-jar'], done: e.current, total: e.total });
    });
    client.on('debug', (d) => log(String(d).trim()));
    client.on('data', (d) => {
      const text = String(d);
      text.split(/\r?\n/).filter(Boolean).forEach(log);
      if (running && !running.started && /Setting user:|LWJGL|Backend library/i.test(text)) {
        running.started = true;
        emit('started', { instanceId });
        progress(null);
      }
    });
    client.on('close', (code) => {
      log(L(`[Aqua] Игра закрыта (код ${code})`, `[Aqua] Game closed (code ${code})`));
      running = null;
      progress(null);
      emit('state', { running: false, instanceId, code });
    });

    inst.lastPlayed = Date.now();
    store.save();
    progress({ task: L('Запуск Minecraft…', 'Launching Minecraft…'), done: 0, total: 1 });
    const proc = await client.launch(opts);
    if (!proc) throw new Error(L('MCLC не смог запустить игру — подробности в Консоли.', 'The game could not start — see the Console for details.'));
    if (running) running.proc = proc;
    const prio = { high: os.constants.priority.PRIORITY_HIGH, above: os.constants.priority.PRIORITY_ABOVE_NORMAL }[s.settings.priority];
    if (prio !== undefined) { try { os.setPriority(proc.pid, prio); } catch (e) { log('[Aqua] priority: ' + e.message); } }
    return true;
  } catch (e) {
    running = null;
    progress(null);
    emit('state', { running: false, instanceId, error: e.message });
    throw e;
  }
}

function kill() {
  if (running && running.proc) running.proc.kill();
}

module.exports = { launch, isRunning, kill, prepareGameFiles };
