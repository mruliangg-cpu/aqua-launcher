/* Aqua 3D Viewer — игрок со скином и 3D-косметикой (те же модели, что в Aqua Client).
   Ортографическая проекция, текстуры на гранях через аффинные преобразования canvas. */
(function () {
  const DATA = () => window.COSMETICS_DATA;
  const find = (slot, id) => (DATA()?.items[slot] || []).find((x) => x.id === id);

  // ---------- математика ----------
  const rotY = (a) => (p) => { const c = Math.cos(a), s = Math.sin(a); return [p[0] * c + p[2] * s, p[1], -p[0] * s + p[2] * c]; };
  const rotX = (a) => (p) => { const c = Math.cos(a), s = Math.sin(a); return [p[0], p[1] * c - p[2] * s, p[1] * s + p[2] * c]; };
  const tr = (x, y, z) => (p) => [p[0] + x, p[1] + y, p[2] + z];
  const sc = (x, y, z) => (p) => [p[0] * x, p[1] * y, p[2] * z];
  const chain = (fs) => (p) => { for (let i = fs.length - 1; i >= 0; i--) p = fs[i](p); return p; }; // последний применяется первым
  const deg = Math.PI / 180;

  function animXf(a, t, flying) {
    const flap = flying ? Math.sin(t * 10) * 35 : Math.sin(t * 2.5) * 12;
    switch (a) {
      case 'spin': return rotY(-t * 40 * deg);
      case 'spinFast': return rotY(-t * 90 * deg);
      case 'propeller': return rotY(-t * 720 * deg);
      case 'bobHalo': return tr(0, Math.sin(t * 2) * 0.6, 0);
      case 'bob': return tr(0, Math.sin(t * 2.2) * 1.2, 0);
      case 'sway': return rotY(-Math.sin(t * 0.8) * 35 * deg);
      case 'wiggle': return tr(Math.sin(t * 8) * 0.8, 0, 0);
      case 'buzz': return tr(0, -Math.abs(Math.sin(t * 30)) * 0.8, 0);
      case 'flicker': return sc(1, 0.6 + Math.abs(Math.sin(t * 20)) * 0.8, 1);
      case 'flapL': return rotY(-(18 + flap) * deg);
      case 'flapR': return rotY((18 + flap) * deg);
      default: return (p) => p;
    }
  }

  const hex = (argb) => {
    const a = ((argb >>> 24) & 255) / 255, r = (argb >>> 16) & 255, g = (argb >>> 8) & 255, b = argb & 255;
    return [r, g, b, a];
  };

  // Грани кубоида: углы для текстуры (TL, TR, BL) + нормаль + UV-прямоугольник
  function cuboidFaces(x1, y1, z1, x2, y2, z2, uv) {
    const [u, v, w, h, d] = uv || [0, 0, 0, 0, 0];
    return [
      { n: [0, 0, -1], c: [[x1, y1, z1], [x2, y1, z1], [x1, y2, z1]], uv: [u + d, v + d, w, h] },          // перед
      { n: [0, 0, 1], c: [[x2, y1, z2], [x1, y1, z2], [x2, y2, z2]], uv: [u + 2 * d + w, v + d, w, h] },  // зад
      { n: [-1, 0, 0], c: [[x1, y1, z2], [x1, y1, z1], [x1, y2, z2]], uv: [u, v + d, d, h] },              // правый бок
      { n: [1, 0, 0], c: [[x2, y1, z1], [x2, y1, z2], [x2, y2, z1]], uv: [u + d + w, v + d, d, h] },      // левый бок
      { n: [0, -1, 0], c: [[x1, y1, z2], [x2, y1, z2], [x1, y1, z1]], uv: [u + d, v, w, d] },              // верх
      { n: [0, 1, 0], c: [[x1, y2, z1], [x2, y2, z1], [x1, y2, z2]], uv: [u + d + w, v, w, d] },          // низ
    ];
  }
  const SHADE = (n) => (n[1] < 0 ? 1 : n[1] > 0 ? 0.55 : n[2] !== 0 ? 0.86 : 0.72);

  // ---------- сборка сцены ----------
  function build(opts, t) {
    const faces = [];
    const { skin, slim, loadout = {}, capeImg, flying = false } = opts;
    const legacy = skin && skin.height === 32;
    const aw = slim ? 3 : 4;
    const push = (xf, box, tex, uv, color, inflate = 0) => {
      const [x1, y1, z1, x2, y2, z2] = box;
      for (const f of cuboidFaces(x1 - inflate, y1 - inflate, z1 - inflate, x2 + inflate, y2 + inflate, z2 + inflate, uv)) {
        const pts = f.c.map(xf);
        const o = xf([0, 0, 0]), nn = xf(f.n);
        f.wn = [nn[0] - o[0], nn[1] - o[1], nn[2] - o[2]];
        f.p = pts; f.tex = tex; f.color = color; f.overlay = inflate > 0; f.glow = ((box[7] | 0) & 1) === 1; f.flat = ((box[7] | 0) & 2) === 2;
        faces.push(f);
      }
    };
    const swing = Math.sin(t * 1.6) * 6 * deg;
    const base = (p) => p;
    // скин
    if (skin) {
      const part = (xf, box, uv, uv2, inf) => { push(xf, box, skin, uv); if (uv2) push(xf, box, skin, uv2, null, inf); };
      part(base, [-4, -8, -4, 4, 0, 4], [0, 0, 8, 8, 8], [32, 0, 8, 8, 8], 0.5);
      part(base, [-4, 0, -2, 4, 12, 2], [16, 16, 8, 12, 4], legacy ? null : [16, 32, 8, 12, 4], 0.25);
      const rArm = chain([tr(-5, 2, 0), rotX(swing), tr(5, -2, 0)]);
      const lArm = chain([tr(5, 2, 0), rotX(-swing), tr(-5, -2, 0)]);
      part(rArm, [-4 - aw, 0, -2, -4, 12, 2], [40, 16, aw, 12, 4], legacy ? null : [40, 32, aw, 12, 4], 0.25);
      part(lArm, [4, 0, -2, 4 + aw, 12, 2], legacy ? [40, 16, aw, 12, 4] : [32, 48, aw, 12, 4], legacy ? null : [48, 48, aw, 12, 4], 0.25);
      part(base, [-4, 12, -2, 0, 24, 2], [0, 16, 4, 12, 4], legacy ? null : [0, 32, 4, 12, 4], 0.25);
      part(base, [0, 12, -2, 4, 24, 2], legacy ? [0, 16, 4, 12, 4] : [16, 48, 4, 12, 4], legacy ? null : [0, 48, 4, 12, 4], 0.25);
    }
    // плащ
    if (capeImg) {
      const xf = chain([tr(0, 0, 2.1), rotX((8 + Math.sin(t * 1.3) * 4) * deg)]);
      const [x1, y1, z1, x2, y2, z2] = [-5, 0, 0, 5, 16, 1];
      const fs = cuboidFaces(x1, y1, z1, x2, y2, z2, [0, 0, 10, 16, 1]);
      fs[1].uv = [1, 1, 10, 16]; fs[0].uv = [12, 1, 10, 16];
      for (const f of fs) {
        f.p = f.c.map(xf); const o = xf([0, 0, 0]), nn = xf(f.n); f.wn = [nn[0] - o[0], nn[1] - o[1], nn[2] - o[2]];
        f.tex = capeImg; faces.push(f);
      }
    }
    // 3D-косметика
    for (const slot of ['hat', 'face', 'back', 'wings', 'pet']) {
      const it = find(slot, loadout[slot]);
      if (!it || !it.g) continue;
      for (const g of it.g) {
        const xf = chain([tr(g.o[0], g.o[1], g.o[2]), ...g.a.map((a) => animXf(a, t, flying))]);
        for (const b of g.b) push(xf, b, null, null, b[6]);
        if (g.sp) {
          const img = spriteImg(g.sp.s);
          if (!img) continue;
          // HD-пластина + тёмный слой позади (объём края)
          for (const [z, tex] of [[0.05, img.dark], [0.3, img.img]]) {
            const c = [[0, g.sp.y1, z], [g.sp.w, g.sp.y1, z], [0, g.sp.y2, z]];
            faces.push({ n: [0, 0, 1], c, p: c.map(xf), wn: [0, 0, 1], tex, full: true, nocull: true, glow: !!g.sp.gl, uv: [0, 0, 1, 1] });
          }
        }
      }
    }
    return faces;
  }

  const SPR = {};
  function spriteImg(id) {
    if (SPR[id]) return SPR[id].ready ? SPR[id] : null;
    const e = SPR[id] = { ready: false };
    const i = new Image();
    i.onload = () => {
      const c = document.createElement('canvas'); c.width = i.width; c.height = i.height;
      const x = c.getContext('2d'); x.drawImage(i, 0, 0); x.globalCompositeOperation = 'source-atop'; x.fillStyle = 'rgba(0,10,25,.55)'; x.fillRect(0, 0, c.width, c.height);
      Object.assign(e, { img: i, dark: c, ready: true });
    };
    i.src = `assets/wings/${id}.png`;
    return null;
  }
  let BEVEL = null;
  { const i = new Image(); i.onload = () => (BEVEL = i); i.src = 'assets/bevel.png'; }

  // ---------- отрисовка ----------
  function render(canvas, opts, t) {
    const g = canvas.getContext('2d');
    const W = canvas.width, H = canvas.height;
    g.setTransform(1, 0, 0, 1, 0, 0);
    g.clearRect(0, 0, W, H);
    g.imageSmoothingEnabled = false;
    const yaw = opts.yaw ?? 0.5, pitch = opts.pitch ?? -0.12;
    const scale = opts.scale || Math.min(H / 50, W / 46);
    const cx = W / 2 + (opts.offsetX || 0), cy = (opts.centerY ?? H * 0.5) ;
    const view = chain([rotX(pitch), rotY(yaw)]);
    const ctr = opts.center || [0, 6, 0];
    const proj = (p) => { const q = view([p[0] - ctr[0], p[1] - ctr[1], p[2] - ctr[2]]); return [cx + q[0] * scale, cy + q[1] * scale, q[2]]; };

    // тень под ногами
    if (opts.shadow !== false) {
      const sy = proj([0, 24, 0])[1];
      const grd = g.createRadialGradient(cx, sy + 2, 2, cx, sy + 2, scale * 9);
      grd.addColorStop(0, 'rgba(0,20,40,.45)'); grd.addColorStop(1, 'rgba(0,20,40,0)');
      g.fillStyle = grd; g.beginPath(); g.ellipse(cx, sy + 2, scale * 9, scale * 2.6, 0, 0, Math.PI * 2); g.fill();
    }

    const faces = build(opts, t);
    const drawn = [];
    for (const f of faces) {
      const n = view(f.wn);
      if (n[2] > 1e-4 && !f.nocull) continue; // задние грани (камера смотрит в +z)
      const P = f.p.map(proj);
      const depth = (P[1][2] + P[2][2]) / 2;
      drawn.push({ f, P, depth, n });
    }
    drawn.sort((a, b) => b.depth - a.depth || (a.f.overlay ? 1 : 0) - (b.f.overlay ? 1 : 0));
    const light = (n) => 0.62 + 0.38 * Math.max(0, -n[2] * 0.8 - n[1] * 0.5);
    for (const { f, P, n } of drawn) {
      const [A, B, C] = P;
      const a = B[0] - A[0], b = B[1] - A[1], c = C[0] - A[0], d = C[1] - A[1];
      g.setTransform(a, b, c, d, A[0], A[1]);
      const k = SHADE(f.n) * 0.5 + light(n) * 0.5;
      if (f.tex) {
        if (f.full) {
          g.imageSmoothingEnabled = false;
          g.drawImage(f.tex, 0, 0, f.tex.width, f.tex.height, 0, 0, 1, 1);
          continue;
        }
        const [u, v, w, h] = f.uv;
        if (w <= 0 || h <= 0) continue;
        const sx = f.tex.width / 64;
        g.drawImage(f.tex, u * sx, v * sx, w * sx, h * sx, -0.004, -0.004, 1.008, 1.008);
        if (!f.overlay && k < 0.999) { g.fillStyle = `rgba(0,10,25,${(1 - k) * 0.85})`; g.fillRect(-0.004, -0.004, 1.008, 1.008); }
      } else {
        const [r, gg, bb, al] = hex(f.color);
        const kk = f.glow ? 1 : k;
        const c = (m) => `rgba(${Math.min(255, Math.round(r * kk * m))},${Math.min(255, Math.round(gg * kk * m))},${Math.min(255, Math.round(bb * kk * m))},${al})`;
        // вертикальный градиент: светлее сверху, темнее снизу (как в клиенте)
        if (f.n[1] === 0) { const lg = g.createLinearGradient(0, 0, 0, 1); lg.addColorStop(0, c(1.06)); lg.addColorStop(1, c(f.glow ? 0.95 : 0.8)); g.fillStyle = lg; }
        else g.fillStyle = c(f.n[1] < 0 ? 1.06 : 0.8);
        g.fillRect(-0.01, -0.01, 1.02, 1.02);
        // пиксельная фактура с фаской (светлый верх-лево, тёмный низ-право)
        if (BEVEL && al > 0.5 && !f.flat) { g.globalCompositeOperation = 'multiply'; g.drawImage(BEVEL, 0, 0, 16, 16, 0, 0, 1, 1); g.globalCompositeOperation = 'source-over'; }
      }
    }
    g.setTransform(1, 0, 0, 1, 0, 0);
    return { proj, scale };
  }

  // ---------- ауры (2D-частицы вокруг игрока) ----------
  const AURA = {
    portal: ['#b25cff', '#e070ff'], endrod: ['#fff', '#e8f6ff'], hearts: ['❤'], notes: ['♪', '♫'], bubbles: ['○'], snow: ['❄'], flame: ['#ff9a2e', '#ffd34d'],
    totem: ['#ffd23f', '#7cfc6b'], cherry: ['#ffb7d5', '#ff8fbf'], soul: ['#5fd7ff', '#3ec9ff'], glow: ['#5ff2c8', '#9fffe9'], dolphin: ['#3e9bff', '#9fd8ff'],
    drip: ['#3ec9ff'], happy: ['#7cfc6b', '#c8ff9a'], crit: ['#fff2b0', '#ffd23f'], enchant: ['ᚠ', 'ᚢ', 'ᚦ', 'ᚨ'], cloud: ['#ffffff', '#e8eef5'], spore: ['#c86bff', '#ff9ad5'],
    ash: ['#dfe3e8', '#bfc5cc'], rain: ['#7fc8ff'],
  };
  function auraStep(state, g, aura, dt, W, H) {
    const pal = AURA[aura];
    if (!pal) return;
    state.p = state.p || [];
    if (state.p.length < 40 && Math.random() < dt * 22) {
      const fall = ['snow', 'drip', 'rain', 'cherry', 'ash'].includes(aura);
      state.p.push({ x: W / 2 + (Math.random() - 0.5) * W * 0.55, y: fall ? H * 0.12 : H * (0.35 + Math.random() * 0.5), vy: fall ? 18 + Math.random() * 20 : -(14 + Math.random() * 22),
        vx: (Math.random() - 0.5) * 10, life: 1.6 + Math.random(), max: 2.4, c: pal[(Math.random() * pal.length) | 0], s: 2 + Math.random() * 3 });
    }
    for (const p of state.p) {
      p.life -= dt; p.x += p.vx * dt + Math.sin(p.life * 4) * 0.3; p.y += p.vy * dt;
      const a = Math.max(0, Math.min(1, p.life / 0.5, (p.max - p.life) * 3));
      g.globalAlpha = a;
      if (p.c.length <= 2 && !p.c.startsWith('#')) { g.fillStyle = aura === 'hearts' ? '#ff4d7a' : aura === 'enchant' ? '#c9b8ff' : '#bff6ff'; g.font = `${8 + p.s * 2}px sans-serif`; g.fillText(p.c, p.x, p.y); }
      else { g.fillStyle = p.c; g.shadowColor = p.c; g.shadowBlur = 6; g.fillRect(p.x, p.y, p.s, p.s); g.shadowBlur = 0; }
    }
    g.globalAlpha = 1;
    state.p = state.p.filter((p) => p.life > 0);
  }

  // ---------- интерактивный просмотрщик ----------
  class Viewer {
    constructor(canvas) {
      this.canvas = canvas; this.opts = { yaw: 0.6, pitch: -0.1 }; this.t = 0; this.last = performance.now();
      this.spin = true; this.vel = 0; this.aura = {}; this.capeFrames = null; this.frame = 0;
      let drag = null;
      canvas.addEventListener('pointerdown', (e) => { drag = { x: e.clientX, y: e.clientY }; this.spin = false; canvas.setPointerCapture(e.pointerId); canvas.classList.add('grab'); });
      canvas.addEventListener('pointermove', (e) => {
        if (!drag) return;
        const dx = e.clientX - drag.x, dy = e.clientY - drag.y; drag = { x: e.clientX, y: e.clientY };
        this.opts.yaw += dx * 0.012; this.vel = dx * 0.012; this.opts.pitch = Math.max(-0.6, Math.min(0.4, this.opts.pitch - dy * 0.006));
      });
      const up = () => { if (drag) { drag = null; canvas.classList.remove('grab'); clearTimeout(this.resume); this.resume = setTimeout(() => (this.spin = true), 2500); } };
      canvas.addEventListener('pointerup', up); canvas.addEventListener('pointercancel', up);
      canvas.addEventListener('dblclick', () => { this.opts.flying = !this.opts.flying; });
      const loop = (now) => {
        if (!this.canvas.isConnected) { this.stopped = true; return; }
        const dt = Math.min(0.05, (now - this.last) / 1000); this.last = now;
        if (this.canvas.offsetParent !== null) {
          this.t += dt;
          if (this.spin) this.opts.yaw += dt * 0.55; else { this.opts.yaw += this.vel; this.vel *= 0.9; }
          if (this.capeFrames && (this.frame += dt * 8) >= 0) this.opts.capeImg = this.capeFrames[Math.floor(this.frame) % this.capeFrames.length];
          const info = render(this.canvas, this.opts, this.t);
          const g = this.canvas.getContext('2d');
          if (this.opts.loadout?.aura && this.opts.loadout.aura !== 'none' && !document.body.classList.contains('reduce-motion')) auraStep(this.aura, g, this.opts.loadout.aura, dt, this.canvas.width, this.canvas.height);
          if (this.opts.name) this.nameTag(g, info);
        }
        requestAnimationFrame(loop);
      };
      requestAnimationFrame(loop);
    }
    async set(o) {
      Object.assign(this.opts, o);
      const cape = o.loadout?.cape;
      this.capeFrames = null; this.opts.capeImg = null;
      if (cape && cape !== 'none') {
        if (cape === 'portal') this.capeFrames = await Promise.all([...Array(8)].map((_, i) => Viewer.img(`assets/capes/portal_${i}.png`))).catch(() => null);
        else this.opts.capeImg = await Viewer.img(`assets/capes/${cape}.png`).catch(() => null);
      }
      const icon = o.loadout?.icon;
      this.iconImg = icon && icon !== 'none' ? await Viewer.img(`assets/icons/${icon}.png`).catch(() => null) : null;
    }
    nameTag(g, info) {
      const head = info.proj([0, -8, 0]);
      const y = head[1] - info.scale * 6;
      g.font = '600 13px Nunito, sans-serif';
      const name = this.opts.name, tw = g.measureText(name).width + (this.iconImg ? 18 : 0);
      const x = this.canvas.width / 2 - tw / 2;
      g.fillStyle = 'rgba(0,0,0,.38)'; g.fillRect(x - 6, y - 13, tw + 12, 19);
      if (this.iconImg) g.drawImage(this.iconImg, x, y - 10, 13, 13);
      const owner = this.opts.owner, pulse = 0.5 + 0.5 * Math.sin(this.t * 2.4);
      g.shadowColor = owner ? '#2ecc71' : '#1ca7ec'; g.shadowBlur = 6 + pulse * 6;
      g.fillStyle = owner ? `rgb(${46 + pulse * 94},${204 + pulse * 51},${113 + pulse * 63})` : `rgb(${28 + pulse * 99},${167 + pulse * 52},${236 + pulse * 19})`;
      g.fillText(name, x + (this.iconImg ? 18 : 0), y + 1);
      g.shadowBlur = 0;
    }
    static img(src) { return new Promise((ok, bad) => { const i = new Image(); i.onload = () => ok(i); i.onerror = bad; i.src = src; }); }
  }

  /** Статичная миниатюра одной вещи (для карточек гардероба). */
  function thumb(canvas, slot, id) {
    const it = find(slot, id);
    if (!it || !it.g) return false;
    const pending = it.g.some((g) => g.sp && !spriteImg(g.sp.s));
    if (pending && (canvas._tries = (canvas._tries || 0) + 1) < 40) setTimeout(() => thumb(canvas, slot, id), 120);
    let mn = [1e9, 1e9, 1e9], mx = [-1e9, -1e9, -1e9];
    for (const g of it.g) {
      const bs = g.sp ? [[Math.min(0, g.sp.w), g.sp.y1, 0, Math.max(0, g.sp.w), g.sp.y2, .3]] : g.b;
      for (const b of bs) for (let i = 0; i < 3; i++) { mn[i] = Math.min(mn[i], b[i] + g.o[i]); mx[i] = Math.max(mx[i], b[i + 3] + g.o[i]); }
    }
    const size = Math.max(mx[0] - mn[0], mx[1] - mn[1], mx[2] - mn[2], 6);
    const center = [(mn[0] + mx[0]) / 2, (mn[1] + mx[1]) / 2, (mn[2] + mx[2]) / 2];
    const yaw = slot === 'back' ? Math.PI + 0.6 : slot === 'wings' ? Math.PI + 0.12 : 0.55;
    const loadout = { [slot]: id };
    render(canvas, { yaw, pitch: -0.25, loadout, scale: canvas.width / (size * (slot === 'wings' ? 1.05 : 1.45)), center, shadow: false }, 0.6);
    return true;
  }

  window.AquaViewer = { Viewer, render, thumb };
})();
