const fs = require('fs');
const L = require('./i18n');
const path = require('path');
const os = require('os');
const paths = require('./paths');
const { download } = require('./net');

const isWin = process.platform === 'win32';

/** Ищет bin/java(w) внутри распакованной Java. */
function findJava(dir, depth = 0) {
  if (!fs.existsSync(dir) || depth > 5) return null;
  const exe = isWin ? 'javaw.exe' : 'java';
  const direct = path.join(dir, 'bin', exe);
  if (fs.existsSync(direct)) return direct;
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (entry.isDirectory()) {
      const found = findJava(path.join(dir, entry.name), depth + 1);
      if (found) return found;
    }
  }
  return null;
}

function adoptiumTarget(major) {
  const osName = isWin ? 'windows' : process.platform === 'darwin' ? 'mac' : 'linux';
  let arch = process.arch === 'arm64' ? 'aarch64' : process.arch === 'ia32' ? 'x32' : 'x64';
  // Java 8 под Apple Silicon у Adoptium нет — используем x64 через Rosetta
  if (osName === 'mac' && arch === 'aarch64' && major === 8) arch = 'x64';
  return { osName, arch };
}

/**
 * Возвращает путь к Java нужной версии; если её нет — скачивает Temurin JRE.
 * Minecraft до 1.16 → Java 8, 1.17–1.20.4 → 17, 1.20.5+ → 21.
 */
async function ensureJava(major, onProgress, imageType = 'jre') {
  const dir = path.join(paths.runtime, `${imageType === 'jdk' ? 'jdk' : 'java'}-${major}`);
  const existing = findJava(dir);
  if (existing) return existing;

  const { osName, arch } = adoptiumTarget(major);
  const url = `https://api.adoptium.net/v3/binary/latest/${major}/ga/${osName}/${arch}/${imageType}/hotspot/normal/eclipse?project=jdk`;
  const archive = path.join(paths.cache, `${imageType}-${major}.${isWin ? 'zip' : 'tar.gz'}`);
  await download(url, archive, (d, t) => onProgress && onProgress({ task: L(`Скачивание ${imageType === 'jdk' ? 'JDK' : 'Java'} ${major}`, `Downloading ${imageType === 'jdk' ? 'JDK' : 'Java'} ${major}`), done: d, total: t }));

  onProgress && onProgress({ task: L(`Распаковка Java ${major}`, `Extracting Java ${major}`), done: 1, total: 1 });
  fs.rmSync(dir, { recursive: true, force: true });
  fs.mkdirSync(dir, { recursive: true });
  if (isWin) {
    const AdmZip = require('adm-zip');
    new AdmZip(archive).extractAllTo(dir, true);
  } else {
    const tar = require('tar');
    await tar.x({ file: archive, cwd: dir });
  }
  fs.rmSync(archive, { force: true });
  const java = findJava(dir);
  if (!java) throw new Error(L(`Java ${major} скачана, но не найдена в ${dir}`, `Java ${major} was downloaded but not found in ${dir}`));
  if (!isWin) {
    for (const f of fs.readdirSync(path.dirname(java))) fs.chmodSync(path.join(path.dirname(java), f), 0o755);
  }
  return java;
}

/** Каталог JAVA_HOME для найденного bin/java. */
function javaHome(javaBin) { return path.dirname(path.dirname(javaBin)); }

function totalMemoryMb() { return Math.floor(os.totalmem() / 1024 / 1024); }

module.exports = { ensureJava, javaHome, totalMemoryMb };
