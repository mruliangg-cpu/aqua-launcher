const fs = require('fs');
const L = require('./i18n');
const path = require('path');
const paths = require('./paths');
const store = require('./store');
const { getJson, download } = require('./net');

const MR = 'https://api.modrinth.com/v2';
const CF = 'https://api.curseforge.com/v1';
const CF_LOADER = { forge: 1, fabric: 4, quilt: 5, neoforge: 6 };
const CF_CLASS = { mod: 6, resourcepack: 12, shader: 6552 };
const FOLDER = { mod: 'mods', resourcepack: 'resourcepacks', shader: 'shaderpacks' };

/** Моды для максимального FPS — ставятся автоматически, если включён «FPS Boost». */
const FPS_PACK = {
  fabric: ['sodium', 'lithium', 'ferrite-core', 'entityculling', 'immediatelyfast', 'modernfix', 'dynamic-fps', 'moreculling', 'badoptimizations', 'krypton'],
  quilt: ['sodium', 'lithium', 'ferrite-core', 'entityculling', 'immediatelyfast', 'modernfix', 'dynamic-fps', 'badoptimizations'],
  forge: ['embeddium', 'ferrite-core', 'entityculling', 'modernfix', 'immediatelyfast'],
  neoforge: ['sodium', 'ferrite-core', 'entityculling', 'modernfix', 'immediatelyfast'],
};

function instance(id) {
  const inst = store.state.instances.find((i) => i.id === id);
  if (!inst) throw new Error(L('Сборка не найдена', 'Profile not found'));
  return inst;
}

function metaFile(inst) { return path.join(paths.instanceDir(inst.id), 'aqua-mods.json'); }
function readMeta(inst) { try { return JSON.parse(fs.readFileSync(metaFile(inst), 'utf8')); } catch { return {}; } }
function writeMeta(inst, meta) {
  fs.mkdirSync(paths.instanceDir(inst.id), { recursive: true });
  fs.writeFileSync(metaFile(inst), JSON.stringify(meta, null, 2));
}

function mrLoaders(inst, type) {
  if (type !== 'mod') return null;
  if (inst.loader === 'quilt') return ['quilt', 'fabric'];
  return [inst.loader];
}

// ------------------------------------------------------------------ ПОИСК
async function search({ source, type = 'mod', query = '', offset = 0, instanceId }) {
  const inst = instance(instanceId);
  if (type === 'mod' && inst.loader === 'vanilla') throw new Error(L('Для модов нужна сборка с Fabric, Quilt, Forge или NeoForge.', 'Mods need a Fabric, Quilt, Forge or NeoForge profile.'));
  if (source === 'curseforge') return searchCurse(inst, type, query, offset);

  const facets = [[`project_type:${type === 'shader' ? 'shader' : type}`], [`versions:${inst.mc}`]];
  const loaders = mrLoaders(inst, type);
  if (loaders) facets.push(loaders.map((l) => `categories:${l}`));
  const url = `${MR}/search?limit=20&offset=${offset}&index=${query ? 'relevance' : 'downloads'}&query=${encodeURIComponent(query)}&facets=${encodeURIComponent(JSON.stringify(facets))}`;
  const data = await getJson(url);
  const installed = readMeta(inst);
  return {
    total: data.total_hits,
    hits: data.hits.map((h) => ({
      source: 'modrinth', id: h.project_id, slug: h.slug, title: h.title, author: h.author,
      description: h.description, icon: h.icon_url, downloads: h.downloads, follows: h.follows,
      url: `https://modrinth.com/${h.project_type}/${h.slug}`,
      installed: Object.values(installed).some((m) => m.projectId === h.project_id),
    })),
  };
}

/** CurseForge: свой ключ из настроек или прокси Aqua Network (ключ хранится на сервере). */
function cf() {
  const st = store.state.settings;
  if (st.curseforgeKey) return { base: CF, headers: { 'x-api-key': st.curseforgeKey }, dl: (u) => ({ url: u, headers: { 'x-api-key': st.curseforgeKey } }) };
  const net = (st.networkUrl || '').replace(/\/+$/, '');
  if (net) return { base: net + '/cf/v1', headers: {}, dl: (u) => ({ url: `${net}/cfdl?url=${encodeURIComponent(u)}`, headers: {} }) };
  throw new Error(L('CurseForge: укажите адрес Aqua Network или свой API-ключ в Настройках (ключ бесплатный на console.curseforge.com).',
    'CurseForge: set your Aqua Network URL or your own API key in Settings (free key at console.curseforge.com).'));
}
function cfHeaders() { return cf().headers; }

async function searchCurse(inst, type, query, offset) {
  const params = new URLSearchParams({
    gameId: '432', classId: String(CF_CLASS[type]), searchFilter: query, gameVersion: inst.mc,
    sortField: query ? '1' : '6', sortOrder: 'desc', pageSize: '20', index: String(offset),
  });
  if (type === 'mod' && CF_LOADER[inst.loader]) params.set('modLoaderType', String(CF_LOADER[inst.loader]));
  const data = await getJson(`${cf().base}/mods/search?${params}`, cfHeaders());
  const installed = readMeta(inst);
  return {
    total: data.pagination ? data.pagination.totalCount : data.data.length,
    hits: data.data.map((m) => ({
      source: 'curseforge', id: String(m.id), slug: m.slug, title: m.name,
      author: m.authors && m.authors[0] ? m.authors[0].name : '', description: m.summary,
      icon: m.logo ? m.logo.thumbnailUrl : null, downloads: m.downloadCount,
      url: m.links ? m.links.websiteUrl : `https://www.curseforge.com/minecraft/mc-mods/${m.slug}`,
      installed: Object.values(installed).some((x) => x.projectId === String(m.id)),
    })),
  };
}

// ------------------------------------------------------------------ УСТАНОВКА
async function install({ source, projectId, instanceId, type = 'mod' }, onLog) {
  const inst = instance(instanceId);
  const meta = readMeta(inst);
  const seen = new Set();
  const installed = [];
  const fn = source === 'curseforge' ? installCurse : installModrinth;
  await fn(inst, projectId, type, meta, seen, installed, onLog, true);
  writeMeta(inst, meta);
  return installed;
}

async function installModrinth(inst, projectId, type, meta, seen, installed, onLog, required) {
  if (seen.has(projectId)) return;
  seen.add(projectId);
  if (Object.values(meta).some((m) => m.projectId === projectId)) return; // уже стоит
  const params = new URLSearchParams({ game_versions: JSON.stringify([inst.mc]) });
  const loaders = mrLoaders(inst, type);
  if (loaders) params.set('loaders', JSON.stringify(loaders));
  const versions = await getJson(`${MR}/project/${encodeURIComponent(projectId)}/version?${params}`);
  const version = versions.find((v) => v.version_type === 'release') || versions[0];
  if (!version) {
    if (required) throw new Error(L(`Нет версии для Minecraft ${inst.mc} (${inst.loader})`, `No version for Minecraft ${inst.mc} (${inst.loader})`));
    onLog && onLog(L(`[Моды] ${projectId}: нет версии для ${inst.mc}, пропускаю`, `[Mods] ${projectId}: no version for ${inst.mc}, skipping`));
    return;
  }
  const file = version.files.find((f) => f.primary) || version.files[0];
  const dest = path.join(paths.instanceDir(inst.id), FOLDER[type], file.filename);
  if (!fs.existsSync(dest)) await download(file.url, dest);
  const project = await getJson(`${MR}/project/${encodeURIComponent(projectId)}`).catch(() => null);
  meta[file.filename] = {
    source: 'modrinth', projectId: version.project_id, title: project ? project.title : file.filename,
    icon: project ? project.icon_url : null, version: version.version_number, type,
  };
  installed.push(meta[file.filename].title);
  onLog && onLog(L(`[Моды] Установлен ${meta[file.filename].title} ${version.version_number}`, `[Mods] Installed ${meta[file.filename].title} ${version.version_number}`));
  for (const dep of version.dependencies || []) {
    if (dep.dependency_type === 'required' && dep.project_id) {
      await installModrinth(inst, dep.project_id, 'mod', meta, seen, installed, onLog, false);
    }
  }
}

async function installCurse(inst, modId, type, meta, seen, installed, onLog, required) {
  if (seen.has(modId)) return;
  seen.add(modId);
  if (Object.values(meta).some((m) => m.projectId === String(modId))) return;
  const params = new URLSearchParams({ gameVersion: inst.mc, pageSize: '20' });
  if (type === 'mod' && CF_LOADER[inst.loader]) params.set('modLoaderType', String(CF_LOADER[inst.loader]));
  const files = (await getJson(`${cf().base}/mods/${modId}/files?${params}`, cfHeaders())).data || [];
  const file = files.find((f) => f.releaseType === 1) || files[0];
  if (!file) {
    if (required) throw new Error(L(`Нет файла для Minecraft ${inst.mc} (${inst.loader})`, `No file for Minecraft ${inst.mc} (${inst.loader})`));
    return;
  }
  if (!file.downloadUrl) {
    throw new Error(L(`Автор «${file.displayName}» запретил скачивание через сторонние лаунчеры — скачайте вручную на CurseForge.`, `The author of “${file.displayName}” disabled third-party downloads — download it manually from CurseForge.`));
  }
  const dest = path.join(paths.instanceDir(inst.id), FOLDER[type], file.fileName);
  if (!fs.existsSync(dest)) { const d = cf().dl(file.downloadUrl); await download(d.url, dest, null, d.headers); }
  const mod = (await getJson(`${cf().base}/mods/${modId}`, cfHeaders()).catch(() => ({ data: null }))).data;
  meta[file.fileName] = {
    source: 'curseforge', projectId: String(modId), title: mod ? mod.name : file.displayName,
    icon: mod && mod.logo ? mod.logo.thumbnailUrl : null, version: file.displayName, type,
  };
  installed.push(meta[file.fileName].title);
  onLog && onLog(L(`[Моды] Установлен ${meta[file.fileName].title}`, `[Mods] Installed ${meta[file.fileName].title}`));
  for (const dep of file.dependencies || []) {
    if (dep.relationType === 3) await installCurse(inst, String(dep.modId), 'mod', meta, seen, installed, onLog, false);
  }
}

// ------------------------------------------------------------------ УСТАНОВЛЕННЫЕ
function listInstalled(instanceId, type = 'mod') {
  const inst = instance(instanceId);
  const dir = path.join(paths.instanceDir(inst.id), FOLDER[type]);
  const meta = readMeta(inst);
  if (!fs.existsSync(dir)) return [];
  return fs.readdirSync(dir)
    .filter((f) => /\.(jar|zip)(\.disabled)?$/i.test(f) || fs.statSync(path.join(dir, f)).isDirectory())
    .map((f) => {
      const base = f.replace(/\.disabled$/, '');
      const m = meta[base] || {};
      return { file: f, title: m.title || base.replace(/\.(jar|zip)$/i, ''), icon: m.icon || null, version: m.version || '', enabled: !f.endsWith('.disabled'), source: m.source || 'local' };
    })
    .sort((a, b) => a.title.localeCompare(b.title));
}

const PROTECTED = /^aqua-client-.*\.jar(\.disabled)?$/i;
function toggle(instanceId, file, type = 'mod') {
  if (PROTECTED.test(file)) throw new Error(L('Aqua Client нельзя отключить', 'Aqua Client cannot be disabled'));
  const dir = path.join(paths.instanceDir(instanceId), FOLDER[type]);
  const target = file.endsWith('.disabled') ? file.slice(0, -9) : file + '.disabled';
  fs.renameSync(path.join(dir, file), path.join(dir, target));
  return target;
}

function remove(instanceId, file, type = 'mod') {
  if (PROTECTED.test(file)) throw new Error(L('Aqua Client нельзя удалить', 'Aqua Client cannot be removed'));
  const inst = instance(instanceId);
  fs.rmSync(path.join(paths.instanceDir(inst.id), FOLDER[type], file), { recursive: true, force: true });
  const meta = readMeta(inst);
  delete meta[file.replace(/\.disabled$/, '')];
  writeMeta(inst, meta);
}

/** FPS-пак + Aqua Client — вызывается перед запуском. */
async function prepareInstance(inst, onLog, onProgress) {
  const meta = readMeta(inst);
  const want = [];
  if (inst.fpsBoost && FPS_PACK[inst.loader]) want.push(...FPS_PACK[inst.loader]);
  const ac = require('./aquaclient');
  const ender = inst.loader === 'fabric' && (!!ac.jarFor(inst.mc) || ['1.21', '1.21.1'].includes(inst.mc)); // Aqua Client всегда установлен
  if (ender) want.push('fabric-api');
  const key = `${inst.mc}|${inst.loader}|${want.join(',')}`;
  if (want.length && inst.preparedFor !== key) {
    onLog && onLog(L('[Aqua] Подготовка модов для FPS-буста…', '[Aqua] Preparing FPS Boost mods…'));
    const seen = new Set(), installed = [];
    for (const slug of want) {
      try {
        const project = await getJson(`${MR}/project/${slug}`);
        await installModrinth(inst, project.id, 'mod', meta, seen, installed, onLog, false);
      } catch (e) {
        onLog && onLog(`[Aqua] ${slug}: ${e.message}`);
      }
    }
    writeMeta(inst, meta);
    inst.preparedFor = key;
    store.save();
  }

  const modsDir = path.join(paths.instanceDir(inst.id), 'mods');
  const target = path.join(modsDir, `aqua-client-${inst.mc}.jar`);
  fs.rmSync(path.join(modsDir, 'ender-client-1.21.1.jar'), { force: true }); // старое имя мода
  try { for (const f of fs.readdirSync(modsDir)) if (/^aqua-client-.*\.jar$/.test(f) && path.join(modsDir, f) !== target) fs.rmSync(path.join(modsDir, f), { force: true }); } catch {}
  if (ender) {
    const source = ac.jarFor(inst.mc) || await ac.ensureJar(onLog, onProgress);
    fs.mkdirSync(modsDir, { recursive: true });
    const need = !fs.existsSync(target) || fs.statSync(target).size !== fs.statSync(source).size;
    if (need) fs.copyFileSync(source, target);
    onLog && onLog(L('[Aqua] Aqua Client подключён — в игре нажмите правый Shift', '[Aqua] Aqua Client enabled — press Right Shift in game'));
  } else if (fs.existsSync(target)) {
    fs.rmSync(target, { force: true });
  }
}

module.exports = { search, install, listInstalled, toggle, remove, prepareInstance, FOLDER };
