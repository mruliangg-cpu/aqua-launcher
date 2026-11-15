/* Минимальный NBT (без сжатия) — чтобы добавлять серверы лаунчера в servers.dat игры. */
const T = { END: 0, BYTE: 1, SHORT: 2, INT: 3, LONG: 4, FLOAT: 5, DOUBLE: 6, BYTE_ARRAY: 7, STRING: 8, LIST: 9, COMPOUND: 10, INT_ARRAY: 11, LONG_ARRAY: 12 };

function read(buf) {
  let o = 0;
  const u8 = () => buf.readUInt8(o++);
  const str = () => { const n = buf.readUInt16BE(o); o += 2; const s = buf.toString('utf8', o, o + n); o += n; return s; };
  const payload = (t) => {
    switch (t) {
      case T.BYTE: return buf.readInt8(o++);
      case T.SHORT: { const v = buf.readInt16BE(o); o += 2; return v; }
      case T.INT: { const v = buf.readInt32BE(o); o += 4; return v; }
      case T.LONG: { const v = buf.readBigInt64BE(o); o += 8; return v; }
      case T.FLOAT: { const v = buf.readFloatBE(o); o += 4; return v; }
      case T.DOUBLE: { const v = buf.readDoubleBE(o); o += 8; return v; }
      case T.BYTE_ARRAY: { const n = buf.readInt32BE(o); o += 4; const v = buf.slice(o, o + n); o += n; return v; }
      case T.STRING: return str();
      case T.LIST: { const et = u8(); const n = buf.readInt32BE(o); o += 4; const items = []; for (let i = 0; i < n; i++) items.push(payload(et)); return { et, items }; }
      case T.COMPOUND: { const m = []; for (;;) { const ct = u8(); if (ct === T.END) break; const name = str(); m.push({ t: ct, name, v: payload(ct) }); } return m; }
      case T.INT_ARRAY: { const n = buf.readInt32BE(o); o += 4; const a = []; for (let i = 0; i < n; i++) { a.push(buf.readInt32BE(o)); o += 4; } return a; }
      case T.LONG_ARRAY: { const n = buf.readInt32BE(o); o += 4; const a = []; for (let i = 0; i < n; i++) { a.push(buf.readBigInt64BE(o)); o += 8; } return a; }
      default: throw new Error('NBT: unknown tag ' + t);
    }
  };
  const t = u8(); const name = str();
  return { t, name, v: payload(t) };
}

function write(root) {
  const parts = [];
  const push = (b) => parts.push(b);
  const u8 = (v) => push(Buffer.from([v & 255]));
  const str = (s) => { const b = Buffer.from(s, 'utf8'); const h = Buffer.alloc(2); h.writeUInt16BE(b.length); push(h); push(b); };
  const payload = (t, v) => {
    const b = (n, f) => { const x = Buffer.alloc(n); f(x); push(x); };
    switch (t) {
      case T.BYTE: return b(1, (x) => x.writeInt8(v));
      case T.SHORT: return b(2, (x) => x.writeInt16BE(v));
      case T.INT: return b(4, (x) => x.writeInt32BE(v));
      case T.LONG: return b(8, (x) => x.writeBigInt64BE(BigInt(v)));
      case T.FLOAT: return b(4, (x) => x.writeFloatBE(v));
      case T.DOUBLE: return b(8, (x) => x.writeDoubleBE(v));
      case T.BYTE_ARRAY: b(4, (x) => x.writeInt32BE(v.length)); return push(Buffer.from(v));
      case T.STRING: return str(v);
      case T.LIST: u8(v.et); b(4, (x) => x.writeInt32BE(v.items.length)); return v.items.forEach((i) => payload(v.et, i));
      case T.COMPOUND: v.forEach((e) => { u8(e.t); str(e.name); payload(e.t, e.v); }); return u8(T.END);
      case T.INT_ARRAY: b(4, (x) => x.writeInt32BE(v.length)); return v.forEach((i) => b(4, (x) => x.writeInt32BE(i)));
      case T.LONG_ARRAY: b(4, (x) => x.writeInt32BE(v.length)); return v.forEach((i) => b(8, (x) => x.writeBigInt64BE(BigInt(i))));
      default: throw new Error('NBT: unknown tag ' + t);
    }
  };
  u8(root.t); str(root.name); payload(root.t, root.v);
  return Buffer.concat(parts);
}

/** Добавляет серверы в servers.dat, не трогая уже существующие. */
function mergeServers(file, servers) {
  const fs = require('fs');
  let root = { t: T.COMPOUND, name: '', v: [] };
  try { if (fs.existsSync(file)) root = read(fs.readFileSync(file)); } catch { /* повреждён — создадим заново */ }
  let list = root.v.find((e) => e.name === 'servers');
  if (!list) { list = { t: T.LIST, name: 'servers', v: { et: T.COMPOUND, items: [] } }; root.v.push(list); }
  if (list.v.et !== T.COMPOUND) list.v = { et: T.COMPOUND, items: [] };
  const have = new Set(list.v.items.map((c) => (c.find((e) => e.name === 'ip') || {}).v));
  let added = 0;
  for (const s of servers) {
    if (!s.address || have.has(s.address)) continue;
    list.v.items.push([{ t: T.STRING, name: 'name', v: s.name || s.address }, { t: T.STRING, name: 'ip', v: s.address }]);
    added++;
  }
  if (added) fs.writeFileSync(file, write(root));
  return added;
}

module.exports = { read, write, mergeServers, T };
