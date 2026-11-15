const fs = require('fs');
const L = require('./i18n');
const path = require('path');

const UA = 'AquaLauncher/1.0.0 (aqua-launcher; contact: github.com)';

async function getJson(url, headers = {}) {
  const res = await fetch(url, { headers: { 'User-Agent': UA, Accept: 'application/json', ...headers } });
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`HTTP ${res.status} — ${url}${text ? ': ' + text.slice(0, 200) : ''}`);
  }
  return res.json();
}

/** Скачивает файл потоком во временный файл, затем атомарно переименовывает. */
async function download(url, dest, onProgress, headers = {}) {
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  const res = await fetch(url, { headers: { 'User-Agent': UA, ...headers }, redirect: 'follow' });
  if (!res.ok || !res.body) throw new Error(L(`Не удалось скачать (${res.status}): ${url}`, `Download failed (${res.status}): ${url}`));
  const total = Number(res.headers.get('content-length')) || 0;
  const tmp = dest + '.part';
  const out = fs.createWriteStream(tmp);
  let done = 0, lastEmit = 0;
  const reader = res.body.getReader();
  try {
    for (;;) {
      const { done: finished, value } = await reader.read();
      if (finished) break;
      done += value.length;
      if (!out.write(Buffer.from(value))) await new Promise((r) => out.once('drain', r));
      const now = Date.now();
      if (onProgress && now - lastEmit > 100) { lastEmit = now; onProgress(done, total); }
    }
  } catch (e) {
    out.destroy();
    fs.rmSync(tmp, { force: true });
    throw e;
  }
  await new Promise((resolve, reject) => out.end((err) => (err ? reject(err) : resolve())));
  fs.renameSync(tmp, dest);
  if (onProgress) onProgress(done, total || done);
  return dest;
}

/** Кэш JSON на диске: если интернета нет — берём последнюю сохранённую копию. */
async function cachedJson(url, cacheFile, maxAgeMs, headers) {
  try {
    if (fs.existsSync(cacheFile) && Date.now() - fs.statSync(cacheFile).mtimeMs < maxAgeMs) {
      return JSON.parse(fs.readFileSync(cacheFile, 'utf8'));
    }
  } catch { /* перекачаем */ }
  try {
    const data = await getJson(url, headers);
    fs.mkdirSync(path.dirname(cacheFile), { recursive: true });
    fs.writeFileSync(cacheFile, JSON.stringify(data));
    return data;
  } catch (e) {
    if (fs.existsSync(cacheFile)) return JSON.parse(fs.readFileSync(cacheFile, 'utf8'));
    throw e;
  }
}

module.exports = { UA, getJson, download, cachedJson };
