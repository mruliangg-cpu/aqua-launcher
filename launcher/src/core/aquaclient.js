/* Aqua Client: берём готовый jar или собираем мод из встроенных исходников (один раз). */
const fs = require('fs');
const L = require('./i18n');
const path = require('path');
const crypto = require('crypto');
const { spawn } = require('child_process');
const paths = require('./paths');
const java = require('./java');

const JAR_NAME = 'aqua-client-1.21.1.jar';
const outDir = path.join(paths.root, 'aqua-client');
const outJar = path.join(outDir, JAR_NAME);
const stampFile = path.join(outDir, 'source.sha1');
const bundledSrc = path.join(paths.resources, 'aqua-client-src');
const srcDir = fs.existsSync(bundledSrc) ? bundledSrc : path.join(__dirname, '..', '..', '..', 'aqua-client');
let building = null;

function hashDir(dir) {
  const h = crypto.createHash('sha1');
  const walk = (d) => {
    for (const e of fs.readdirSync(d, { withFileTypes: true }).sort((a, b) => a.name.localeCompare(b.name))) {
      if (e.name === 'build' || e.name === '.gradle') continue;
      const p = path.join(d, e.name);
      if (e.isDirectory()) walk(p);
      else { h.update(path.relative(dir, p)); h.update(fs.readFileSync(p)); }
    }
  };
  walk(dir);
  return h.digest('hex');
}

function copyDir(from, to) {
  fs.mkdirSync(to, { recursive: true });
  for (const e of fs.readdirSync(from, { withFileTypes: true })) {
    if (e.name === 'build' || e.name === '.gradle') continue;
    const a = path.join(from, e.name), b = path.join(to, e.name);
    if (e.isDirectory()) copyDir(a, b); else fs.copyFileSync(a, b);
  }
}

function status() {
  const prebuilt = path.join(paths.resources, JAR_NAME);
  if (fs.existsSync(prebuilt)) return { ready: true, jar: prebuilt, source: L('встроен', 'bundled') };
  if (!fs.existsSync(srcDir)) return { ready: false, error: L('Исходники Aqua Client не найдены', 'Aqua Client sources not found') };
  const stamp = fs.existsSync(stampFile) ? fs.readFileSync(stampFile, 'utf8') : '';
  if (fs.existsSync(outJar) && stamp === hashDir(srcDir)) return { ready: true, jar: outJar, source: L('собран', 'built') };
  return { ready: false };
}

/** Возвращает путь к jar, при необходимости собирая мод (JDK 21 + Gradle скачиваются сами). */
async function ensureJar(log, progress, force = false) {
  if (!force) {
    const st = status();
    if (st.ready) return st.jar;
  }
  if (building) return building;
  building = (async () => {
    const hash = hashDir(srcDir);
    progress && progress({ task: L('Aqua Client: подготовка JDK 21', 'Aqua Client: preparing JDK 21'), done: 0, total: 1 });
    const jdkBin = await java.ensureJava(21, progress, 'jdk');
    const home = java.javaHome(jdkBin);
    const work = path.join(paths.root, 'aqua-build');
    fs.rmSync(path.join(work, 'src'), { recursive: true, force: true });
    copyDir(srcDir, work);
    const isWin = process.platform === 'win32';
    if (!isWin) fs.chmodSync(path.join(work, 'gradlew'), 0o755);
    log(L('[Aqua] Сборка Aqua Client — первый раз занимает 3–6 минут (скачиваются Minecraft и Gradle)…', '[Aqua] Building Aqua Client — first time takes 3–6 minutes (downloading Minecraft and Gradle)…'));
    let lines = 0;
    await new Promise((resolve, reject) => {
      const cmd = isWin ? 'gradlew.bat' : './gradlew';
      const child = spawn(cmd, ['build', '-x', 'copyToLauncher', '--no-daemon', '--console=plain'], {
        cwd: work, shell: isWin, env: { ...process.env, JAVA_HOME: home, GRADLE_USER_HOME: path.join(paths.root, 'gradle-home') },
      });
      const onData = (d) => String(d).split(/\r?\n/).filter(Boolean).forEach((l) => {
        lines++;
        log('[Gradle] ' + l);
        progress && progress({ task: L('Сборка Aqua Client (первый раз — несколько минут)', 'Building Aqua Client (first time takes a few minutes)'), done: Math.min(lines, 380), total: 400 });
      });
      child.stdout.on('data', onData);
      child.stderr.on('data', onData);
      child.on('error', reject);
      child.on('close', (code) => (code === 0 ? resolve() : reject(new Error(L(`Сборка Aqua Client завершилась с ошибкой (код ${code}). Подробности в Консоли.`, `Aqua Client build failed (code ${code}). See the Console.`)))));
    });
    const libs = path.join(work, 'build', 'libs');
    const jar = fs.readdirSync(libs).find((f) => f.endsWith('.jar') && !f.includes('sources'));
    if (!jar) throw new Error(L('Gradle не создал jar Aqua Client', 'Gradle did not produce the Aqua Client jar'));
    fs.mkdirSync(outDir, { recursive: true });
    fs.copyFileSync(path.join(libs, jar), outJar);
    fs.writeFileSync(stampFile, hash);
    log(L('[Aqua] Aqua Client собран ✓', '[Aqua] Aqua Client built ✓'));
    return outJar;
  })();
  try { return await building; } finally { building = null; }
}

module.exports = { ensureJar, status, JAR_NAME };
