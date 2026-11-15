/* Aqua Launcher — живой подводный мир v2:
   поверхность с солнцем, лучи света, каустика, стаи рыб, черепахи, дельфины, аксолотли,
   светящиеся спруты, древний страж вдали, коралловый риф, ламинарии, пузырьковые колонны и «морской снег». */
(() => {
  const canvas = document.getElementById('ocean');
  const ctx = canvas.getContext('2d');
  let W = 0, H = 0, dpr = 1, t = 0, last = performance.now();
  let enabled = true, dirty = true, quality = 'high';
  let mx = -9999, my = -9999, mouseNX = 0.5, mouseNY = 0.5;
  const rnd = (a, b) => a + Math.random() * (b - a);
  const lerp = (a, b, k) => a + (b - a) * k;

  // ======================== пиксельные спрайты (смотрят ВЛЕВО) ========================
  const SPRITES = {
    tropical: {
      rows: ['......DD.....', '....BBBBB...D', '..BBWBBBBB.DD', '.BEBWBBDBBBDD', 'BBBBWBBDBBBDD', '.BBBWBBBBB.DD', '...BBBBBB...D', '.....DD......'],
      palettes: [
        { B: '#ff8a3d', D: '#e8f3ff', W: '#ffffff', E: '#14202a' },
        { B: '#41d1ff', D: '#ffe066', W: '#ffffff', E: '#14202a' },
        { B: '#ff5fa2', D: '#7c5cff', W: '#ffe6f2', E: '#14202a' },
        { B: '#ffd23f', D: '#2e86de', W: '#ffffff', E: '#14202a' },
        { B: '#7cfc6b', D: '#1e7b4a', W: '#e9ffe0', E: '#14202a' },
      ],
    },
    cod: {
      rows: ['....BBBBB.....', '..BBBBBBBBB..T', '.EBBBDBBDBBBTT', 'BBBLLLLLLBBBTT', '.BBLLLLLLBB..T', '...BBBBBB.....'],
      palettes: [
        { B: '#b39a72', D: '#8c7556', L: '#e2d3b5', E: '#1a1a1a', T: '#9c8461' },
        { B: '#d0574c', D: '#9e3a33', L: '#f0a07a', E: '#1a1a1a', T: '#b4473f' },
      ],
    },
    minnow: { rows: ['.BBBB.T', 'EBLLBBT', '.BBBB.T'], palettes: [{ B: '#9fc6d6', L: '#e8f6ff', E: '#10202a', T: '#7fa6b6' }, { B: '#c9b98f', L: '#f3ead2', E: '#10202a', T: '#a8986f' }] },
    puffer: {
      rows: ['..S.S.S...', '.YYYYYYY..', 'SYEYYYYYYS', '.YYYYYYYYT', 'SYYYYYYYYT', '.YLLLLLYYT', 'SYLLLLLYYS', '.YYYYYYY..', '..S.S.S...'],
      palettes: [{ Y: '#f2c94c', S: '#d4a017', E: '#1a1a1a', L: '#fff1b8', T: '#e0b030' }],
    },
    turtle: {
      rows: ['......SSSSSS......', '....SSPSSPSSSS....', '...SSPPSSSPPSSS...', 'HHHSSSSSSSSSSSSS..', 'HEHHSSPSSSSPSSSS.F', 'HHH..SSSSSSSSSS..F', '.....FF.....FF....', '....FF.......FF...'],
      palettes: [{ H: '#6fbf5b', E: '#16301a', S: '#2f7d3a', P: '#57a44b', F: '#5aa84a' }],
    },
    squid: {
      rows: ['...GGGG...', '..GGGGGG..', '.GGgGGgGG.', '.GGGGGGGG.', '.GEGGGGEG.', '.GGGGGGGG.', '..G.G.G.G.', '.G.G.G.G..', '..G...G...'],
      palettes: [{ G: '#1fb5a8', g: '#9ffff0', E: '#0c2b30' }],
    },
    dolphin: {
      rows: ['......GG............', '.....GGGG...........', '..GGGGGGGGGGGGG...TT', '.EGGGGGGGGGGGGGGGTT.', 'GGGLLLLLLLLLGGGGGTT.', '.GLLLLLLLLLLLGG...TT', '....LLL.............'],
      palettes: [{ G: '#6d8fa6', L: '#c9dbe6', E: '#0e1a22', T: '#5a7a90' }],
    },
    axolotl: {
      rows: ['P.P.........', '.PPPPPPPPP..', 'PEPPPPPPPPPT', '.PPPPPPPPPTT', '..F.F..F.F..'],
      palettes: [{ P: '#f4a6c8', E: '#2a1020', T: '#e489b0', F: '#d9799f' }, { P: '#8fd0ff', E: '#10202a', T: '#6fb8f0', F: '#5aa0e0' }, { P: '#ffd76b', E: '#2a2010', T: '#f0c050', F: '#e0b040' }],
    },
    elder: {
      rows: ['..S...S....S...S..', '.SBBBBBBBBBBBBBBS.', 'SBBBBBBBBBBBBBBBBS', 'BBEEBBBBBBBBBBBBBB', 'BBEEBBBBBBBBBBBBBBTT', 'SBBBBBBBBBBBBBBBBSTT', '.SBBBBBBBBBBBBBBS...', '..S...S....S...S...'],
      palettes: [{ B: '#123b52', S: '#0d2c3e', E: '#2d5f75', T: '#0f3346' }],
    },
  };
  const cache = new Map();
  function sprite(type, pal, scale, flip) {
    const key = `${type}|${pal}|${scale}|${flip}`;
    if (cache.has(key)) return cache.get(key);
    const def = SPRITES[type], rows = def.rows, colors = def.palettes[pal % def.palettes.length];
    const w = Math.max(...rows.map((r) => r.length));
    const c = document.createElement('canvas');
    c.width = w * scale; c.height = rows.length * scale;
    const g = c.getContext('2d');
    rows.forEach((r, y) => [...r].forEach((ch, x) => {
      if (ch === '.' || !colors[ch]) return;
      g.fillStyle = colors[ch];
      g.fillRect((flip ? w - 1 - x : x) * scale, y * scale, scale, scale);
    }));
    cache.set(key, c);
    return c;
  }

  // ======================== сцена ========================
  let creatures = [], schools = [], bubbles = [], snow = [], kelp = [], grass = [], corals = [], pickles = [], rays = [], columns = [];
  let farLayer = null, seabed = null, elder = null, groundY = 0;
  const caustic = document.createElement('canvas');
  const cctx = caustic.getContext('2d');
  let cimg = null;

  function spawnCreature(initial, forceType) {
    const r = Math.random();
    const type = forceType || (r < 0.42 ? 'tropical' : r < 0.6 ? 'cod' : r < 0.7 ? 'puffer' : r < 0.8 ? 'turtle' : r < 0.88 ? 'squid' : r < 0.95 ? 'axolotl' : 'dolphin');
    const depth = rnd(0.45, 1);
    const base = { turtle: 4, dolphin: 4, squid: 3, axolotl: 3, puffer: 3 }[type] || 3;
    const scale = Math.max(2, Math.round(base * depth));
    const dir = Math.random() < 0.5 ? -1 : 1;
    const pal = Math.floor(Math.random() * SPRITES[type].palettes.length);
    const spr = sprite(type, pal, scale, dir > 0);
    const speedBase = { turtle: [14, 22], puffer: [10, 18], squid: [8, 14], axolotl: [18, 28], dolphin: [120, 170] }[type] || [30, 64];
    const yMin = type === 'axolotl' ? groundY - 70 : H * 0.14, yMax = type === 'axolotl' ? groundY - 24 : H * 0.74;
    return {
      type, pal, scale, dir, depth, spr, speed: rnd(...speedBase) * depth,
      x: initial ? rnd(0, W) : (dir > 0 ? -spr.width - 30 : W + 30),
      y: rnd(yMin, yMax), phase: rnd(0, Math.PI * 2), amp: type === 'dolphin' ? rnd(30, 60) : rnd(4, 14),
      fleeX: 0, fleeY: 0,
    };
  }

  function makeSchool(initial) {
    const dir = Math.random() < 0.5 ? -1 : 1;
    const depth = rnd(0.3, 0.7);
    const pal = Math.floor(Math.random() * 2);
    const scale = depth > 0.5 ? 3 : 2;
    const cx = initial ? rnd(0, W) : (dir > 0 ? -120 : W + 120), cy = rnd(H * 0.18, H * 0.6);
    const fish = Array.from({ length: Math.round(rnd(10, 18)) }, () => ({ x: cx + rnd(-60, 60), y: cy + rnd(-30, 30), vx: dir * 40, vy: 0 }));
    return { dir, depth, pal, scale, cx, cy, speed: rnd(35, 55), fish, phase: rnd(0, 6), spr: sprite('minnow', pal, scale, dir > 0) };
  }

  function newBubble(initial, x, y, big) {
    return { x: x ?? rnd(0, W), y: y ?? (initial ? rnd(0, H) : H + rnd(0, 40)), r: big ? rnd(5, 11) : rnd(1.5, 5.5), s: rnd(25, 75), ph: rnd(0, 6), w: rnd(0.6, 2) };
  }

  // ---------- дальний план: монумент, арки скал, затонувший корабль ----------
  function buildFar() {
    farLayer = document.createElement('canvas');
    farLayer.width = W + 200; farLayer.height = H;
    const g = farLayer.getContext('2d');
    const base = H - 60;
    g.fillStyle = 'rgba(6, 40, 66, 0.9)';
    // скалы
    for (let x = -50; x < W + 250; x += rnd(120, 220)) {
      const h = rnd(80, 220), w = rnd(90, 200);
      for (let yy = 0; yy < h; yy += 10) {
        const k = 1 - yy / h, ww = w * (0.35 + 0.65 * k) + rnd(-8, 8);
        g.fillRect(x + (w - ww) / 2, base - yy, ww, 11);
      }
    }
    // подводная крепость (монумент)
    const mxp = W * 0.62, my0 = base;
    g.fillStyle = 'rgba(8, 52, 78, 0.95)';
    g.fillRect(mxp, my0 - 120, 260, 120);
    g.fillRect(mxp + 30, my0 - 170, 200, 50);
    g.fillRect(mxp + 80, my0 - 210, 100, 40);
    g.fillStyle = 'rgba(70, 200, 210, 0.35)';
    for (let i = 0; i < 9; i++) g.fillRect(mxp + 18 + i * 26, my0 - 100, 10, 10);
    for (let i = 0; i < 5; i++) g.fillRect(mxp + 50 + i * 34, my0 - 155, 10, 10);
    g.fillRect(mxp + 122, my0 - 198, 16, 12);
    // затонувший корабль
    const sx = W * 0.12, sy = base - 10;
    g.fillStyle = 'rgba(10, 46, 70, 0.95)';
    g.beginPath(); g.moveTo(sx, sy - 40); g.lineTo(sx + 220, sy - 70); g.lineTo(sx + 200, sy); g.lineTo(sx + 20, sy); g.closePath(); g.fill();
    g.fillRect(sx + 100, sy - 170, 8, 110);
    g.fillRect(sx + 60, sy - 140, 90, 6);
  }

  // ---------- риф и дно (рисуем один раз) ----------
  const CORAL = [
    { c1: '#2f6fe0', c2: '#4f8ff7' }, { c1: '#e0509e', c2: '#ff7cc0' }, { c1: '#a03be0', c2: '#c46cff' },
    { c1: '#e0402f', c2: '#ff6b4f' }, { c1: '#e8c53a', c2: '#ffe066' },
  ];
  function buildSeabed() {
    seabed = document.createElement('canvas');
    seabed.width = W; seabed.height = H - groundY + 4;
    const g = seabed.getContext('2d');
    for (let row = 0; row * 8 < seabed.height; row++) {
      for (let x = 0; x < W; x += 8) {
        const n = Math.random();
        g.fillStyle = row === 0 ? (n > 0.5 ? '#ddd093' : '#d3c587') : n > 0.66 ? '#c2b276' : n > 0.33 ? '#b7a86c' : '#ab9c62';
        g.fillRect(x, row * 8, 8, 8);
        if (row > 0 && Math.random() < 0.03) { g.fillStyle = '#8e8f8c'; g.fillRect(x, row * 8, 8, 8); }
      }
    }
  }

  function build() {
    groundY = H - 46;
    creatures = Array.from({ length: Math.round(Math.min(22, W / 70)) }, () => spawnCreature(true));
    creatures.push(spawnCreature(true, 'turtle'));
    schools = Array.from({ length: 3 }, () => makeSchool(true));
    bubbles = Array.from({ length: 45 }, () => newBubble(true));
    snow = Array.from({ length: 90 }, () => ({ x: rnd(0, W), y: rnd(0, H), s: rnd(3, 10), r: rnd(0.6, 1.8), ph: rnd(0, 6) }));
    kelp = [];
    for (let x = 10; x < W; x += rnd(40, 110)) kelp.push({ x, h: Math.round(rnd(6, 18)), phase: rnd(0, 6), seg: Math.random() < 0.5 ? 10 : 8, dark: Math.random() < 0.4 });
    grass = Array.from({ length: Math.round(W / 22) }, () => ({ x: rnd(0, W), h: rnd(10, 26), ph: rnd(0, 6) }));
    corals = [];
    for (let x = rnd(20, 80); x < W; x += rnd(70, 160)) corals.push({ x, type: Math.floor(rnd(0, 3)), col: CORAL[Math.floor(rnd(0, CORAL.length))], h: Math.round(rnd(3, 6)), ph: rnd(0, 6) });
    pickles = Array.from({ length: Math.round(W / 150) }, () => ({ x: rnd(0, W), n: 1 + Math.floor(rnd(0, 4)), phase: rnd(0, 6) }));
    rays = Array.from({ length: 7 }, (_, i) => ({ x: (i + 0.5) * W / 7 + rnd(-60, 60), w: rnd(60, 170), phase: rnd(0, 6), a: rnd(0.05, 0.1) }));
    columns = Array.from({ length: Math.max(2, Math.round(W / 600)) }, () => ({ x: rnd(W * 0.1, W * 0.9), timer: 0 }));
    elder = { x: -400, y: H * 0.35, dir: 1, wait: rnd(4, 12), spr: sprite('elder', 0, 9, true) };
    caustic.width = Math.max(64, Math.round(W / 10));
    caustic.height = Math.max(40, Math.round(H / 10));
    cimg = cctx.createImageData(caustic.width, caustic.height);
    buildFar();
    buildSeabed();
  }

  function resize() {
    dpr = Math.min(window.devicePixelRatio || 1, 1.5);
    W = window.innerWidth; H = window.innerHeight;
    canvas.width = Math.round(W * dpr); canvas.height = Math.round(H * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.imageSmoothingEnabled = false;
    build();
    dirty = true;
  }

  // ======================== отрисовка ========================
  function drawWater() {
    const shift = Math.sin(t * 0.05) * 8;
    const g = ctx.createLinearGradient(0, 0, 0, H);
    g.addColorStop(0, `hsl(${198 + shift}, 85%, 34%)`);
    g.addColorStop(0.3, `hsl(${203 + shift}, 88%, 22%)`);
    g.addColorStop(0.7, '#03243f');
    g.addColorStop(1, '#021526');
    ctx.fillStyle = g;
    ctx.fillRect(0, 0, W, H);

    // солнце сквозь воду
    const sx = W * 0.7 + Math.sin(t * 0.07) * 40 + (mouseNX - 0.5) * 30;
    const sun = ctx.createRadialGradient(sx, -20, 0, sx, -20, Math.max(W, H) * 0.55);
    sun.addColorStop(0, 'rgba(220, 255, 255, 0.45)');
    sun.addColorStop(0.25, 'rgba(150, 235, 255, 0.15)');
    sun.addColorStop(1, 'rgba(150, 235, 255, 0)');
    ctx.fillStyle = sun;
    ctx.fillRect(0, 0, W, H);

    // поверхность: волны и блики
    ctx.lineWidth = 2;
    for (let k = 0; k < 4; k++) {
      ctx.strokeStyle = `rgba(210, 252, 255, ${0.16 - k * 0.03})`;
      ctx.beginPath();
      for (let x = 0; x <= W; x += 12) {
        const y = 14 + k * 14 + Math.sin(x * 0.012 + t * (0.9 + k * 0.3) + k) * 5 + Math.sin(x * 0.033 - t * 1.4) * 2;
        x === 0 ? ctx.moveTo(x, y) : ctx.lineTo(x, y);
      }
      ctx.stroke();
    }
    for (let i = 0; i < 26; i++) {
      const x = (i * 97.3 + t * 18 * (i % 2 ? 1 : -1)) % (W + 40);
      const a = 0.5 + 0.5 * Math.sin(t * 3 + i * 1.7);
      ctx.fillStyle = `rgba(255,255,255,${0.35 * a})`;
      ctx.fillRect(x, 10 + (i % 4) * 12 + Math.sin(t + i) * 3, 3, 2);
    }
  }

  function drawFar() {
    if (!farLayer) return;
    const off = (mouseNX - 0.5) * -20 - 100;
    ctx.globalAlpha = 0.85;
    ctx.drawImage(farLayer, off, (mouseNY - 0.5) * -8);
    ctx.globalAlpha = 1;
    // дымка
    const fog = ctx.createLinearGradient(0, H * 0.35, 0, H);
    fog.addColorStop(0, 'rgba(4, 40, 68, 0.5)');
    fog.addColorStop(1, 'rgba(4, 40, 68, 0)');
    ctx.fillStyle = fog;
    ctx.fillRect(0, H * 0.35, W, H * 0.65);
  }

  function drawElder(dt) {
    elder.wait -= dt;
    if (elder.wait > 0) return;
    elder.x += elder.dir * 22 * dt;
    const y = elder.y + Math.sin(t * 0.4) * 20;
    ctx.globalAlpha = 0.55;
    ctx.drawImage(elder.spr, Math.round(elder.x), Math.round(y));
    ctx.globalAlpha = 1;
    if ((elder.dir > 0 && elder.x > W + 50) || (elder.dir < 0 && elder.x < -elder.spr.width - 50)) {
      elder.dir *= -1;
      elder.spr = sprite('elder', 0, 9, elder.dir > 0);
      elder.y = rnd(H * 0.2, H * 0.45);
      elder.wait = rnd(25, 60);
    }
  }

  function drawRays() {
    const px = (mouseNX - 0.5) * 30;
    ctx.save();
    ctx.globalCompositeOperation = 'lighter';
    for (const r of rays) {
      const sway = Math.sin(t * 0.25 + r.phase) * 40;
      const a = r.a * (0.55 + 0.45 * Math.sin(t * 0.6 + r.phase) * Math.sin(t * 0.23 + r.phase * 2));
      const g = ctx.createLinearGradient(0, 0, 0, H * 0.9);
      g.addColorStop(0, `rgba(180,245,255,${Math.max(0, a)})`);
      g.addColorStop(1, 'rgba(180,245,255,0)');
      ctx.fillStyle = g;
      ctx.beginPath();
      ctx.moveTo(r.x + sway + px - r.w * 0.3, 0);
      ctx.lineTo(r.x + sway + px + r.w * 0.3, 0);
      ctx.lineTo(r.x + sway * 2.2 + px * 2 + r.w, H * 0.9);
      ctx.lineTo(r.x + sway * 2.2 + px * 2 - r.w * 0.4, H * 0.9);
      ctx.closePath();
      ctx.fill();
    }
    ctx.restore();
  }

  let causticFrame = 0;
  function drawCaustics() {
    const cw = caustic.width, ch = caustic.height, d = cimg.data;
    if ((causticFrame++ & 1) === 0) {
    const tt = t * 0.9;
    for (let y = 0; y < ch; y++) {
      const ny = y / ch;
      // сильнее у поверхности и у дна
      const mask = Math.max(0, 1 - ny * 3) * 0.6 + Math.max(0, (ny - 0.72) * 3.2);
      for (let x = 0; x < cw; x++) {
        const fx = x * 0.21, fy = y * 0.23;
        const v = Math.sin(fx + tt + Math.sin(fy * 1.3 + tt * 0.7) * 1.6) + Math.sin(fy - tt * 0.8 + Math.sin(fx * 1.1 - tt * 0.6) * 1.6);
        const b = Math.pow(Math.max(0, 1 - Math.abs(v) * 0.9), 5) * mask;
        const i = (y * cw + x) * 4;
        d[i] = 170; d[i + 1] = 245; d[i + 2] = 255; d[i + 3] = Math.min(255, b * 255);
      }
    }
    cctx.putImageData(cimg, 0, 0);
    }
    ctx.save();
    ctx.globalCompositeOperation = 'lighter';
    ctx.globalAlpha = 0.45;
    ctx.imageSmoothingEnabled = true;
    ctx.drawImage(caustic, 0, 0, W, H);
    ctx.restore();
    ctx.imageSmoothingEnabled = false;
  }

  function drawSchools(dt) {
    for (let s = 0; s < schools.length; s++) {
      const sc = schools[s];
      sc.phase += dt;
      sc.cx += sc.dir * sc.speed * dt;
      sc.cy += Math.sin(sc.phase * 0.7) * 12 * dt;
      let alive = false;
      for (const f of sc.fish) {
        // стая: тянемся к центру, держим дистанцию, убегаем от курсора
        let ax = (sc.cx - f.x) * 0.9 + sc.dir * 30, ay = (sc.cy - f.y) * 0.9 + Math.sin(sc.phase * 2 + f.x * 0.05) * 10;
        for (const o of sc.fish) {
          if (o === f) continue;
          const dx = f.x - o.x, dy = f.y - o.y, d2 = dx * dx + dy * dy;
          if (d2 < 220 && d2 > 0.01) { ax += dx / d2 * 900; ay += dy / d2 * 900; }
        }
        const mdx = f.x - mx, mdy = f.y - my, md2 = mdx * mdx + mdy * mdy;
        if (md2 < 14000) { const k = 9000 / (md2 + 200); ax += mdx * k; ay += mdy * k; }
        f.vx = lerp(f.vx, ax, Math.min(1, dt * 2.2));
        f.vy = lerp(f.vy, ay, Math.min(1, dt * 2.2));
        f.x += f.vx * dt; f.y += f.vy * dt;
        if (f.x > -40 && f.x < W + 40) alive = true;
        ctx.globalAlpha = 0.5 + sc.depth * 0.5;
        ctx.drawImage(f.vx >= 0 ? sprite('minnow', sc.pal, sc.scale, true) : sprite('minnow', sc.pal, sc.scale, false), Math.round(f.x), Math.round(f.y));
      }
      ctx.globalAlpha = 1;
      if (!alive && ((sc.dir > 0 && sc.cx > W + 200) || (sc.dir < 0 && sc.cx < -200))) schools[s] = makeSchool(false);
    }
  }

  function drawCreatures(dt) {
    for (let i = 0; i < creatures.length; i++) {
      const c = creatures[i];
      c.x += c.dir * c.speed * dt;
      c.phase += dt * (c.type === 'turtle' ? 1.2 : c.type === 'dolphin' ? 1.6 : 2.4);
      // испуг от курсора
      const cxm = c.x + c.spr.width / 2, cym = c.y + c.spr.height / 2;
      const dx = cxm - mx, dy = cym - my, d2 = dx * dx + dy * dy;
      if (d2 < 16000 && c.type !== 'turtle') { c.fleeY += (dy > 0 ? 1 : -1) * 160 * dt; c.fleeX += (dx > 0 ? 1 : -1) * 60 * dt; }
      c.fleeX *= 0.96; c.fleeY *= 0.96;
      const y = c.y + Math.sin(c.phase) * c.amp + (mouseNY - 0.5) * 30 * c.depth + c.fleeY;
      const x = c.x + (mouseNX - 0.5) * 40 * c.depth + c.fleeX;
      if ((c.dir > 0 && c.x > W + 60) || (c.dir < 0 && c.x < -c.spr.width - 60)) { creatures[i] = spawnCreature(false); continue; }
      ctx.globalAlpha = 0.45 + c.depth * 0.55;
      ctx.drawImage(c.spr, Math.round(x), Math.round(y));
      if (c.type === 'squid') {
        ctx.globalCompositeOperation = 'lighter';
        const pulse = 0.12 + 0.08 * Math.sin(t * 3 + c.phase);
        const g = ctx.createRadialGradient(x + c.spr.width / 2, y + c.spr.height / 2, 0, x + c.spr.width / 2, y + c.spr.height / 2, c.spr.width * 1.4);
        g.addColorStop(0, `rgba(80,255,220,${pulse})`);
        g.addColorStop(1, 'rgba(80,255,220,0)');
        ctx.fillStyle = g;
        ctx.fillRect(x - c.spr.width, y - c.spr.height * 1.5, c.spr.width * 3, c.spr.height * 4);
        ctx.globalCompositeOperation = 'source-over';
      }
      if (c.type === 'dolphin' && Math.random() < dt * 6) bubbles.push(newBubble(false, x + (c.dir > 0 ? 0 : c.spr.width), y + c.spr.height / 2));
    }
    ctx.globalAlpha = 1;
  }

  function drawSeabed(dt) {
    const px = (mouseNX - 0.5) * -12;
    // ламинарии
    for (const k of kelp) {
      for (let i = 0; i < k.h; i++) {
        const sway = Math.sin(t * 1.1 + k.phase + i * 0.35) * (i * 1.3) + Math.sin(t * 0.37 + k.phase) * i * 0.6;
        const x = Math.round(k.x + sway + px), y = groundY - (i + 1) * k.seg;
        ctx.fillStyle = k.dark ? (i % 2 ? '#2e6b2a' : '#3a7f33') : (i % 2 ? '#4f9a3b' : '#5fae46');
        ctx.fillRect(x, y, k.seg, k.seg);
        if (i % 3 === 1) { ctx.fillStyle = '#7cc95a'; ctx.fillRect(x + (i % 2 ? -4 : k.seg), y + 2, 4, 3); }
      }
    }
    // кораллы
    for (const c of corals) {
      const x = Math.round(c.x + px * 0.8);
      const sway = Math.sin(t * 1.4 + c.ph) * 2;
      if (c.type === 0) { // кустовой коралл
        for (let i = 0; i < c.h; i++) {
          ctx.fillStyle = i % 2 ? c.col.c1 : c.col.c2;
          ctx.fillRect(x + Math.round(sway * i / c.h), groundY - (i + 1) * 6, 6, 6);
          if (i > 0 && i % 2 === 0) { ctx.fillRect(x - 6 + sway, groundY - (i + 1) * 6, 6, 4); ctx.fillRect(x + 6 + sway, groundY - i * 6, 6, 4); }
        }
      } else if (c.type === 1) { // коралловый блок
        for (let yy = 0; yy < 3; yy++) for (let xx = 0; xx < 4; xx++) {
          ctx.fillStyle = (xx + yy) % 2 ? c.col.c1 : c.col.c2;
          ctx.fillRect(x + xx * 6, groundY - (yy + 1) * 6, 6, 6);
        }
      } else { // веер
        for (let i = 0; i < 5; i++) {
          ctx.fillStyle = i % 2 ? c.col.c1 : c.col.c2;
          const w = 4 + i * 4;
          ctx.fillRect(x - w / 2 + sway * (i / 4), groundY - 6 - i * 5, w, 5);
        }
      }
    }
    // водоросли-трава
    ctx.fillStyle = '#4c9a3c';
    for (const g of grass) {
      const sway = Math.sin(t * 1.6 + g.ph) * 3;
      for (let i = 0; i < g.h; i += 3) ctx.fillRect(Math.round(g.x + px + sway * (i / g.h)), groundY - i - 3, 2, 3);
    }
    // дно
    if (seabed) ctx.drawImage(seabed, Math.round(px * 0.2) - 4, groundY);
    // магма + пузырьковые колонны
    for (const col of columns) {
      const x = Math.round(col.x + px * 0.5);
      const glow = 0.6 + 0.4 * Math.sin(t * 4 + col.x);
      ctx.fillStyle = `rgb(${200 + 55 * glow},${70 + 40 * glow},20)`;
      ctx.fillRect(x - 8, groundY, 16, 8);
      ctx.fillStyle = '#5a1f0a';
      ctx.fillRect(x - 4, groundY + 2, 4, 2);
      col.timer -= dt;
      if (col.timer <= 0) { col.timer = 0.05; bubbles.push({ ...newBubble(false, x + rnd(-6, 6), groundY - 2), s: rnd(90, 140), w: 3, col: x }); }
    }
    // морские огурцы
    for (const p of pickles) {
      const glow = 0.5 + 0.5 * Math.sin(t * 2 + p.phase);
      const x = Math.round(p.x + px * 0.5), y = groundY - 10;
      const rg = ctx.createRadialGradient(x + 6, y + 4, 0, x + 6, y + 4, 36);
      rg.addColorStop(0, `rgba(190,255,120,${0.22 + glow * 0.22})`);
      rg.addColorStop(1, 'rgba(190,255,120,0)');
      ctx.fillStyle = rg;
      ctx.fillRect(x - 32, y - 32, 76, 74);
      for (let i = 0; i < p.n; i++) {
        ctx.fillStyle = '#5f8f2f';
        ctx.fillRect(x + i * 5, y, 4, 10);
        ctx.fillStyle = '#e8ff9a';
        ctx.fillRect(x + i * 5 + 1, y - 2, 2, 2);
      }
    }
  }

  function drawBubbles(dt) {
    for (let i = bubbles.length - 1; i >= 0; i--) {
      const b = bubbles[i];
      b.y -= b.s * dt;
      b.ph += dt * b.w;
      const x = b.col !== undefined ? b.col + Math.sin(b.ph * 3) * (4 + (groundY - b.y) * 0.02) : b.x + Math.sin(b.ph * 2) * 6;
      if (b.y < -12) { if (b.col !== undefined || bubbles.length > 120) bubbles.splice(i, 1); else bubbles[i] = newBubble(false); continue; }
      ctx.strokeStyle = 'rgba(215,250,255,0.6)';
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.arc(x, b.y, b.r, 0, Math.PI * 2);
      ctx.stroke();
      ctx.fillStyle = 'rgba(210,250,255,0.10)';
      ctx.fill();
      ctx.fillStyle = 'rgba(255,255,255,0.75)';
      ctx.fillRect(x - b.r * 0.45, b.y - b.r * 0.45, Math.max(1, b.r * 0.35), Math.max(1, b.r * 0.35));
    }
    if (bubbles.length > 400) bubbles.splice(0, bubbles.length - 400);
  }

  function drawSnow(dt) {
    ctx.fillStyle = 'rgba(220, 245, 255, 0.35)';
    for (const p of snow) {
      p.y += p.s * dt * 0.6;
      p.x += Math.sin(t * 0.5 + p.ph) * 6 * dt;
      if (p.y > H) { p.y = -4; p.x = rnd(0, W); }
      ctx.fillRect(p.x, p.y, p.r, p.r);
    }
  }

  function drawMouseGlow() {
    if (mx < 0) return;
    ctx.save();
    ctx.globalCompositeOperation = 'lighter';
    const g = ctx.createRadialGradient(mx, my, 0, mx, my, 150);
    g.addColorStop(0, 'rgba(120, 230, 255, 0.10)');
    g.addColorStop(1, 'rgba(120, 230, 255, 0)');
    ctx.fillStyle = g;
    ctx.fillRect(mx - 150, my - 150, 300, 300);
    ctx.restore();
  }

  function drawVignette() {
    const g = ctx.createRadialGradient(W / 2, H * 0.45, Math.min(W, H) * 0.35, W / 2, H * 0.5, Math.max(W, H) * 0.8);
    g.addColorStop(0, 'rgba(1, 12, 24, 0)');
    g.addColorStop(1, 'rgba(1, 12, 24, 0.55)');
    ctx.fillStyle = g;
    ctx.fillRect(0, 0, W, H);
  }

  function frame(now) {
    const dt = Math.min(0.05, (now - last) / 1000);
    last = now;
    if (enabled || dirty) {
      const step = enabled ? dt : 0;
      dirty = false;
      t += step;
      drawWater();
      drawFar();
      drawElder(step);
      if (quality !== 'low') drawRays();
      if (quality !== 'low') drawSchools(step);
      drawCreatures(step);
      drawSeabed(step);
      if (quality === 'high') drawCaustics();
      drawBubbles(step);
      if (quality !== 'low') drawSnow(step);
      drawMouseGlow();
      drawVignette();
    }
    requestAnimationFrame(frame);
  }

  window.addEventListener('resize', resize);
  window.addEventListener('mousemove', (e) => { mx = e.clientX; my = e.clientY; mouseNX = mx / W; mouseNY = my / H; });
  window.addEventListener('mouseleave', () => { mx = my = -9999; });
  window.addEventListener('mousedown', (e) => {
    for (let i = 0; i < 10; i++) bubbles.push(newBubble(false, e.clientX + rnd(-14, 14), e.clientY + rnd(-8, 8)));
  });

  window.Ocean = {
    setEnabled(v) { enabled = !!v; dirty = true; },
    setQuality(q) {
      if (q === quality && enabled === (q !== 'off')) return;
      quality = q === 'off' ? 'low' : q;
      enabled = q !== 'off';
      if (q === 'low' && creatures.length > 8) creatures.length = 8;
      dirty = true;
    },
    burst(x, y, n = 18) { for (let i = 0; i < n; i++) bubbles.push(newBubble(false, x + rnd(-30, 30), y + rnd(-10, 10), Math.random() < 0.3)); },
    sprite,
  };

  resize();
  requestAnimationFrame(frame);
})();
