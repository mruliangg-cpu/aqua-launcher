/* Пинг серверов Minecraft (Server List Ping) — онлайн, MOTD, иконка, задержка. */
const net = require('net');
const L = require('./i18n');
const dns = require('dns').promises;

function varint(n) {
  const out = [];
  do { let b = n & 0x7f; n >>>= 7; if (n) b |= 0x80; out.push(b); } while (n);
  return Buffer.from(out);
}
function readVarint(buf, off) {
  let n = 0, shift = 0, b;
  do {
    if (off >= buf.length) return null;
    b = buf[off++];
    n |= (b & 0x7f) << shift;
    shift += 7;
  } while (b & 0x80);
  return [n, off];
}

async function resolve(host, port) {
  if (port !== 25565 || /^\d+\.\d+\.\d+\.\d+$/.test(host)) return { host, port };
  try {
    const srv = await dns.resolveSrv(`_minecraft._tcp.${host}`);
    if (srv && srv[0]) return { host: srv[0].name, port: srv[0].port };
  } catch { /* SRV нет */ }
  return { host, port };
}

function stripCodes(motd) {
  if (!motd) return '';
  if (typeof motd === 'string') return motd.replace(/§./g, '');
  let s = motd.text || '';
  if (Array.isArray(motd.extra)) s += motd.extra.map(stripCodes).join('');
  return s.replace(/§./g, '');
}

async function ping(address, timeout = 5000) {
  let [host, p] = address.split(':');
  const target = await resolve(host, Number(p) || 25565);
  return new Promise((ok, fail) => {
    const sock = net.createConnection(target.port, target.host);
    let buf = Buffer.alloc(0), started = 0, done = false;
    const finish = (err, val) => { if (done) return; done = true; sock.destroy(); err ? fail(err) : ok(val); };
    sock.setTimeout(timeout, () => finish(new Error(L('Таймаут', 'Timeout'))));
    sock.on('error', (e) => finish(e));
    sock.on('connect', () => {
      const h = Buffer.from(host, 'utf8');
      const portBuf = Buffer.alloc(2); portBuf.writeUInt16BE(target.port);
      const data = Buffer.concat([varint(0), varint(767), varint(h.length), h, portBuf, varint(1)]);
      sock.write(Buffer.concat([varint(data.length), data]));
      sock.write(Buffer.from([1, 0]));
      started = Date.now();
    });
    sock.on('data', (chunk) => {
      buf = Buffer.concat([buf, chunk]);
      const len = readVarint(buf, 0); if (!len) return;
      const [plen, o1] = len;
      if (buf.length < o1 + plen) return;
      const id = readVarint(buf, o1); const sl = readVarint(buf, id[1]);
      const json = buf.slice(sl[1], sl[1] + sl[0]).toString('utf8');
      try {
        const d = JSON.parse(json);
        finish(null, {
          online: true, ping: Date.now() - started, version: d.version ? d.version.name : '',
          players: d.players ? { online: d.players.online, max: d.players.max } : null,
          motd: stripCodes(d.description), favicon: d.favicon || null,
        });
      } catch (e) { finish(e); }
    });
  });
}

module.exports = { ping };
