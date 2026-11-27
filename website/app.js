/* Aqua Launcher — сайт: ссылки на самую новую версию с GitHub, анимации, язык. */
(() => {
  const REPO = 'mruliangg-cpu/aqua-launcher';
  const RELEASES = `https://github.com/${REPO}/releases/latest`;
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => [...r.querySelectorAll(s)];

  // ---------------------------------------------------------------- переводы
  const EN = {
    nav_features: 'Features', nav_shots: 'Screenshots', nav_download: 'Download', nav_get: 'Download',
    hero_badge: 'New version', hero_tag: 'Play. PvP. Enjoy.',
    hero_lead: 'A Minecraft launcher with the built-in Aqua Client: 237 modules, a Right Shift menu, a real FPS boost, 960 cosmetics and updates that install themselves automatically.',
    cta_win: 'Download for Windows', cta_mod: 'Aqua Client mod', hero_note: 'Free · Windows 10/11 · Minecraft 1.20 – 1.21.11',
    st_modules: 'Aqua Client modules', st_cos: 'cosmetic items', st_ver: 'Minecraft versions', st_upd: 'auto-updates',
    f_title: 'Everything you need to play', f_sub: 'One launcher — profiles, mods and a PvP client.',
    f1_t: 'Right Shift menu', f1_d: '237 modules: HUD, Keystrokes, CPS, Zoom, Freelook, minimap, TNT timer, waypoints and much more.',
    f2_t: 'A real FPS boost', f2_d: 'Graphics, particles and distance tuning, Unfocused FPS and RAM Cleaner — more frames on any PC.',
    f3_t: '960 cosmetics', f3_d: 'Capes, wings, hats, pets, auras and name icons — 120 in every slot, plus your own custom title above your name. Free.',
    f4_t: 'Updates itself', f4_d: 'The launcher finds a new version, downloads it and restarts automatically.',
    f5_t: 'Mods & profiles', f5_d: 'Thousands of Modrinth mods in one click, Fabric and any Minecraft version.',
    f6_t: 'Friends & leaderboard', f6_d: 'Add friends, see who is online and where, climb the Aqua leaderboard.',
    s_title: 'What it looks like', s_launcher: 'Launcher', s_game: 'In game',
    c_home: 'Home', c_locker: 'Locker — 3D try-on', c_preview: 'Aqua Client preview', c_cos: 'Cosmetics on the player', c_menu: 'Right Shift menu', c_hud: 'HUD editor',
    mk_home: 'Home', mk_builds: 'Builds', mk_servers: 'Servers', mk_mods: 'Mods', mk_friends: 'Friends', mk_cosmetics: 'Cosmetics', mk_settings: 'Settings', mk_online: 'Online', mk_premium: 'Premium', mk_level: 'Level 12', mk_quick: 'Quick play', mk_play: 'PLAY ›', mk_pvp: 'PvP servers', mk_rating: 'Leaderboard', mk_online_n: 'Online (3)',
    d_title: 'Download', d_rec: 'Recommended',
    d_setup: 'Installer (Setup)', d_setup_d: 'Installs in a minute and keeps itself up to date.', d_btn: 'Download',
    d_port: 'Aqua Launcher EXE (ready)', d_port_d: 'Just run it — no install. Updates itself too.',
    d_mod: 'Aqua Client mod', d_mod_d: 'For any launcher with Fabric. Requires Fabric API.', d_ver: 'Minecraft version',
    st1: 'Download and run the installer', st2: 'Sign in with Microsoft', st3: 'Press “Play” — Aqua Client is already inside',
    q1: 'Is it free?', a1: 'Yes. The launcher, the mod and all cosmetics are free.',
    q2: 'Do I need to own Minecraft?', a2: 'Yes — a Microsoft account with Minecraft: Java Edition. Without the game Minecraft starts in the official Demo mode.',
    q3: 'Is it a cheat?', a3: 'No. Aqua Client only has visual and quality-of-life features like popular PvP clients: HUD, zoom, FPS, cosmetics. No advantage over other players.',
    q4: 'How do I use the mod in another launcher?', a4: 'Install Fabric Loader for your version, put AquaClient-…-fabric.jar and Fabric API into .minecraft/mods and start the game. Menu — Right Shift.',
    q5: 'Who sees my cosmetics?', a5: 'Every player with Aqua Client. Your locker is tied to your account: pick items in the launcher and they appear in game, even from another launcher.',
    foot_note: 'Not affiliated with Mojang or Microsoft.',
  };
  const RU = {};
  $$('[data-t]').forEach((el) => { RU[el.dataset.t] = el.textContent; });
  let lang = (localStorage.getItem('aqua-lang') || (navigator.language || 'ru').slice(0, 2)) === 'ru' ? 'ru' : 'en';
  function applyLang() {
    const d = lang === 'ru' ? RU : EN;
    $$('[data-t]').forEach((el) => { if (d[el.dataset.t]) el.textContent = d[el.dataset.t]; });
    document.documentElement.lang = lang;
    $$('.langsw button').forEach((b) => b.classList.toggle('on', b.dataset.l === lang));
    fillDate();
  }
  $$('.langsw button').forEach((b) => b.onclick = () => { lang = b.dataset.l; try { localStorage.setItem('aqua-lang', lang); } catch {} applyLang(); });

  // ---------------------------------------------------------------- последняя версия с GitHub
  const FALLBACK_MC = ['1.21.11', '1.21.10', '1.21.9', '1.21.8', '1.21.7', '1.21.6', '1.21.5', '1.21.4', '1.21.3', '1.21.2', '1.21.1', '1.21',
    '1.20.6', '1.20.5', '1.20.4', '1.20.3', '1.20.2', '1.20.1', '1.20'];
  let release = null;
  const vcmp = (a, b) => { const x = a.split('.').map(Number), y = b.split('.').map(Number); for (let i = 0; i < 3; i++) if ((x[i] || 0) !== (y[i] || 0)) return (y[i] || 0) - (x[i] || 0); return 0; };
  const mb = (n) => (n / 1048576).toFixed(0) + ' MB';
  function fillDate() {
    if (!release) return;
    const d = new Date(release.published_at);
    $('#rel-date').textContent = `${release.tag_name} · ${d.toLocaleDateString(lang === 'ru' ? 'ru-RU' : 'en-US', { day: 'numeric', month: 'long', year: 'numeric' })}`;
  }
  function modList(mods) {
    const sel = $('#mc');
    sel.innerHTML = mods.map((m) => `<option value="${m.mc}">Minecraft ${m.mc}</option>`).join('');
    const update = () => {
      const m = mods.find((x) => x.mc === sel.value) || mods[0];
      $('#dl-mod').href = m.url;
      $('#mod-name').textContent = m.name + (m.size ? ' · ' + mb(m.size) : '');
    };
    sel.onchange = update; update();
  }
  modList(FALLBACK_MC.map((mc) => ({ mc, name: `AquaClient-${mc}-fabric.jar`, url: `${RELEASES}/download/AquaClient-${mc}-fabric.jar` })));
  fetch(`https://api.github.com/repos/${REPO}/releases/latest`).then((r) => (r.ok ? r.json() : Promise.reject(r.status))).then((rel) => {
    release = rel;
    const assets = rel.assets || [];
    $('#ver').textContent = rel.tag_name;
    const setup = assets.find((a) => /Setup.*\.exe$/i.test(a.name)), port = assets.find((a) => /Portable.*\.exe$/i.test(a.name));
    const main = port || setup; // готовая EXE — основная кнопка
    if (main) {
      for (const id of ['#dl-setup', '#dl-portable']) { const el = $(id); if (el) el.href = main.browser_download_url; }
      $('#port-name').textContent = `${main.name} · ${mb(main.size)}`;
      $('#setup-size').textContent = `${rel.tag_name} · ${mb(main.size)}`;
    }
    if (setup) { const el = $('#dl-setup2'); if (el) el.href = setup.browser_download_url; $('#setup-name').textContent = `${setup.name} · ${mb(setup.size)}`; }
    const mods = assets.map((a) => { const m = /^AquaClient-(.+)-fabric\.jar$/.exec(a.name); return m && { mc: m[1], name: a.name, url: a.browser_download_url, size: a.size }; })
      .filter(Boolean).sort((a, b) => vcmp(a.mc, b.mc));
    if (mods.length) modList(mods);
    $('[data-count="19"]').dataset.count = String(mods.length || 19);
    fillDate();
  }).catch(() => { $('#ver').textContent = 'latest'; });

  // ---------------------------------------------------------------- анимации
  const io = new IntersectionObserver((es) => es.forEach((e) => {
    if (!e.isIntersecting) return;
    e.target.classList.add('in'); io.unobserve(e.target);
    $$('[data-count]', e.target).forEach(countUp);
  }), { threshold: 0.12 });
  $$('.reveal').forEach((el, i) => { el.style.transitionDelay = (i % 3) * 80 + 'ms'; io.observe(el); });
  // страховка: что бы ни случилось с наблюдателем — контент показываем
  setTimeout(() => $$('.reveal:not(.in)').forEach((e) => { e.classList.add('in'); $$('[data-count]', e).forEach(countUp); }), 2200);
  function countUp(el) {
    const to = +el.dataset.count, t0 = performance.now();
    const step = (t) => { const k = Math.min(1, (t - t0) / 1400); el.textContent = Math.round(to * (1 - Math.pow(1 - k, 3))); if (k < 1) requestAnimationFrame(step); };
    requestAnimationFrame(step);
  }
  addEventListener('scroll', () => $('#nav').classList.toggle('scrolled', scrollY > 10), { passive: true });

  // наклон скриншота за мышкой
  const tilt = $('#tilt');
  if (tilt && matchMedia('(pointer: fine)').matches) {
    tilt.addEventListener('pointermove', (e) => {
      const r = tilt.getBoundingClientRect(), x = (e.clientX - r.left) / r.width - 0.5, y = (e.clientY - r.top) / r.height - 0.5;
      tilt.style.transform = `perspective(1200px) rotateY(${x * 8}deg) rotateX(${-y * 8}deg)`;
    });
    tilt.addEventListener('pointerleave', () => { tilt.style.transform = ''; });
  }

  // вкладки скриншотов и просмотр
  $$('.tabs button').forEach((b) => b.onclick = () => {
    $$('.tabs button').forEach((x) => x.classList.toggle('on', x === b));
    $$('.gallery').forEach((g) => { g.hidden = g.dataset.group !== b.dataset.tab; if (!g.hidden) g.classList.add('in'); });
  });
  const lb = $('#lightbox');
  $$('.gallery img').forEach((img) => img.onclick = () => { $('img', lb).src = img.src; lb.hidden = false; });
  lb.onclick = () => { lb.hidden = true; };
  addEventListener('keydown', (e) => { if (e.key === 'Escape') lb.hidden = true; });

  applyLang();
})();
