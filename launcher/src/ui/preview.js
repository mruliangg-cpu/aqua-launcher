/* Предпросмотр Aqua Client прямо в лаунчере: копия меню Right Shift (категории, модули, настройки, поиск),
   HUD поверх игровой сцены и редактор HUD с перетаскиванием. Данные модулей — assets/client-modules.js. */
(function () {
  const KEY = 'aqua-preview-v1';
  const HUD_SAMPLE = {
    Watermark: 'Aqua Client', FPS: '144 FPS', CPS: '12 | 4 CPS', Ping: '42 ms', 'Server Address': 'mc.hypixel.net', Memory: '1.4 / 4 GB',
    Speed: '4.32 m/s', 'Day Counter': 'Day 12', Saturation: 'Sat.: 5.0', 'Pack Display': 'Aqua 16x', 'Light Level': 'Light: 7 ☀ 15',
    Coordinates: 'XYZ: 128 64 -302', 'Direction HUD': 'N ↑ 182°', Stopwatch: '01:24.6', 'Combo Counter': 'Combo: 4', 'Reach Display': '3.02 blocks',
  };
  let st = load(), root = null, T = (k) => k, raf = 0, keyHandler = null;

  function load() {
    try { return JSON.parse(localStorage.getItem(KEY)) || {}; } catch { return {}; }
  }
  function save() { try { localStorage.setItem(KEY, JSON.stringify(st)); } catch { /* нет хранилища */ } }
  const data = () => window.AQUA_CLIENT || { cats: [], mods: [] };
  const on = (m) => (st.on && m.n in st.on ? st.on[m.n] : m.on);
  const val = (m, s) => (st.v && st.v[m.n] && s.n in st.v[m.n] ? st.v[m.n][s.n] : s.v);
  const setVal = (m, s, v) => { st.v = st.v || {}; (st.v[m.n] = st.v[m.n] || {})[s.n] = v; save(); };
  const esc = (x) => String(x).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
  const ru = () => (document.documentElement.lang || 'ru') !== 'en';

  // ------------------------------------------------------------------ сцена
  function drawWorld(cv) {
    const g = cv.getContext('2d'), W = cv.width = 480, H = cv.height = 270;
    g.imageSmoothingEnabled = false;
    const sky = g.createLinearGradient(0, 0, 0, H);
    sky.addColorStop(0, '#5aa9ff'); sky.addColorStop(0.6, '#a6d6ff'); sky.addColorStop(1, '#d9f0ff');
    g.fillStyle = sky; g.fillRect(0, 0, W, H);
    g.fillStyle = '#fff6b0'; g.fillRect(372, 34, 26, 26);
    const rnd = (i) => { const x = Math.sin(i * 127.1) * 43758.5453; return x - Math.floor(x); };
    for (let c = 0; c < 7; c++) { g.fillStyle = 'rgba(255,255,255,.85)'; const x = (c * 83 + 20) % W, y = 18 + rnd(c) * 50; g.fillRect(x, y, 34 + rnd(c + 9) * 30, 8); g.fillRect(x + 8, y - 6, 20, 6); }
    const B = 10;
    for (let x = 0; x < W / B; x++) {
      const far = Math.floor(12 + Math.sin(x * 0.25) * 2 + rnd(x) * 2);
      for (let y = far; y < H / B; y++) { g.fillStyle = y === far ? '#5f9f5a' : '#6d8c74'; g.fillRect(x * B, y * B, B, B); }
      const h = Math.floor(17 + Math.sin(x * 0.18) * 2.5 + Math.sin(x * 0.55) * 1.2);
      for (let y = h; y < H / B; y++) {
        const d = y - h;
        g.fillStyle = d === 0 ? (rnd(x * 7 + y) > 0.5 ? '#5dbb3f' : '#4fae36') : d < 3 ? (rnd(x + y * 3) > 0.5 ? '#8a5a35' : '#7a4e2e') : (rnd(x * 3 + y) > 0.5 ? '#7d7d7d' : '#6c6c6c');
        g.fillRect(x * B, y * B, B, B);
      }
      if (x % 9 === 4) { // дерево
        for (let k = 1; k <= 4; k++) { g.fillStyle = '#6b4a2a'; g.fillRect(x * B, (h - k) * B, B, B); }
        g.fillStyle = '#2f8a2f';
        for (let a = -2; a <= 2; a++) for (let b = 5; b <= 7; b++) if (Math.abs(a) + (b === 7 ? 1 : 0) < 3) g.fillRect((x + a) * B, (h - b) * B, B, B);
      }
    }
    g.fillStyle = 'rgba(0,0,0,.06)';
    for (let x = 0; x < W; x += B) g.fillRect(x, 0, 1, H);
  }

  // ------------------------------------------------------------------ HUD
  function hudText(m) {
    if (m.n === 'Clock') return new Date().toTimeString().slice(0, 5);
    return HUD_SAMPLE[m.n] || (m.x && m.x !== m.n ? m.x : m.n);
  }
  function hudHtml(m) {
    if (m.n === 'Keystrokes') return `<div class="pv-keys"><i></i><b>W</b><i></i><b>A</b><b class="on">S</b><b>D</b><b class="wide">LMB</b><b class="wide">RMB</b></div>`;
    if (m.n === 'Armor Status') return `<div class="pv-armor">⛑ 363<br>🛡 528<br>👖 495<br>👢 429</div>`;
    if (m.n === 'Potion Effects') return `<div class="pv-pot">⚡ ${ru() ? 'Скорость II' : 'Speed II'} <small>1:30</small><br>🔥 ${ru() ? 'Огнестойкость' : 'Fire Res.'} <small>3:00</small></div>`;
    if (m.n === 'Minimap') return `<div class="pv-mini"><i></i></div>`;
    const style = m.s.find((s) => s.n === 'Стиль');
    const txt = esc(hudText(m)), sv = style ? val(m, style) : 'Плашка';
    return sv === 'Скобки' ? `[${txt}]` : txt;
  }
  function renderHud(scene, editing) {
    const layer = scene.querySelector('.pv-hud');
    const hud = data().mods.filter((m) => m.hud && on(m));
    st.pos = st.pos || {};
    let li = 0;
    const auto = [];
    layer.innerHTML = hud.map((m) => {
      if (!st.pos[m.n]) {
        const right = ['Clock', 'Server Address', 'Memory', 'Minimap', 'Potion Effects', 'Armor Status'].includes(m.n) || li++ > 5;
        auto.push([m.n, right]);
      }
      const p = st.pos[m.n] || { x: 0, y: 0 }, bg = m.s.find((s) => s.n === 'Фон'), tc = m.s.find((s) => s.n === 'Цвет текста'), sc = m.s.find((s) => s.n === 'Размер');
      const plain = (bg && !val(m, bg)) || (m.s.find((s) => s.n === 'Стиль') && val(m, m.s.find((s) => s.n === 'Стиль')) === 'Текст');
      return `<div class="pv-el ${plain ? 'plain' : ''} ${editing ? 'edit' : ''}" data-n="${esc(m.n)}" style="left:${p.x * 100}%;top:${p.y * 100}%;${tc ? `color:${val(m, tc)};` : ''}${sc ? `--s:${val(m, sc)};` : ''}">${hudHtml(m)}</div>`;
    }).join('');
    // раскладка новых элементов: столбики слева и справа без наложений
    if (auto.length) {
      const R = layer.getBoundingClientRect();
      if (R.width) {
        const used = { false: 0.02, true: 0.02 };
        layer.querySelectorAll('.pv-el').forEach((el) => {
          const p = st.pos[el.dataset.n];
          if (p) { const b = el.getBoundingClientRect(); const col = p.x > 0.5; used[col] = Math.max(used[col], p.y + b.height / R.height + 0.015); }
        });
        auto.forEach(([n, right]) => {
          const el = layer.querySelector(`.pv-el[data-n="${CSS.escape(n)}"]`); if (!el) return;
          const b = el.getBoundingClientRect(), h = b.height / R.height, w = b.width / R.width;
          if (used[right] + h > 0.86) return; // не влезло — оставим на месте, пользователь подвинет
          const p = { x: right ? 1 - w - 0.012 : 0.012, y: used[right] };
          used[right] += h + 0.015;
          st.pos[n] = p; el.style.left = p.x * 100 + '%'; el.style.top = p.y * 100 + '%';
        });
      }
    }
    save();
    if (!editing) return;
    layer.querySelectorAll('.pv-el').forEach((el) => {
      el.onpointerdown = (e) => {
        const r = layer.getBoundingClientRect(), b = el.getBoundingClientRect();
        const dx = e.clientX - b.left, dy = e.clientY - b.top;
        el.setPointerCapture(e.pointerId); el.classList.add('drag');
        el.onpointermove = (ev) => {
          const x = Math.max(0, Math.min(1 - b.width / r.width, (ev.clientX - dx - r.left) / r.width));
          const y = Math.max(0, Math.min(1 - b.height / r.height, (ev.clientY - dy - r.top) / r.height));
          el.style.left = x * 100 + '%'; el.style.top = y * 100 + '%';
          st.pos[el.dataset.n] = { x, y };
        };
        el.onpointerup = () => { el.onpointermove = null; el.classList.remove('drag'); save(); };
      };
    });
  }

  // ------------------------------------------------------------------ меню Right Shift
  let cat = null, query = '', openMod = null;
  function guiHtml() {
    const d = data();
    const cats = [{ id: null, t: ru() ? 'Все модули' : 'All modules', i: '☰' }, ...d.cats.map((c) => ({ ...c, t: ru() ? c.t : ({ Бой: 'Combat', Визуал: 'Render', Движение: 'Movement', Косметика: 'Cosmetic', Разное: 'Misc' }[c.t] || c.t) }))];
    const side = cats.map((c) => `<button class="pv-cat ${cat === c.id ? 'on' : ''}" data-c="${c.id || ''}"><span>${c.i}</span>${esc(c.t)}<small>${c.id ? d.mods.filter((m) => m.c === c.id).length : d.mods.length}</small></button>`).join('');
    let body;
    if (openMod) {
      const m = openMod;
      body = `<div class="pv-set"><div class="pv-set-head"><button class="pv-back">‹</button><div><b>${esc(m.n)}</b><small>${esc(m.d)}</small></div>
          <button class="pv-toggle ${on(m) ? 'on' : ''}" data-t="${esc(m.n)}"><i></i></button></div>
        <div class="pv-set-list">${m.s.length ? m.s.map((s, i) => setRow(m, s, i)).join('') : `<p class="pv-empty">${ru() ? 'У этого модуля нет настроек — просто включи его.' : 'No settings — just toggle it.'}</p>`}</div></div>`;
    } else {
      const q = query.toLowerCase();
      const mods = d.mods.filter((m) => (!cat || m.c === cat) && (!q || m.n.toLowerCase().includes(q) || m.d.toLowerCase().includes(q)));
      body = `<div class="pv-grid">${mods.map((m) => `<div class="pv-card ${on(m) ? 'on' : ''}" data-m="${esc(m.n)}">
          <div class="pv-card-t"><b>${esc(m.n)}</b>${m.s.length ? '<button class="pv-gear" title="⚙">⚙</button>' : ''}</div>
          <small>${esc(m.d)}</small><button class="pv-toggle ${on(m) ? 'on' : ''}" data-t="${esc(m.n)}"><i></i></button></div>`).join('') || `<p class="pv-empty">${ru() ? 'Ничего не найдено' : 'Nothing found'}</p>`}</div>`;
    }
    return `<div class="pv-gui"><aside><div class="pv-logo">🐢 <b>AQUA</b> CLIENT</div>${side}<button class="pv-cat pv-hudbtn" data-hud="1"><span>✥</span>${ru() ? 'Редактор HUD' : 'HUD editor'}</button></aside>
      <main><header><input class="pv-search" placeholder="${ru() ? 'Поиск модулей…' : 'Search modules…'}" value="${esc(query)}" ${openMod ? 'disabled' : ''}><span class="pv-count">${d.mods.filter(on).length}/${d.mods.length} ${ru() ? 'вкл.' : 'on'}</span></header>${body}</main></div>`;
  }
  function setRow(m, s, i) {
    const v = val(m, s), id = `data-i="${i}"`;
    let ctl = '';
    if (s.t === 'bool') ctl = `<button class="pv-toggle ${v ? 'on' : ''}" ${id}><i></i></button>`;
    else if (s.t === 'number') ctl = `<input type="range" ${id} min="${s.min}" max="${s.max}" step="${s.step}" value="${v}"><output>${+(+v).toFixed(2)}${esc(s.u || '')}</output>`;
    else if (s.t === 'mode') ctl = `<button class="pv-mode" ${id}>‹ ${esc(v)} ›</button>`;
    else if (s.t === 'color') ctl = `<input type="color" ${id} value="${esc(v)}">`;
    else if (s.t === 'key') ctl = `<button class="pv-mode" ${id}>${esc(v)}</button>`;
    else if (s.t === 'string') ctl = `<input class="pv-str" ${id} value="${esc(v)}">`;
    else ctl = `<button class="pv-mode" ${id}>${esc(s.v)}</button>`;
    return `<div class="pv-row"><span>${esc(s.n)}</span><div class="pv-ctl">${ctl}</div></div>`;
  }

  function wireGui(scene) {
    const gui = scene.querySelector('.pv-gui');
    const d = data(), byName = (n) => d.mods.find((m) => m.n === n);
    const redraw = () => { scene.querySelector('.pv-gui-wrap').innerHTML = guiHtml(); wireGui(scene); renderHud(scene, false); };
    gui.querySelectorAll('.pv-cat[data-c]').forEach((b) => b.onclick = () => { cat = b.dataset.c || null; openMod = null; redraw(); });
    gui.querySelector('.pv-hudbtn').onclick = () => setMode(scene, 'hud');
    const s = gui.querySelector('.pv-search');
    s.oninput = () => { query = s.value; const p = s.selectionStart; redraw(); const n = scene.querySelector('.pv-search'); n.focus(); n.setSelectionRange(p, p); };
    gui.querySelectorAll('.pv-toggle[data-t]').forEach((b) => b.onclick = (e) => {
      e.stopPropagation();
      const m = byName(b.dataset.t); st.on = st.on || {}; st.on[m.n] = !on(m); save();
      const r = b.getBoundingClientRect(), gr = gui.getBoundingClientRect();
      redraw();
      const fx = document.createElement('i'); fx.className = 'pv-burst'; fx.style.left = (r.left - gr.left + r.width / 2) + 'px'; fx.style.top = (r.top - gr.top + r.height / 2) + 'px';
      scene.querySelector('.pv-gui').appendChild(fx); setTimeout(() => fx.remove(), 600);
    });
    gui.querySelectorAll('.pv-card').forEach((c) => c.onclick = () => { const m = byName(c.dataset.m); if (m.s.length) { openMod = m; redraw(); } });
    const back = gui.querySelector('.pv-back'); if (back) back.onclick = () => { openMod = null; redraw(); };
    if (!openMod) return;
    const m = openMod;
    gui.querySelectorAll('.pv-set-list [data-i]').forEach((el) => {
      const sd = m.s[+el.dataset.i];
      if (sd.t === 'bool') el.onclick = () => { setVal(m, sd, !val(m, sd)); redraw(); };
      else if (sd.t === 'number') el.oninput = () => { setVal(m, sd, +el.value); el.nextElementSibling.textContent = +(+el.value).toFixed(2) + (sd.u || ''); renderHud(scene, false); };
      else if (sd.t === 'mode') el.onclick = () => { const o = sd.o, k = o.indexOf(val(m, sd)); setVal(m, sd, o[(k + 1) % o.length]); redraw(); };
      else if (sd.t === 'color') el.oninput = () => { setVal(m, sd, el.value); renderHud(scene, false); };
      else if (sd.t === 'string') el.onchange = () => setVal(m, sd, el.value);
      else if (sd.t === 'key') el.onclick = () => {
        el.textContent = '…';
        const h = (e) => { e.preventDefault(); setVal(m, sd, e.code === 'Escape' ? 'None' : e.code.replace(/^Key|^Digit/, '')); window.removeEventListener('keydown', h, true); redraw(); };
        window.addEventListener('keydown', h, true);
      };
    });
  }

  function setMode(scene, mode) {
    scene.dataset.mode = mode;
    const wrap = scene.querySelector('.pv-gui-wrap');
    if (mode === 'gui') { wrap.innerHTML = guiHtml(); wireGui(scene); }
    else wrap.innerHTML = '';
    renderHud(scene, mode === 'hud');
    root.querySelectorAll('[data-pvmode]').forEach((b) => b.classList.toggle('on', b.dataset.pvmode === mode));
  }

  function render(el, t) {
    root = el; T = t;
    el.innerHTML = `
      <div class="pv-bar glass">
        <div class="seg"><button data-pvmode="gui">⌨ ${t('pv_menu')}</button><button data-pvmode="hud">✥ ${t('pv_hud')}</button><button data-pvmode="play">🎮 ${t('pv_game')}</button></div>
        <span class="note">${t('pv_hint')}</span>
        <button class="btn ghost small" id="pv-reset">${t('pv_reset')}</button>
      </div>
      <div class="pv-scene" data-mode="gui">
        <canvas class="pv-world"></canvas>
        <div class="pv-hud"></div>
        <div class="pv-cross">+</div>
        <div class="pv-hotbar">${Array.from({ length: 9 }, (_, i) => `<i class="${i === 0 ? 'sel' : ''}">${['⚔', '🏹', '🧱', '🍖', '🪓', '⛏', '🧪', '🪣', '🎣'][i]}</i>`).join('')}</div>
        <div class="pv-gui-wrap"></div>
        <div class="pv-tip">${t('pv_tip_hud')}</div>
      </div>`;
    const scene = el.querySelector('.pv-scene');
    drawWorld(scene.querySelector('.pv-world'));
    el.querySelectorAll('[data-pvmode]').forEach((b) => b.onclick = () => setMode(scene, b.dataset.pvmode));
    el.querySelector('#pv-reset').onclick = () => { st = {}; save(); cat = null; query = ''; openMod = null; setMode(scene, 'gui'); };
    setMode(scene, scene.dataset.mode || 'gui');
    // Right Shift открывает/закрывает меню — как в игре
    if (keyHandler) window.removeEventListener('keydown', keyHandler);
    keyHandler = (e) => {
      if (!document.body.contains(scene) || scene.offsetParent === null) return;
      if (e.code === 'ShiftRight') setMode(scene, scene.dataset.mode === 'gui' ? 'play' : 'gui');
      else if (e.code === 'Escape' && scene.dataset.mode !== 'play') setMode(scene, 'play');
    };
    window.addEventListener('keydown', keyHandler);
    clearTimeout(raf);
    const tick = () => { // часы в HUD
      const c = scene.querySelector('.pv-el[data-n="Clock"]');
      if (c) c.textContent = new Date().toTimeString().slice(0, 5);
      raf = setTimeout(tick, 15000);
    };
    tick();
  }

  window.AquaPreview = { render };
})();
