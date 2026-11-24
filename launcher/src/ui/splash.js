// Lunar-style launch splash: shows a full-screen animated loading screen
// from the moment Play is pressed until the Minecraft window opens.
(function () {
  const E = (id) => document.getElementById(id);
  const tr = (k, d) => (window.t ? window.t(k) : d) || d;

  let tipTimer = null, shown = false, safety = null;
  let raf = null, cv = null, g = null, parts = [];

  function tips() {
    const out = [];
    for (let i = 0; i < 6; i++) { const s = window.t ? window.t('splash_tip_' + i) : ''; if (s && s !== 'splash_tip_' + i) out.push(s); }
    return out;
  }

  function rotateTip() {
    const el = E('ls-tip'); if (!el) return;
    const arr = tips(); if (!arr.length) { el.textContent = ''; return; }
    let i = Math.floor(Math.random() * arr.length);
    const set = () => { el.style.opacity = '0'; setTimeout(() => { el.textContent = '💡 ' + arr[i % arr.length]; el.style.opacity = '1'; i++; }, 260); };
    set();
    clearInterval(tipTimer);
    tipTimer = setInterval(set, 4200);
  }

  // --- rising-bubble canvas fx ---
  function mk() { const w = cv ? cv.width : 800, h = cv ? cv.height : 600; return { x: Math.random() * w, y: h + Math.random() * h, r: 1 + Math.random() * 3, s: 0.3 + Math.random() * 1.2, a: 0.15 + Math.random() * 0.5 }; }
  function resize() { if (!cv) return; cv.width = cv.clientWidth || window.innerWidth; cv.height = cv.clientHeight || window.innerHeight; }
  function loop() { if (!g || !cv) return; g.clearRect(0, 0, cv.width, cv.height); for (const p of parts) { p.y -= p.s; p.x += Math.sin((p.y + p.r) * 0.03) * 0.3; if (p.y < -6) { Object.assign(p, mk()); p.y = cv.height + 6; } g.beginPath(); g.fillStyle = 'rgba(120,230,255,' + p.a + ')'; g.arc(p.x, p.y, p.r, 0, 6.2832); g.fill(); } raf = requestAnimationFrame(loop); }
  function startFx() { cv = E('splash-fx'); if (!cv) return; g = cv.getContext('2d'); resize(); window.addEventListener('resize', resize); parts = Array.from({ length: 46 }, mk); if (!raf) loop(); }
  function stopFx() { if (raf) cancelAnimationFrame(raf); raf = null; window.removeEventListener('resize', resize); }

  const Splash = {
    show() {
      const s = E('launch-splash'); if (!s) return;
      shown = true;
      s.hidden = false;
      requestAnimationFrame(() => s.classList.add('on'));
      E('ls-status').textContent = tr('launching', 'LAUNCHING…');
      E('ls-bar-i').style.width = '6%';
      const hide = E('ls-hide'); if (hide) { hide.hidden = false; hide.textContent = tr('splash_hide', 'Hide'); hide.onclick = () => Splash.hide(); }
      rotateTip();
      startFx();
      clearTimeout(safety);
      safety = setTimeout(() => Splash.hide(), 180000); // never stay stuck
    },
    progress(p) {
      if (!shown || !p) return;
      const pct = p.total ? Math.min(100, Math.round((p.done / p.total) * 100)) : null;
      E('ls-bar-i').style.width = (pct == null ? 100 : Math.max(6, pct)) + '%';
      E('ls-status').textContent = (p.task || tr('launching', 'LAUNCHING…')) + (pct != null && p.total > 1 ? ' — ' + pct + '%' : '');
    },
    started() {
      if (!shown) return;
      E('ls-status').textContent = tr('splash_opening', 'Opening Minecraft…');
      E('ls-bar-i').style.width = '100%';
      setTimeout(() => Splash.hide(), 1500);
    },
    hide() {
      const s = E('launch-splash'); if (!s || !shown) return;
      shown = false;
      clearInterval(tipTimer);
      clearTimeout(safety);
      s.classList.remove('on');
      setTimeout(() => { s.hidden = true; stopFx(); }, 540);
    }
  };

  window.Splash = Splash;
})();
