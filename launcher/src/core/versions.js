const fs = require('fs');
const L = require('./i18n');
const path = require('path');
const paths = require('./paths');
const { getJson, download, cachedJson } = require('./net');

const MANIFEST = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json';
const HOUR = 3600e3;

async function manifest() {
  return cachedJson(MANIFEST, path.join(paths.cache, 'version_manifest_v2.json'), HOUR / 6);
}

/** Все версии Minecraft (релизы, снапшоты, beta, alpha). */
async function listVersions() {
  const m = await manifest();
  return {
    latest: m.latest,
    versions: m.versions.map((v) => ({ id: v.id, type: v.type, releaseTime: v.releaseTime })),
  };
}

async function versionJson(id) {
  const m = await manifest();
  const v = m.versions.find((x) => x.id === id);
  if (!v) throw new Error(L(`Версия ${id} не найдена`, `Version ${id} not found`));
  return cachedJson(v.url, path.join(paths.cache, 'versions', `${id}.json`), 1000 * HOUR);
}

// ------------------------------------------------------------------ ЗАГРУЗЧИКИ
const LOADERS = {
  fabric: 'https://meta.fabricmc.net/v2/versions/loader',
  quilt: 'https://meta.quiltmc.org/v3/versions/loader',
};

async function loaderVersions(loader, mc) {
  if (loader === 'fabric' || loader === 'quilt') {
    const list = await getJson(`${LOADERS[loader]}/${encodeURIComponent(mc)}`);
    return list.map((x) => ({ id: x.loader.version, stable: x.loader.stable !== false && !/beta|pre/i.test(x.loader.version) }));
  }
  if (loader === 'forge') {
    const meta = await cachedJson('https://files.minecraftforge.net/net/minecraftforge/forge/maven-metadata.json',
      path.join(paths.cache, 'forge-meta.json'), HOUR);
    let promos = {};
    try {
      promos = (await cachedJson('https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json',
        path.join(paths.cache, 'forge-promos.json'), HOUR)).promos || {};
    } catch { /* без рекомендаций */ }
    const rec = promos[`${mc}-recommended`], latest = promos[`${mc}-latest`];
    return (meta[mc] || []).slice().reverse().map((full) => {
      const v = full.split('-')[1];
      return { id: full, stable: v === rec || v === latest, label: v + (v === rec ? ' ★' : '') };
    });
  }
  if (loader === 'neoforge') {
    const data = await cachedJson('https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge',
      path.join(paths.cache, 'neoforge-meta.json'), HOUR);
    // 21.1.x → 1.21.1, 20.4.x → 1.20.4, 21.0.x → 1.21
    const parts = mc.split('.');
    const prefix = parts.length >= 2 ? `${parts[1]}.${parts[2] || 0}.` : '';
    return (data.versions || []).filter((v) => v.startsWith(prefix)).reverse()
      .map((v) => ({ id: v, stable: !/beta|alpha/i.test(v) }));
  }
  return [];
}

async function resolveLoaderVersion(loader, mc, wanted) {
  if (wanted && wanted !== 'latest') return wanted;
  const list = await loaderVersions(loader, mc);
  if (!list.length) throw new Error(L(`${loader} не поддерживает Minecraft ${mc}`, `${loader} does not support Minecraft ${mc}`));
  return (list.find((x) => x.stable) || list[0]).id;
}

/** Fabric/Quilt: кладём профиль версии в versions/<id>/<id>.json — MCLC запустит его как custom. */
async function installProfile(loader, mc, loaderVersion) {
  const lv = await resolveLoaderVersion(loader, mc, loaderVersion);
  const url = `${LOADERS[loader]}/${encodeURIComponent(mc)}/${encodeURIComponent(lv)}/profile/json`;
  const profile = await getJson(url);
  const id = profile.id;
  const dir = path.join(paths.shared, 'versions', id);
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, `${id}.json`), JSON.stringify(profile, null, 2));
  return { id, loaderVersion: lv };
}

/** Forge/NeoForge: скачиваем установщик, дальше его разбирает MCLC через ForgeWrapper. */
async function forgeInstaller(loader, mc, loaderVersion, onProgress) {
  const v = await resolveLoaderVersion(loader, mc, loaderVersion);
  const url = loader === 'forge'
    ? `https://maven.minecraftforge.net/net/minecraftforge/forge/${v}/forge-${v}-installer.jar`
    : `https://maven.neoforged.net/releases/net/neoforged/neoforge/${v}/neoforge-${v}-installer.jar`;
  const file = path.join(paths.shared, 'installers', `${loader}-${v}-installer.jar`);
  if (!fs.existsSync(file)) {
    await download(url, file, (d, t) => onProgress && onProgress({ task: L(`Скачивание ${loader === 'forge' ? 'Forge' : 'NeoForge'} ${v}`, `Downloading ${loader === 'forge' ? 'Forge' : 'NeoForge'} ${v}`), done: d, total: t }));
  }
  return { file, loaderVersion: v };
}

module.exports = { listVersions, versionJson, loaderVersions, resolveLoaderVersion, installProfile, forgeInstaller };
