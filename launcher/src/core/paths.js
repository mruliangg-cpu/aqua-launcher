const path = require('path');
const fs = require('fs');
const { app } = require('electron');

const root = path.join(app.getPath('appData'), '.aqualauncher');
const p = {
  root,
  shared: path.join(root, 'minecraft'),          // versions / libraries / assets — общие для всех сборок
  instances: path.join(root, 'instances'),
  runtime: path.join(root, 'runtime'),           // скачанная Java
  cache: path.join(root, 'cache'),
  store: path.join(root, 'launcher.json'),
  instanceDir: (id) => path.join(root, 'instances', id),
  /** Встроенные файлы (jar Aqua Client). В dev — ./resources, в сборке — process.resourcesPath/resources */
  resources: app.isPackaged ? path.join(process.resourcesPath, 'resources') : path.join(__dirname, '..', '..', 'resources'),
};

for (const d of [p.root, p.shared, p.instances, p.runtime, p.cache]) fs.mkdirSync(d, { recursive: true });

module.exports = p;
