/* Пиксель-арт Aqua Launcher: рисуется в canvas, анимируется. Свои рисунки, без чужих картинок. */
(function () {
  const PAL = {
    k: '#0b1626', K: '#1d2a3c', w: '#ffffff', W: '#d6f6ff', c: '#7fe9ff', C: '#2bb6d9', d: '#147a99', D: '#0a4f66',
    y: '#ffe066', Y: '#ffc21a', o: '#e38f00', O: '#9c5c00', b: '#8a5a2b', B: '#5a3a1a', g: '#9aa6b2', G: '#5d6875',
    p: '#c48bff', P: '#7d3bd9', r: '#ff4d6d', s: '#1f5fff', S: '#0f2f99', e: '#62f0ff', n: '#2f7d3a', N: '#6fbf5b',
  };
  function sprite(rows) { return rows.map((r) => r.split('')); }
  function draw(ctx, spr, x, y, px, pal = PAL) {
    spr.forEach((row, j) => row.forEach((ch, i) => { if (ch !== '.' && pal[ch]) { ctx.fillStyle = pal[ch]; ctx.fillRect(x + i * px, y + j * px, px, px); } }));
  }
  const SWORD = sprite([
    '..............kk',
    '.............kWk',
    '............kWck',
    '...........kWck.',
    '..........kWck..',
    '.........kWck...',
    '........kWck....',
    '..kk...kWck.....',
    '..kYk.kWck......',
    '...kYkWck.......',
    '....kYck........',
    '...kbkYk........',
    '..kbk.kYk.......',
    '.kbk...kk.......',
    'kOk.............',
    'kk..............',
  ]);
  const PICKAXE = sprite([
    '....kkkkkkkk....',
    '..kkcWWWWWWckk..',
    '.kcWccccccccWck.',
    'kcck...kk...kcck',
    'kck....kbk...kck',
    '.k.....kbk....k.',
    '.......kbk......',
    '.......kbk......',
    '.......kBk......',
    '.......kbk......',
    '.......kbk......',
    '.......kBk......',
    '.......kbk......',
    '.......kbk......',
    '.......kBk......',
    '........k.......',
  ]);
  const TROPHY = sprite([
    '..kkkkkkkkkkkk..',
    'kkkyyyyyyyyYYkkk',
    'kYkyWyyyyyyYYkYk',
    'kYkyWyyyyyyYOkYk',
    'kYkyyyyyyyyYOkYk',
    '.kkyyyyyyyyYOkk.',
    '...kyyyyyyYOk...',
    '....kyyyyYOk....',
    '.....kkyYkk.....',
    '......kYOk......',
    '......kYOk......',
    '.....kyyYOk.....',
    '...kkkkkkkkkk...',
    '...kGGGGGGGGk...',
    '...kGgggggggk...',
    '...kkkkkkkkkk...',
  ]);
  const PUZZLE = sprite([
    '......kkk.......',
    '.....kcWck......',
    '.....kcck.......',
    '.kkkkkcckkkkk...',
    '.kWcccccccccck..',
    '.kccccccccccdk..',
    'kkcccccccccccdkk',
    'kWcccccccccccdck',
    'kccccccccccccddk',
    'kkcccccccccccdkk',
    '.kccccccccccdk..',
    '.kcccccccccddk..',
    '.kddddddddddk...',
    '.kkkkkkkkkkkk...',
  ]);
  const CROWN = sprite([
    'y.....y.....y',
    'yy...yYy...yy',
    'yYy.yYYYy.yYy',
    'yYYyYYrYYyYYy',
    'yYYYYYYYYYYYy',
    'yYsYYYrYYYsYy',
    'yYYYYYYYYYYYy',
    'OOOOOOOOOOOOO',
  ]);
  const SQUID = sprite([
    '...kkkkkkkk...',
    '..kSSSSSSSSk..',
    '.kSssssssssSk.',
    '.kSssssssssSk.',
    '.kSseesseesSk.',
    '.kSseesseesSk.',
    '.kSssssssssSk.',
    '.kSssKKKKssSk.',
    '.kSssssssssSk.',
    '..kSSSSSSSSk..',
  ]);
  const api = { PAL, draw, SWORD, PICKAXE, TROPHY, PUZZLE, CROWN, SQUID };

  /** Анимированный рисунок на карточке. kind: pvp | mods | leader */
  api.card = function (canvas, kind) {
    const g = canvas.getContext('2d'); let t = 0, last = performance.now();
    const loop = (now) => {
      if (!canvas.isConnected) return;
      const dt = Math.min(0.05, (now - last) / 1000); last = now; t += dt;
      const hover = canvas.closest('.feat')?.matches(':hover');
      g.clearRect(0, 0, canvas.width, canvas.height); g.imageSmoothingEnabled = false;
      const W = canvas.width, H = canvas.height, px = Math.floor(H / 20);
      if (kind === 'pvp') {
        const swing = Math.sin(t * (hover ? 6 : 2)) * (hover ? 10 : 3);
        g.save(); g.translate(W * 0.55, H * 0.5); g.rotate((-8 + swing) * Math.PI / 180); draw(g, SWORD, -8 * px, -8 * px, px); g.restore();
        g.save(); g.translate(W * 0.75, H * 0.5); g.scale(-1, 1); g.rotate((-8 - swing) * Math.PI / 180); draw(g, SWORD, -8 * px, -8 * px, px, { ...PAL, W: '#ffe9e9', c: '#ff8fa3', Y: '#7fe9ff' }); g.restore();
        if (hover || (t % 2.4) < 0.25) { g.fillStyle = 'rgba(255,255,255,.85)'; const sx = W * 0.65, sy = H * 0.35; g.fillRect(sx - px, sy, px * 3, px); g.fillRect(sx, sy - px, px, px * 3); }
      } else if (kind === 'mods') {
        draw(g, PUZZLE, W * 0.22, H * 0.22 + Math.sin(t * 2) * px * 0.6, px);
        g.save(); g.translate(W * 0.8, H * 0.6); g.rotate((Math.sin(t * (hover ? 8 : 1.5)) * (hover ? 35 : 10) - 30) * Math.PI / 180); draw(g, PICKAXE, -8 * px, -14 * px, px); g.restore();
      } else {
        const glow = 0.4 + 0.3 * Math.sin(t * 3);
        const gr = g.createRadialGradient(W * 0.7, H * 0.45, 2, W * 0.7, H * 0.45, H * 0.6); gr.addColorStop(0, `rgba(255,214,90,${glow})`); gr.addColorStop(1, 'rgba(255,214,90,0)');
        g.fillStyle = gr; g.fillRect(0, 0, W, H);
        draw(g, TROPHY, W * 0.7 - 8 * px, H * 0.12 + Math.sin(t * 2.2) * px * 0.5, px);
        for (let i = 0; i < 4; i++) { const a = t * 1.5 + i * 1.6; g.fillStyle = `rgba(255,240,170,${0.5 + 0.5 * Math.sin(a * 2)})`; g.fillRect(W * 0.7 + Math.cos(a) * H * 0.45, H * 0.45 + Math.sin(a) * H * 0.3, px, px); }
      }
      requestAnimationFrame(loop);
    };
    requestAnimationFrame(loop);
  };

  /** Корона над заголовком и кальмар в шапке. */
  api.crown = function (canvas) {
    const g = canvas.getContext('2d'); g.imageSmoothingEnabled = false;
    const px = Math.floor(canvas.width / 13);
    draw(g, CROWN, 0, 0, px);
  };
  api.squid = function (canvas) {
    const g = canvas.getContext('2d'); let t = 0, last = performance.now();
    const loop = (now) => {
      if (!canvas.isConnected) return;
      const dt = Math.min(0.05, (now - last) / 1000); last = now; t += dt;
      g.clearRect(0, 0, canvas.width, canvas.height); g.imageSmoothingEnabled = false;
      const px = Math.floor(canvas.width / 22);
      const ox = px * 4, oy = px * 2 + Math.sin(t * 1.4) * px * 1.5;
      // щупальца
      for (let k = 0; k < 5; k++) {
        for (let s = 0; s < 9; s++) {
          const wave = Math.sin(t * 3 + k * 0.9 + s * 0.6) * px * (0.4 + s * 0.12);
          g.fillStyle = s % 2 ? '#0f2f99' : '#1f5fff';
          g.fillRect(ox + px * (2 + k * 2.2) + wave + s * px * 0.55, oy + px * 10 + s * px * 1.1, px * 1.2, px * 1.2);
        }
      }
      draw(g, SQUID, ox, oy, px);
      // светящиеся глаза
      const pulse = 0.5 + 0.5 * Math.sin(t * 2);
      g.fillStyle = `rgba(98,240,255,${0.25 + pulse * 0.35})`;
      g.fillRect(ox + px * 3, oy + px * 3, px * 3, px * 4); g.fillRect(ox + px * 7, oy + px * 3, px * 3, px * 4);
      requestAnimationFrame(loop);
    };
    requestAnimationFrame(loop);
  };
  window.AquaArt = api;
})();
