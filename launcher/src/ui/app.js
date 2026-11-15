/* Aqua Launcher — логика интерфейса (EN/RU) */
const $ = (s, el = document) => el.querySelector(s);
const $$ = (s, el = document) => [...el.querySelectorAll(s)];
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const LOADERS = { vanilla: 'Vanilla', fabric: 'Fabric', quilt: 'Quilt', forge: 'Forge', neoforge: 'NeoForge' };
const ICONS = ['🐢', '🐟', '🐠', '🐡', '🐙', '🦑', '🦀', '🐬', '🐳', '🦈', '🪼', '🌊', '🪸', '🫧', '💎', '🔱', '⚓', '🏝️', '🟣', '👁️'];
const OWNER = 'mrulian_gg';
const CLIENT_MODULES = 80;
let AQUA_MC = ['1.21', '1.21.1'];
const fmt = (n) => n >= 1e6 ? (n / 1e6).toFixed(1) + 'M' : n >= 1e3 ? (n / 1e3).toFixed(1) + 'K' : String(n);

let S = null, INFO = null, VERSIONS = null;
let running = false, busy = false;
const logLines = [];

// ======================================================================= ЯЗЫК
const lang = () => (S && S.settings.language === 'ru' ? 'ru' : 'en');
function t(key, vars) {
  let s = (I18N[lang()] && I18N[lang()][key]) ?? I18N.en[key] ?? key;
  if (vars) for (const [k, v] of Object.entries(vars)) s = s.replaceAll(`{${k}}`, v);
  return s;
}
function applyI18n() {
  document.documentElement.lang = lang();
  $$('[data-i18n]').forEach((el) => { el.innerHTML = t(el.dataset.i18n); });
  $$('[data-i18n-ph]').forEach((el) => { el.placeholder = t(el.dataset.i18nPh); });
  $$('[data-win]').forEach((b) => { b.title = b.dataset.win; });
}
const cosName = (slot, id) => { const it = COSMETICS.items[slot].find((x) => x[0] === id); return it ? (lang() === 'ru' ? it[3] : it[2]) : id; };
const cosEmoji = (slot, id) => { const it = COSMETICS.items[slot].find((x) => x[0] === id); return it ? it[1] : '❔'; };

// ======================================================================= ОБЩЕЕ
let audioCtx;
function blip(freq = 520) {
  if (!S || !S.settings.uiSounds) return;
  try {
    audioCtx = audioCtx || new AudioContext();
    const o = audioCtx.createOscillator(), g = audioCtx.createGain();
    o.type = 'sine';
    o.frequency.setValueAtTime(freq, audioCtx.currentTime);
    o.frequency.exponentialRampToValueAtTime(freq * 1.8, audioCtx.currentTime + 0.09);
    g.gain.setValueAtTime(0.06, audioCtx.currentTime);
    g.gain.exponentialRampToValueAtTime(0.0001, audioCtx.currentTime + 0.14);
    o.connect(g).connect(audioCtx.destination);
    o.start(); o.stop(audioCtx.currentTime + 0.15);
  } catch { /* без звука */ }
}

function toast(msg, type = '') {
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  const life = type === 'err' ? 6500 : 3500;
  el.innerHTML = `<span>${type === 'err' ? '⚠️' : type === 'ok' ? '✅' : '🫧'}</span><span>${esc(msg)}</span><i class="toast-bar" style="animation-duration:${life}ms"></i>`;
  $('#toasts').appendChild(el);
  if (type === 'ok') blip(700);
  setTimeout(() => { el.classList.add('out'); setTimeout(() => el.remove(), 300); }, life);
}

async function safe(fn, okMsg) {
  try { const r = await fn(); if (okMsg) toast(okMsg, 'ok'); return r === undefined ? true : r; } catch (e) { toast(e.message, 'err'); return undefined; }
}

/** Аккаунт без купленной игры — подсказка про демо-режим. */
function demoNote() { setTimeout(() => { const a = selectedAccount(); if (a && a.demo) toast(t('demo_note'), 'ok'); }, 600); }
function modal(html, cls = '') {
  const root = $('#modal-root');
  root.innerHTML = `<div class="backdrop"><div class="modal glass ${cls}">${html}</div></div>`;
  const back = $('.backdrop', root);
  const close = () => { root.innerHTML = ''; };
  back.addEventListener('mousedown', (e) => { if (e.target === back) close(); });
  $$('[data-close]', root).forEach((b) => b.addEventListener('click', close));
  return { el: $('.modal', root), close };
}

function confirmBox(title, text, okLabel) {
  if (!S.settings.confirmDelete) return Promise.resolve(true);
  return new Promise((resolve) => {
    const m = modal(`<div class="modal-head"><h3>${esc(title)}</h3><button class="x" data-close>✕</button></div>
      <div class="modal-body"><p>${text}</p></div>
      <div class="modal-foot"><button class="btn ghost" data-close>${t('cancel')}</button><button class="btn danger" id="cb-ok">${esc(okLabel || t('delete'))}</button></div>`, 'narrow');
    $('#cb-ok', m.el).onclick = () => { m.close(); resolve(true); };
    $$('[data-close]', m.el).forEach((b) => b.addEventListener('click', () => resolve(false)));
  });
}

const selectedInstance = () => S.instances.find((i) => i.id === S.selectedInstance) || S.instances[0];
const selectedAccount = () => S.accounts.find((a) => a.id === S.selectedAccount);
const isOwner = (a) => a && String(a.name).toLowerCase() === OWNER;
const avatar = (a) => a ? `https://mc-heads.net/avatar/${encodeURIComponent(a.type === 'microsoft' ? a.uuid : a.name)}/64` : '';

function badges(inst) {
  const b = [`<span class="badge">${esc(inst.mc)}</span>`, `<span class="badge loader">${LOADERS[inst.loader] || inst.loader}</span>`];
  if (inst.loader === 'fabric' && AQUA_MC.includes(inst.mc)) b.push('<span class="badge ender">Aqua Client</span>');
  if (inst.fpsBoost && inst.loader !== 'vanilla') b.push('<span class="badge fps">⚡ FPS Boost</span>');
  return b.join('');
}

// ======================================================================= НАВИГАЦИЯ
/** Плавающая подсветка под активным пунктом меню. */
function moveNavGlider() {
  const act = $('.nav.active'), gl = $('#nav-glider');
  if (!act || !gl) return;
  gl.style.transform = `translateY(${act.offsetTop}px)`;
  gl.style.height = act.offsetHeight + 'px';
  gl.style.opacity = 1;
}
/** Волна, пробегающая по контенту при смене страницы. */
function pageWave() {
  if (document.body.classList.contains('reduce-motion')) return;
  const w = $('#page-wave');
  if (!w) return;
  w.classList.remove('go'); void w.offsetWidth; w.classList.add('go');
}
window.addEventListener('resize', () => moveNavGlider());
function go(page) {
  blip(440);
  $$('.nav').forEach((n) => n.classList.toggle('active', n.dataset.page === page));
  $$('.page').forEach((p) => p.classList.toggle('active', p.id === `page-${page}`));
  moveNavGlider();
  pageWave();
  if (page === 'mods') loadMods(true);
  if (page === 'console') scrollConsole();
  if (page === 'servers') renderServers(true);
  if (page === 'settings') renderSettings();
  if (page === 'locker') renderLocker();
  if (page === 'skins') renderSkins();
  if (page === 'friends') renderFriends();
  if (page === 'leaders') renderLeaders();
  if (page === 'preview' && window.AquaPreview) AquaPreview.render($('#preview-root'), t);
}

// ======================================================================= ТЕМА / ВНЕШНИЙ ВИД
const THEMES = {
  aqua: ['#3ee6ff', '#5ff2c8'], turtle: ['#6fe36b', '#c6f56a'], coral: ['#ff7a8a', '#ffb36b'], deep: ['#4d8cff', '#3ee6ff'], gold: ['#ffd86b', '#ff9f43'],
};
function applyLook() {
  const st = S.settings;
  const [a, b] = THEMES[st.theme] || THEMES.aqua;
  document.documentElement.style.setProperty('--aqua', a);
  document.documentElement.style.setProperty('--prism', b);
  document.body.classList.toggle('reduce-motion', !!st.reduceMotion);
  if (window.Ocean) Ocean.setQuality(st.bgQuality || 'high');
}

// ======================================================================= РЕНДЕР
function renderAll() {
  applyI18n();
  applyLook();
  renderAccount();
  renderHome();
  renderInstances();
  renderModsSelect();
  if ($('#page-settings').classList.contains('active')) renderSettings();
  if ($('#page-locker').classList.contains('active')) renderLocker();
}

function renderAccount() {
  const a = selectedAccount();
  $('#account-name').innerHTML = a ? `<span class="${isOwner(a) ? 'owner-name' : 'aqua-name'}">${esc(a.name)}</span>` : esc(t('sign_in'));
  $('#account-type').innerHTML = a ? `${isOwner(a) ? '<span class="owner-tag">🐢 OWNER</span>· ' : '🐢 '}${a.type === 'microsoft' ? t('microsoft') : t('offline')}` : t('no_account');
  $('#account-avatar').src = a ? avatar(a) : 'data:image/gif;base64,R0lGODlhAQABAAAAACw=';
}

function renderHome() {
  const n = VERSIONS ? VERSIONS.versions.length : '800+';
  if ($('#hero-sub')) $('#hero-sub').innerHTML = t('hero_sub', { n });
  const inst = selectedInstance();
  $('#current-instance').innerHTML = inst ? `<span>${esc(inst.icon)}</span><span>${esc(inst.name)} · ${esc(inst.mc)} ${LOADERS[inst.loader]}</span><span>›</span>` : `<span>➕ ${t('create_first')}</span>`;
  const q = $('#quick-instance');
  q.innerHTML = S.instances.map((i) => `<option value="${esc(i.id)}">${esc(i.icon)} ${esc(i.name)} — ${esc(i.mc)}</option>`).join('');
  if (inst) q.value = inst.id;
  renderProfileCard();
  updatePlay();
}

async function renderProfileCard() {
  const acc = selectedAccount();
  const name = acc ? acc.name : 'Player';
  const owner = name.toLowerCase() === OWNER;
  $('#pc-name').innerHTML = `<span class="${owner ? 'owner-name' : 'aqua-name'}">${esc(name)}</span>${owner ? '<span class="crown">👑</span>' : ''}`;
  $('#pc-status').textContent = t('online');
  $('#pc-head').src = acc ? `https://mc-heads.net/avatar/${encodeURIComponent(name)}/128` : 'assets/icons/turtle.png';
  $('#pc-head').onerror = () => { $('#pc-head').src = 'assets/icons/turtle.png'; };
  const lv = await aqua.level().catch(() => ({ level: 1, progress: 0 }));
  const inst = selectedInstance();
  const pvp = inst && inst.loader === 'fabric' && AQUA_MC.includes(inst.mc);
  $('#pc-badges').innerHTML = [
    pvp ? `<span class="pill">⚔️ ${t('pvp_ready')}</span>` : '',
    acc && acc.type === 'microsoft' ? `<span class="pill gold">⚡ ${t('premium')}</span>` : acc ? '<span class="pill aqua">🐢 Aqua</span>' : '',
    `<span class="pill gold">⭐ ${t('level', { n: lv.level })}</span>`,
  ].join('');
  requestAnimationFrame(() => { $('#pc-xp').style.width = Math.round(Math.max(0.04, lv.progress) * 100) + '%'; });
}

/** Правая панель «Онлайн»: друзья из Aqua Network. */
async function renderOnline() {
  const list = $('#op-list');
  if (!list) return;
  const acc = selectedAccount();
  if (!acc || (!acc.aqua && acc.type !== 'microsoft')) { $('#op-count').textContent = ''; list.innerHTML = `<div class="op-empty">${t('op_signin')}</div>`; return; }
  let data;
  try { data = await aqua.friends(); } catch { list.innerHTML = `<div class="op-empty">${t('op_signin')}</div>`; return; }
  const me = { name: data.me, online: true, owner: (data.me || '').toLowerCase() === OWNER, me: true };
  const all = [me, ...data.friends];
  const on = all.filter((f) => f.online).length;
  $('#op-count').textContent = `(${on})`;
  const shown = all.slice(0, 9);
  list.innerHTML = shown.map((f, i) => `<div class="op-item ${f.online ? '' : 'offline'}" style="animation-delay:${i * 40}ms" data-n="${esc(f.name)}">
      <img src="https://mc-heads.net/avatar/${encodeURIComponent(f.name)}/64" alt="">
      <div style="min-width:0"><div class="op-n ${f.owner ? 'owner-name' : ''}">${esc(f.name)}${f.owner ? ' 👑' : ''}</div><small>${f.me ? t('lb_you') : f.online ? (esc(f.status) || t('online_now')) : t('offline_since')}</small></div></div>`).join('')
    + (all.length > shown.length ? `<div class="op-more" id="op-more">＋ ${t('op_more', { n: all.length - shown.length })}</div>` : '')
    + (data.friends.length ? '' : `<div class="op-empty">${t('op_none')}</div>`);
  const more = $('#op-more'); if (more) more.onclick = () => go('friends');
  $$('.op-item', list).forEach((el) => el.onclick = () => go('friends'));
  setBadge(data.incoming.length);
  $('#bell-dot').hidden = !data.incoming.length;
}

async function renderLeaders() {
  const lv = await aqua.level().catch(() => ({ level: 1, progress: 0, hours: 0 }));
  const acc = selectedAccount();
  const name = acc ? acc.name : 'Player';
  $('#lb-me').innerHTML = `<div class="lb-row" style="padding:4px 0"><div class="rank">⭐</div><img src="https://mc-heads.net/avatar/${encodeURIComponent(name)}/64" alt="">
    <div><b>${esc(name)}</b><br><small>${t('lb_hours', { h: lv.hours.toFixed(1) })}</small></div><b>${t('level', { n: lv.level })}</b>
    <div class="pc-xp" style="width:120px;margin:0"><i style="width:${Math.round(lv.progress * 100)}%"></i></div></div>`;
  const box = $('#lb-table');
  box.innerHTML = '<div class="spinner"></div>';
  const data = await aqua.leaderboard().catch(() => null);
  if (!data || !data.players) { box.innerHTML = `<p class="note">${t('lb_offline')}</p>`; return; }
  box.innerHTML = data.players.map((p, i) => `<div class="lb-row glass ${i < 3 ? 'top' + (i + 1) : ''}" style="animation-delay:${i * 30}ms">
    <div class="rank">${i < 3 ? ['🥇', '🥈', '🥉'][i] : i + 1}</div><img src="https://mc-heads.net/avatar/${encodeURIComponent(p.name)}/64" alt="">
    <b class="${p.owner ? 'owner-name' : ''}">${esc(p.name)}${p.owner ? ' 👑' : ''}</b><b>${t('level', { n: p.level })}</b>
    <small><i class="dot ${p.online ? 'on' : ''}"></i> ${p.online ? t('online') : ''}</small></div>`).join('');
}

function updatePlay() {
  const btn = $('#play-btn');
  btn.classList.toggle('running', running);
  btn.classList.toggle('busy', busy && !running);
  $('.play-label', btn).textContent = running ? t('in_game') : busy ? t('launching') : t('play');
}

async function loadNews() {
  const box = $('#news');
  try {
    const items = await aqua.news();
    const all = $('#news-all');
    if (all) all.onclick = (e) => { e.preventDefault(); aqua.external('https://www.minecraft.net/en-us/articles'); };
    box.innerHTML = items.slice(0, 3).map((n, i) => `<div class="news-row" data-ext="https://www.minecraft.net/en-us/updates" style="animation-delay:${i * 80}ms">
      ${n.image ? `<div class="nr-img" style="background-image:url('${esc(n.image)}')"></div>` : ''}
      <div><b>${esc(n.title)}</b><p>${esc(n.text).slice(0, 110)}</p></div><small>${esc(n.date ? new Date(n.date).toLocaleDateString(lang()) : (n.version || ''))}</small></div>`).join('');
  } catch { box.innerHTML = `<p class="note">${t('news_fail')}</p>`; }
}

function renderInstances() {
  const grid = $('#instances-grid');
  grid.innerHTML = S.instances.map((i, n) => `
    <div class="inst-card glass ${i.id === S.selectedInstance ? 'selected' : ''}" data-id="${esc(i.id)}" style="animation-delay:${n * 40}ms">
      <div class="top"><div class="inst-icon">${esc(i.icon)}</div><div><h3>${esc(i.name)}</h3>
      <small>${i.lastPlayed ? t('played', { d: new Date(i.lastPlayed).toLocaleDateString(lang()) }) : t('never')}</small></div></div>
      <div class="badges">${badges(i)}</div>
      <div class="actions">
        <button class="btn primary small" data-act="play">${t('act_play')}</button>
        <button class="btn ghost small" data-act="edit">${t('act_edit')}</button>
        <button class="btn ghost small" data-act="folder">📁</button>
        <button class="btn danger small" data-act="del">🗑</button>
      </div>
    </div>`).join('') + `<div class="new-card" id="new-card"><div><span>＋</span>${t('new_profile_card')}</div></div>`;
  $$('.inst-card', grid).forEach((card) => {
    const id = card.dataset.id;
    const inst = S.instances.find((i) => i.id === id);
    card.addEventListener('click', (e) => {
      const act = e.target.closest('[data-act]')?.dataset.act;
      if (act === 'play') safe(() => aqua.selectInstance(id)).then(() => play(id));
      else if (act === 'edit') instanceModal(inst);
      else if (act === 'folder') safe(() => aqua.open('instance', id));
      else if (act === 'del') confirmBox(t('del_title'), t('del_text', { n: esc(inst.name) })).then((ok) => ok && safe(() => aqua.removeInstance(id, true), t('deleted')));
      else safe(() => aqua.selectInstance(id));
    });
  });
  $('#new-card').onclick = () => instanceModal(null);
}

// ======================================================================= ЗАПУСК
async function play(id, server) {
  if (running || busy) { if (running) go('console'); return; }
  if (!selectedAccount()) { welcome(true); toast(t('add_account_first'), 'err'); return; }
  const inst = id ? S.instances.find((i) => i.id === id) : selectedInstance();
  if (!inst) { instanceModal(null); return; }
  busy = true;
  updatePlay();
  blip(330);
  const r = $('#play-btn').getBoundingClientRect();
  if (window.Ocean) Ocean.burst(r.left + r.width / 2, r.top + r.height / 2, 30);
  if (S.settings.consoleOnLaunch) go('console');
  try { await aqua.launch(inst.id, server); } catch (e) { toast(e.message, 'err'); } finally { busy = false; updatePlay(); }
}

function onProgress(p) {
  const box = $('#progress');
  if (!p) { box.classList.remove('show'); return; }
  box.classList.add('show');
  const pct = p.total ? Math.min(100, Math.round((p.done / p.total) * 100)) : 0;
  $('.bar i', box).style.width = (p.total ? pct : 100) + '%';
  const size = p.total > 1e5 ? ` · ${(p.done / 1048576).toFixed(1)} / ${(p.total / 1048576).toFixed(1)} MB` : p.total > 1 ? ` · ${p.done}/${p.total}` : '';
  $('small', box).textContent = `${p.task}${p.total > 1 ? ' — ' + pct + '%' : ''}${size}`;
}

// ======================================================================= КОНСОЛЬ
let logPending = false;
function addLog(line) {
  logLines.push(line);
  if (logLines.length > 4000) logLines.splice(0, logLines.length - 4000);
  if (!logPending) { logPending = true; requestAnimationFrame(flushLog); }
}
function flushLog() {
  logPending = false;
  const el = $('#console');
  const atBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 60;
  el.innerHTML = logLines.slice(-1500).map((l) => {
    const cls = /\b(ERROR|FATAL|Exception|Caused by)\b/.test(l) ? 'err' : /\bWARN\b/.test(l) ? 'warn' : /^\[(Aqua|Mods|Моды|Gradle)\]/.test(l) ? 'aqua' : '';
    return `<span class="${cls}">${esc(l)}</span>`;
  }).join('\n');
  if (atBottom) el.scrollTop = el.scrollHeight;
}
function scrollConsole() { const el = $('#console'); el.scrollTop = el.scrollHeight; }

// ======================================================================= АККАУНТЫ
function accountsModal() {
  const m = modal(`<div class="modal-head"><h3>${t('accounts')}</h3><button class="x" data-close>✕</button></div>
    <div class="modal-body">
      <div class="acc-list" id="acc-list"></div>
      <button class="ms-btn" id="ms-login"><span class="ms-logo"><i style="background:#f25022"></i><i style="background:#7fba00"></i><i style="background:#00a4ef"></i><i style="background:#ffb900"></i></span><span>${t('ms_login')}</span></button>
      <p class="note">${t('ms_only_note')}</p>
    </div>`, 'narrow');
  const draw = () => {
    $('#acc-list', m.el).innerHTML = S.accounts.length ? S.accounts.map((a) => `
      <div class="acc ${a.id === S.selectedAccount ? 'on' : ''}" data-id="${esc(a.id)}">
        <img src="${avatar(a)}" alt=""><div class="grow"><b class="${isOwner(a) ? 'owner-name' : 'aqua-name'}">${isOwner(a) ? '🐢 ' : ''}${esc(a.name)}</b><small>${a.demo ? 'Microsoft · Demo' : 'Microsoft'}</small></div>
        <button class="btn danger small" data-del="${esc(a.id)}">✕</button></div>`).join('') : `<p class="note">${t('no_accounts')}</p>`;
    $$('.acc', m.el).forEach((el) => el.onclick = (e) => {
      if (e.target.dataset.del) safe(() => aqua.removeAccount(e.target.dataset.del)).then(draw);
      else safe(() => aqua.selectAccount(el.dataset.id)).then(draw);
    });
  };
  draw();
  const unsub = aqua.onState(() => draw());
  $('#ms-login', m.el).onclick = async () => {
    const b = $('#ms-login', m.el), label = b.lastElementChild;
    b.disabled = true; label.textContent = t('ms_wait');
    await safe(() => aqua.loginMicrosoft(), t('signed_in'));
    demoNote();
    b.disabled = false; label.textContent = t('ms_login');
    draw();
  };
  const obs = new MutationObserver(() => { if (!document.body.contains(m.el)) { unsub(); obs.disconnect(); } });
  obs.observe($('#modal-root'), { childList: true });
}

/** Кнопка входа через Microsoft (единственный способ входа). */
function msButton(box, onDone) {
  box.innerHTML = `<button class="ms-btn"><span class="ms-logo"><i style="background:#f25022"></i><i style="background:#7fba00"></i><i style="background:#00a4ef"></i><i style="background:#ffb900"></i></span><span>${t('ms_login')}</span></button>`;
  const b = $('button', box), label = b.lastElementChild;
  b.onclick = async () => {
    b.disabled = true; label.textContent = t('ms_wait');
    const ok = await safe(() => aqua.loginMicrosoft(), t('signed_in'));
    demoNote();
    b.disabled = false; label.textContent = t('ms_login');
    if (ok && onDone) onDone();
  };
}

// ======================================================================= ДРУЗЬЯ
async function renderFriends() {
  const root = $('#friends-root');
  const acc = selectedAccount();
  $('#friends-me').innerHTML = '';
  if (!S.settings.networkUrl) {
    root.innerHTML = `<div class="card glass friends-empty"><div class="big-emoji">🐬</div><p>${t('net_needed')}</p><button class="btn primary" id="fr-settings">${t('to_settings')}</button></div>`;
    $('#fr-settings').onclick = () => go('settings');
    return;
  }
  if (!acc || !acc.aqua) {
    root.innerHTML = `<div class="card glass friends-empty"><div class="big-emoji">🐢</div><h3>${t('aqua_signin_title')}</h3><p>${t(acc && acc.demo ? 'friends_demo' : 'friends_ms')}</p><div id="fr-form"></div></div>`;
    if (!acc || !acc.demo) msButton($('#fr-form'), renderFriends);
    return;
  }
  if (!root.innerHTML.includes('friend-card')) root.innerHTML = '<div class="spinner"></div>';
  let data;
  try { data = await aqua.friends(); } catch (e) {
    root.innerHTML = `<div class="card glass friends-empty"><div class="big-emoji">🦀</div><p>${esc(e.message)}</p></div>`;
    return;
  }
  $('#friends-me').innerHTML = t('friends_me', { n: esc(data.me) });
  setBadge(data.incoming.length);
  const person = (f, actions) => `<div class="friend-card glass ${f.online ? 'online' : ''}" data-n="${esc(f.name)}">
      <div class="friend-ava"><img src="https://mc-heads.net/avatar/${encodeURIComponent(f.name)}/64" alt=""><i class="dot ${f.online ? 'on' : ''}"></i></div>
      <div class="info"><b class="${f.owner ? 'owner-name' : 'aqua-name'}">🐢 ${esc(f.name)}${f.owner ? ' <span class="owner-tag">OWNER</span>' : ''}</b>
        <small>${f.online ? (esc(f.status) || t('online_now')) : t('offline_since')}</small></div>
      <div class="acts">${actions}</div></div>`;
  root.innerHTML = `
    <div class="toolbar glass"><input class="input grow" id="fr-name" maxlength="16" placeholder="${t('friend_add_ph')}"><button class="btn primary" id="fr-add">＋ ${t('friend_add')}</button></div>
    ${data.incoming.length ? `<h3 class="section-title">${t('friend_requests')}</h3><div class="friends-grid">${data.incoming.map((f) => person(f, `<button class="btn primary small" data-a="accept">${t('accept')}</button><button class="btn ghost small" data-a="decline">${t('decline')}</button>`)).join('')}</div>` : ''}
    <h3 class="section-title">${t('friend_list', { n: data.friends.length })}</h3>
    <div class="friends-grid">${data.friends.length ? data.friends.map((f) => person(f, `<button class="btn danger small" data-a="remove">✕</button>`)).join('') : `<p class="note">${t('friend_none')}</p>`}</div>
    ${data.outgoing.length ? `<h3 class="section-title">${t('friend_sent')}</h3><div class="friends-grid">${data.outgoing.map((f) => person(f, `<button class="btn ghost small" data-a="decline">${t('cancel_req')}</button>`)).join('')}</div>` : ''}`;
  const add = async () => {
    const n = $('#fr-name').value.trim();
    if (n && await safe(() => aqua.friendAction('request', n), t('friend_requested'))) renderFriends();
  };
  $('#fr-add').onclick = add;
  $('#fr-name').onkeydown = (e) => { if (e.key === 'Enter') add(); };
  $$('.friend-card [data-a]', root).forEach((b) => b.onclick = async () => {
    const name = b.closest('.friend-card').dataset.n, kind = b.dataset.a;
    const msg = kind === 'accept' ? t('friend_added') : kind === 'remove' ? t('friend_removed') : null;
    if (await safe(() => aqua.friendAction(kind, name), msg)) renderFriends();
  });
}
function setBadge(n) { const b = $('#friends-badge'); b.hidden = !n; b.textContent = n; }

function welcome(show) {
  const w = $('#welcome');
  if (!show) { w.hidden = true; return; }
  w.hidden = false;
  w.innerHTML = `<div class="welcome-box">
    <h1>AQUA LAUNCHER</h1>
    <div class="seg lang-switch"><button data-l="en" class="${lang() === 'en' ? 'on' : ''}">English</button><button data-l="ru" class="${lang() === 'ru' ? 'on' : ''}">Русский</button></div>
    <p>${t('welcome_sub')}</p>
    <div class="login-cards">
      <div class="login-card glass">
        <h3><span class="big-ic" style="background:#fff"><span class="ms-logo"><i style="background:#f25022"></i><i style="background:#7fba00"></i><i style="background:#00a4ef"></i><i style="background:#ffb900"></i></span></span>Microsoft</h3>
        <p>${t('ms_desc')}</p>
        <button class="ms-btn" id="w-ms">${t('ms_login')}</button>
      </div>
      <div class="login-card glass">
        <h3><span class="big-ic">🐢</span>${t('pv_title')}</h3>
        <p>${t('pv_welcome')}</p>
        <button class="btn primary" id="w-preview">${t('pv_open')}</button>
      </div>
    </div>
    <button class="skip" id="w-skip">${t('later')}</button>
  </div>`;
  $$('.lang-switch button', w).forEach((b) => b.onclick = async () => { S.settings.language = b.dataset.l; await aqua.saveSettings(S.settings); renderAll(); welcome(true); });
  $('#w-ms').onclick = async () => {
    const b = $('#w-ms'); b.disabled = true; b.textContent = t('ms_wait');
    const ok = await safe(() => aqua.loginMicrosoft(), t('signed_in'));
    demoNote();
    b.disabled = false; b.textContent = t('ms_login');
    if (ok || S.accounts.length) welcome(false);
  };
  $('#w-preview').onclick = () => { welcome(false); go('preview'); };
  $('#w-skip').onclick = () => welcome(false);
}

// ======================================================================= СБОРКА
async function instanceModal(inst) {
  const d = inst ? { ...inst } : { name: lang() === 'ru' ? 'Моя сборка' : 'My profile', icon: '🐟', mc: '', loader: 'fabric', loaderVersion: 'latest', fpsBoost: true, enderClient: true, ram: null };
  const filter = { release: true, snapshot: !!S.settings.showSnapshots, old: !!S.settings.showOld, q: '' };
  const m = modal(`<div class="modal-head"><h3>${inst ? t('edit_profile') : t('create_profile')}</h3><button class="x" data-close>✕</button></div>
    <div class="modal-body two-col">
      <div>
        <label class="field"><span>${t('name')}</span><input class="input" id="i-name" maxlength="40"></label>
        <div class="field"><span>${t('icon')}</span><div class="icons" id="i-icons"></div></div>
        <div class="field"><span>${t('loader')}</span><div class="seg" id="i-loader">${Object.entries(LOADERS).map(([k, v]) => `<button data-v="${k}">${v}</button>`).join('')}</div></div>
        <label class="field" id="i-lv-wrap"><span>${t('loader_ver')}</span><select class="select" id="i-lv"></select><small class="note" id="i-lv-note"></small></label>
        <label class="check"><input type="checkbox" id="i-fps"><i></i>${t('fps_boost')}</label>
        <label class="check"><input type="checkbox" id="i-ender"><i></i>${t('ender_toggle')}</label>
        <p class="note" id="i-ender-note"></p>
        <label class="field" style="margin-top:12px"><span>${t('ram_for')}</span><input class="input" id="i-ram" type="number" min="1024" step="256" placeholder="${S.settings.ramMax}"></label>
      </div>
      <div>
        <div class="field"><span style="display:flex">${t('mc_version')} <b class="version-count" id="i-count"></b></span>
          <div class="inline" style="margin-bottom:8px">
            <div class="seg" id="i-filter"><button data-f="release">${t('releases')}</button><button data-f="snapshot">${t('snapshots')}</button><button data-f="old">${t('old')}</button></div>
            <input class="input grow" id="i-vq" placeholder="1.21.1">
          </div>
          <div class="version-list" id="i-versions"><div class="spinner"></div></div>
        </div>
      </div>
    </div>
    <div class="modal-foot"><button class="btn ghost" data-close>${t('cancel')}</button><button class="btn primary" id="i-save">${inst ? t('save') : t('create_profile')}</button></div>`);
  const el = m.el;
  $('#i-name', el).value = d.name;
  $('#i-name', el).oninput = (e) => { d.name = e.target.value; };
  $('#i-ram', el).value = d.ram || '';
  $('#i-ram', el).oninput = (e) => { d.ram = e.target.value ? Number(e.target.value) : null; };
  $('#i-fps', el).checked = d.fpsBoost;
  $('#i-fps', el).onchange = (e) => { d.fpsBoost = e.target.checked; };
  $('#i-ender', el).checked = true;
  $('#i-ender', el).disabled = true; // Aqua Client и меню на правый Shift нельзя отключить
  $('#i-icons', el).innerHTML = ICONS.map((ic) => `<button data-ic="${ic}">${ic}</button>`).join('');
  const drawIcons = () => $$('#i-icons button', el).forEach((b) => b.classList.toggle('on', b.dataset.ic === d.icon));
  $$('#i-icons button', el).forEach((b) => b.onclick = () => { d.icon = b.dataset.ic; drawIcons(); });
  drawIcons();

  let lvToken = 0;
  const updateLoader = async () => {
    $$('#i-loader button', el).forEach((b) => b.classList.toggle('on', b.dataset.v === d.loader));
    const enderOk = d.loader === 'fabric' && AQUA_MC.includes(d.mc);
    $('#i-ender', el).checked = true;
    $('#i-ender', el).disabled = true;
    $('#i-ender-note', el).innerHTML = enderOk ? t('ender_ready') : t('ender_only');
    const fix = $('#i-ender-fix', el);
    if (fix) fix.onclick = (e) => { e.preventDefault(); d.loader = 'fabric'; d.mc = '1.21.1'; d.loaderVersion = 'latest'; drawVersions(); updateLoader(); };
    $('#i-fps', el).disabled = d.loader === 'vanilla';
    const wrap = $('#i-lv-wrap', el), sel = $('#i-lv', el), note = $('#i-lv-note', el);
    wrap.style.display = d.loader === 'vanilla' ? 'none' : '';
    if (d.loader === 'vanilla' || !d.mc) { sel.innerHTML = ''; note.textContent = d.mc ? '' : t('pick_version_first'); return; }
    const token = ++lvToken;
    sel.innerHTML = `<option>${t('loading')}</option>`;
    note.textContent = '';
    try {
      const list = await aqua.loaderVersions(d.loader, d.mc);
      if (token !== lvToken) return;
      if (!list.length) { sel.innerHTML = ''; note.innerHTML = `<span class="warn">${esc(t('not_supported', { l: LOADERS[d.loader], v: d.mc }))}</span>`; return; }
      sel.innerHTML = `<option value="latest">${t('latest_stable')}</option>` + list.slice(0, 150).map((v) => `<option value="${esc(v.id)}">${esc(v.label || v.id)}${v.stable ? '' : ' (beta)'}</option>`).join('');
      sel.value = list.some((v) => v.id === d.loaderVersion) ? d.loaderVersion : 'latest';
      d.loaderVersion = sel.value;
    } catch (e) { if (token === lvToken) { sel.innerHTML = ''; note.textContent = e.message; } }
  };
  $('#i-lv', el).onchange = (e) => { d.loaderVersion = e.target.value; };
  $$('#i-loader button', el).forEach((b) => b.onclick = () => { d.loader = b.dataset.v; d.loaderVersion = 'latest'; updateLoader(); });

  const drawVersions = () => {
    $$('#i-filter button', el).forEach((b) => b.classList.toggle('on', !!filter[b.dataset.f]));
    if (!VERSIONS) return;
    const q = filter.q.trim();
    const list = VERSIONS.versions.filter((v) => (v.type === 'release' && filter.release) || (v.type === 'snapshot' && filter.snapshot)
      || ((v.type === 'old_beta' || v.type === 'old_alpha') && filter.old)).filter((v) => !q || v.id.includes(q));
    $('#i-count', el).textContent = t('count', { a: list.length, b: VERSIONS.versions.length });
    const box = $('#i-versions', el);
    box.innerHTML = list.map((v) => `<button data-v="${esc(v.id)}" class="${v.id === d.mc ? 'on' : ''}"><span>${esc(v.id)}</span>
      <small>${v.type === 'release' ? '' : v.type === 'snapshot' ? t('snapshot') : t('oldv')}${new Date(v.releaseTime).getFullYear()}</small></button>`).join('') || `<p class="empty">${t('nothing')}</p>`;
    $$('button', box).forEach((b) => b.onclick = () => { d.mc = b.dataset.v; $$('button', box).forEach((x) => x.classList.toggle('on', x === b)); d.loaderVersion = 'latest'; updateLoader(); });
    const on = $('button.on', box);
    if (on) on.scrollIntoView({ block: 'center' });
  };
  $$('#i-filter button', el).forEach((b) => b.onclick = () => { filter[b.dataset.f] = !filter[b.dataset.f]; drawVersions(); });
  $('#i-vq', el).oninput = (e) => { filter.q = e.target.value; drawVersions(); };
  try {
    if (!VERSIONS) VERSIONS = await aqua.versions();
    if (!d.mc) d.mc = '1.21.1';
    drawVersions();
  } catch (e) { $('#i-versions', el).innerHTML = `<p class="empty">${esc(e.message)}</p>`; }
  updateLoader();
  $('#i-save', el).onclick = async () => {
    if (!d.mc) return toast(t('choose_version'), 'err');
    const ok = await safe(() => aqua.saveInstance(d), inst ? t('saved') : t('created'));
    if (ok) m.close();
  };
}

// ======================================================================= СЕРВЕРЫ
const pingCache = {};
function renderServers(refresh) {
  const box = $('#servers-list');
  const list = S.servers || [];
  const acc = selectedAccount();
  $('#srv-ms-warn').hidden = !acc || acc.type === 'microsoft';
  box.innerHTML = list.length ? list.map((sv, n) => {
    const p = pingCache[sv.address];
    const state = !p ? '' : p.error ? 'off' : 'on';
    return `<div class="srv glass" style="animation-delay:${n * 40}ms" data-addr="${esc(sv.address)}">
      ${p && p.favicon ? `<img src="${esc(p.favicon)}" alt="">` : '<div class="noicon">🌊</div>'}
      <div class="info"><h4><i class="dot ${state}"></i>${esc(sv.name)} <span class="addr">${esc(sv.address)}</span></h4>
        <div class="motd">${p ? (p.error ? t('srv_offline') : esc(p.motd || '')) : t('checking')}</div>
        <div class="meta">${p && !p.error ? `👥 ${p.players ? fmt(p.players.online) + ' / ' + fmt(p.players.max) : '?'} · 📶 ${p.ping} ms · ${esc(p.version)}` : ''}</div></div>
      <div class="acts"><button class="btn primary small" data-join>${t('act_play')}</button><button class="btn danger small" data-del>${t('remove')}</button></div></div>`;
  }).join('') : `<div class="empty"><span class="fish">🐬</span>${t('add_server_hint')}</div>`;
  $$('.srv', box).forEach((row) => {
    const addr = row.dataset.addr;
    $('[data-join]', row).onclick = () => { go('home'); play(null, addr.includes(':') ? addr : addr + ':25565'); };
    $('[data-del]', row).onclick = () => safe(() => aqua.saveServers(S.servers.filter((x) => x.address !== addr)));
  });
  if (refresh) list.forEach((sv) => {
    aqua.pingServer(sv.address).then((r) => { pingCache[sv.address] = r; }, (e) => { pingCache[sv.address] = { error: e.message }; })
      .finally(() => { if ($('#page-servers').classList.contains('active')) renderServers(false); });
  });
}

// ======================================================================= МОДЫ
const MODS = { tab: 'browse', source: 'modrinth', type: 'mod', q: '', offset: 0, total: 0, token: 0 };
function renderModsSelect() {
  const sel = $('#mods-instance');
  const cur = sel.value || S.selectedInstance;
  sel.innerHTML = S.instances.map((i) => `<option value="${esc(i.id)}">${esc(i.icon)} ${esc(i.name)} — ${esc(i.mc)} ${LOADERS[i.loader]}</option>`).join('');
  if (S.instances.some((i) => i.id === cur)) sel.value = cur;
}
function segBind(id, key) {
  $$(`#${id} button`).forEach((b) => b.onclick = () => {
    MODS[key] = b.dataset.v;
    $$(`#${id} button`).forEach((x) => x.classList.toggle('on', x === b));
    loadMods(true);
  });
}
async function loadMods(reset) {
  const list = $('#mods-list'), more = $('#mods-more');
  const instId = $('#mods-instance').value;
  if (!instId) { list.innerHTML = `<p class="empty">${t('create_profile_first')}</p>`; return; }
  MODS.source = 'modrinth'; // CurseForge убран: без ключа API он не работает, все моды берём с Modrinth
  $('#mods-search').style.display = MODS.tab === 'browse' ? '' : 'none';
  if (reset) { MODS.offset = 0; list.innerHTML = '<div class="spinner"></div>'; }
  const token = ++MODS.token;
  if (MODS.tab === 'installed') {
    more.hidden = true;
    try {
      const items = await aqua.installedMods(instId, MODS.type);
      if (token !== MODS.token) return;
      list.innerHTML = items.length ? items.map((m, n) => `
        <div class="mod glass ${m.enabled ? '' : 'disabled'}" style="animation-delay:${Math.min(n, 12) * 30}ms" data-file="${esc(m.file)}">
          ${m.icon ? `<img src="${esc(m.icon)}" alt="">` : '<div class="noicon">📦</div>'}
          <div class="info"><h4>${esc(m.title)}<small>${esc(m.version)}</small></h4><p>${esc(m.file)}</p></div>
          <label class="check" style="margin:0"><input type="checkbox" ${m.enabled ? 'checked' : ''} data-toggle><i></i></label>
          <button class="btn danger small" data-remove>🗑</button></div>`).join('')
        : `<div class="empty"><span class="fish">🐠</span>${t('empty_installed')}</div>`;
      $$('.mod', list).forEach((row) => {
        const file = row.dataset.file;
        $('[data-toggle]', row).onchange = () => safe(() => aqua.toggleMod(instId, file, MODS.type)).then(() => loadMods(false));
        $('[data-remove]', row).onclick = () => safe(() => aqua.removeMod(instId, file, MODS.type), t('removed')).then(() => loadMods(false));
      });
    } catch (e) { list.innerHTML = `<p class="empty">${esc(e.message)}</p>`; }
    return;
  }
  try {
    const res = await aqua.searchMods({ source: MODS.source, type: MODS.type, query: MODS.q, offset: MODS.offset, instanceId: instId });
    if (token !== MODS.token) return;
    MODS.total = res.total;
    const html = res.hits.map((h, n) => `
      <div class="mod glass" style="animation-delay:${Math.min(n, 12) * 30}ms" data-id="${esc(h.id)}">
        ${h.icon ? `<img src="${esc(h.icon)}" alt="" loading="lazy">` : '<div class="noicon">🧩</div>'}
        <div class="info"><h4>${esc(h.title)}<small>${t('by')} ${esc(h.author)}</small></h4><p>${esc(h.description)}</p>
          <div class="stats">⬇ ${fmt(h.downloads)}${h.follows ? ` · ♥ ${fmt(h.follows)}` : ''} · <a href="#" data-ext="${esc(h.url)}">${t('page')}</a></div></div>
        <button class="btn ${h.installed ? 'ghost' : 'primary'} small" data-install ${h.installed ? 'disabled' : ''}>${h.installed ? t('installed_ok') : t('install')}</button>
      </div>`).join('');
    if (reset) list.innerHTML = html || `<div class="empty"><span class="fish">🐡</span>${t('nothing_for_version')}</div>`;
    else list.insertAdjacentHTML('beforeend', html);
    more.hidden = MODS.offset + 20 >= MODS.total;
    $$('.mod [data-install]', list).forEach((b) => b.onclick = async () => {
      const id = b.closest('.mod').dataset.id;
      b.disabled = true; b.textContent = t('installing');
      const r = await safe(() => aqua.installMod({ source: MODS.source, projectId: id, instanceId: instId, type: MODS.type }));
      if (r) { b.textContent = t('installed_ok'); b.className = 'btn ghost small'; toast(Array.isArray(r) && r.length > 1 ? t('installed_list', { n: r.join(', ') }) : t('installed_ok'), 'ok'); }
      else { b.disabled = false; b.textContent = t('install'); }
    });
  } catch (e) {
    if (token === MODS.token) { list.innerHTML = `<div class="empty"><span class="fish">🦀</span>${esc(e.message)}</div>`; more.hidden = true; }
  }
}

// ======================================================================= СКИН (2D-превью)
function drawSkin(canvas, img, slim, overlay = true) {
  const g = canvas.getContext('2d');
  g.imageSmoothingEnabled = false;
  g.clearRect(0, 0, canvas.width, canvas.height);
  if (!img) return;
  const s = canvas.width / 16; // ширина фигуры 16 пикселей
  const legacy = img.height === 32;
  const part = (sx, sy, w, h, dx, dy, flip) => {
    if (flip) { g.save(); g.translate((dx + w) * s, dy * s); g.scale(-1, 1); g.drawImage(img, sx, sy, w, h, 0, 0, w * s, h * s); g.restore(); }
    else g.drawImage(img, sx, sy, w, h, dx * s, dy * s, w * s, h * s);
  };
  const aw = slim ? 3 : 4;
  const oy = 4;
  part(8, 8, 8, 8, 4, oy);                       // голова
  part(20, 20, 8, 12, 4, oy + 8);                // тело
  part(44, 20, aw, 12, 4 - aw, oy + 8);          // правая рука
  if (legacy) part(44, 20, aw, 12, 12, oy + 8, true); else part(36, 52, aw, 12, 12, oy + 8);
  part(4, 20, 4, 12, 4, oy + 20);                // правая нога
  if (legacy) part(4, 20, 4, 12, 8, oy + 20, true); else part(20, 52, 4, 12, 8, oy + 20);
  if (overlay) {
    part(40, 8, 8, 8, 4, oy);
    if (!legacy) {
      part(20, 36, 8, 12, 4, oy + 8); part(44, 36, aw, 12, 4 - aw, oy + 8); part(52, 52, aw, 12, 12, oy + 8);
      part(4, 36, 4, 12, 4, oy + 20); part(4, 52, 4, 12, 8, oy + 20);
    }
  }
}
function loadImg(src) { return new Promise((ok, fail) => { const i = new Image(); i.onload = () => ok(i); i.onerror = fail; i.src = src; }); }
let SKIN = { data: null, model: 'classic', has: false, pending: null };

async function renderSkins() {
  const sk = await aqua.getSkin().catch(() => ({ has: false, model: 'classic' }));
  SKIN = { ...sk, pending: null };
  await drawSkinPage();
}
async function drawSkinPage() {
  const data = SKIN.pending || SKIN.data;
  $('#skin-empty').hidden = !!data;
  $$('#skin-model button').forEach((b) => b.classList.toggle('on', b.dataset.v === SKIN.model));
  const acc = selectedAccount();
  $('#skin-mojang').disabled = !acc || acc.type !== 'microsoft' || !SKIN.has;
  const img = data ? await loadImg('data:image/png;base64,' + data).catch(() => null) : null;
  drawSkin($('#skin-canvas'), img, SKIN.model === 'slim');
}

// ======================================================================= ГАРДЕРОБ
let lockerSlot = 'cape', lockerQuery = '', lockerObs = null;
let VIEWER = null;
async function lockerSkin() {
  const sk = SKIN.data ? SKIN : await aqua.getSkin().catch(() => ({}));
  const acc = selectedAccount();
  const img = sk.data ? await loadImg('data:image/png;base64,' + sk.data).catch(() => null)
    : acc ? await loadImg(`https://mc-heads.net/skin/${encodeURIComponent(acc.name)}`).catch(() => null) : null;
  return { img: img || await loadImg('assets/default-skin.png').catch(() => null), slim: sk.model === 'slim' };
}
async function renderLocker() {
  const L = S.loadout || {};
  $('#locker-slots').innerHTML = COSMETICS.slots.map((s) => `<button data-s="${s}" class="${s === lockerSlot ? 'on' : ''}">${t('slot_' + s)}<i>${COSMETICS.items[s].length - 1}</i></button>`).join('');
  $$('#locker-slots button').forEach((b) => b.onclick = () => { lockerSlot = b.dataset.s; blip(480); renderLocker(); });
  const q = (lockerQuery || '').trim().toLowerCase();
  const items = COSMETICS.items[lockerSlot].filter(([id, , en, ru]) => !q || id === 'none' || en.toLowerCase().includes(q) || ru.toLowerCase().includes(q));
  const is3d = ['hat', 'face', 'wings', 'back', 'pet'].includes(lockerSlot);
  const search = $('#locker-search');
  if (search && !search._wired) { search._wired = true; let tm; search.oninput = () => { clearTimeout(tm); tm = setTimeout(() => { lockerQuery = search.value; renderLocker(); }, 140); }; }
  $('#locker-grid').innerHTML = items.map(([id, emoji], n) => {
    const on = (L[lockerSlot] || 'none') === id;
    let vis = `<div class="cos-emoji">${emoji}</div>`;
    if (id !== 'none') {
      if (lockerSlot === 'cape') vis = `<div class="cape-thumb" style="background-image:url('assets/capes/${id === 'portal' ? 'portal_0' : id}.png')"></div>`;
      else if (lockerSlot === 'icon') vis = `<img class="icon-thumb" src="assets/icons/${id}.png" alt="">`;
      else if (is3d) vis = `<canvas class="cos-3d" width="84" height="64" data-id="${id}"></canvas>`;
    }
    return `<button class="cos-card glass ${on ? 'on' : ''}" data-id="${id}" style="animation-delay:${Math.min(n, 20) * 22}ms">${vis}<b>${esc(cosName(lockerSlot, id))}</b><small>${on ? '✓ ' + t('equipped') : t('equip')}</small></button>`;
  }).join('');
  if (is3d && window.AquaViewer) {
    // миниатюры рисуем лениво — только видимые карточки (в слоте 120+ вещей)
    if (lockerObs) lockerObs.disconnect();
    const slot = lockerSlot;
    lockerObs = new IntersectionObserver((ents) => ents.forEach((en) => {
      if (!en.isIntersecting) return;
      lockerObs.unobserve(en.target); AquaViewer.thumb(en.target, slot, en.target.dataset.id);
    }), { root: $('#locker-grid'), rootMargin: '200px' });
    $$('#locker-grid canvas.cos-3d').forEach((c) => lockerObs.observe(c));
  }
  if (!items.length || (items.length === 1 && q)) $('#locker-grid').insertAdjacentHTML('beforeend', `<p class="note">${t('locker_nothing')}</p>`);
  $$('#locker-grid .cos-card').forEach((b) => {
    b.onmouseenter = () => { if (VIEWER) VIEWER.set({ loadout: { ...S.loadout, [lockerSlot]: b.dataset.id }, name: VIEWER.opts.name, owner: VIEWER.opts.owner }); };
    b.onmouseleave = () => { if (VIEWER) VIEWER.set({ loadout: { ...S.loadout }, name: VIEWER.opts.name, owner: VIEWER.opts.owner }); };
    b.onclick = async (e) => {
      const next = { ...S.loadout, [lockerSlot]: b.dataset.id };
      S.loadout = next;
      blip(620);
      sparkle(e.clientX, e.clientY);
      $$('#locker-grid .cos-card').forEach((x) => { const on = x === b; x.classList.toggle('on', on); x.querySelector('small').textContent = on ? '✓ ' + t('equipped') : t('equip'); });
      if (VIEWER) VIEWER.set({ loadout: next });
      updateEquipped();
      await safe(() => aqua.saveLocker(next));
      toast(t('locker_saved'), 'ok');
    };
  });
  updateEquipped();
  const canvas = $('#locker-3d');
  if (!VIEWER || VIEWER.canvas !== canvas) VIEWER = window.AquaViewer ? new AquaViewer.Viewer(canvas) : null;
  if (VIEWER) {
    const { img, slim } = await lockerSkin();
    const name = selectedAccount()?.name || 'Player';
    VIEWER.set({ skin: img, slim, loadout: { ...S.loadout }, name, owner: name.toLowerCase() === OWNER });
  }
}
function updateEquipped() {
  const L = S.loadout || {};
  $('#locker-equipped').innerHTML = COSMETICS.slots.filter((s) => L[s] && L[s] !== 'none')
    .map((s) => `<span class="badge pop">${cosEmoji(s, L[s])} ${esc(cosName(s, L[s]))}</span>`).join('');
}
/** Маленький «всплеск» пузырьков в точке клика. */
function sparkle(x, y) {
  if (document.body.classList.contains('reduce-motion')) return;
  for (let i = 0; i < 10; i++) {
    const d = document.createElement('i');
    d.className = 'spark';
    const a = (i / 10) * Math.PI * 2, r = 26 + Math.random() * 22;
    d.style.left = x + 'px'; d.style.top = y + 'px';
    d.style.setProperty('--dx', Math.cos(a) * r + 'px'); d.style.setProperty('--dy', Math.sin(a) * r + 'px');
    document.body.appendChild(d);
    setTimeout(() => d.remove(), 700);
  }
}

// ======================================================================= НАСТРОЙКИ
function renderSettings() {
  const st = S.settings;
  const opt = (v, label, cur) => `<option value="${esc(v)}" ${String(cur) === String(v) ? 'selected' : ''}>${esc(label)}</option>`;
  const sel = (key, options) => `<select class="select" data-set="${key}">${options.map(([v, l]) => opt(v, l, st[key])).join('')}</select>`;
  const chk = (key, label) => `<label class="check"><input type="checkbox" data-set="${key}" ${st[key] ? 'checked' : ''}><i></i>${label}</label>`;
  const row = (label, control) => `<div class="set-row"><span>${label}</span>${control}</div>`;
  const maxRam = Math.max(2048, Math.floor((INFO.totalMem * 0.85) / 256) * 256);
  const servers = [['', t('none')], ...(S.servers || []).map((s) => [s.address, `${s.name} (${s.address})`])];
  $('#settings-root').innerHTML = `
    <div class="card glass"><h4>${t('g_general')}</h4>
      ${row(t('s_language'), sel('language', [['en', 'English'], ['ru', 'Русский']]))}
      ${row(t('s_theme'), sel('theme', Object.keys(THEMES).map((k) => [k, t('theme_' + k)])))}
      ${row(t('s_zoom'), sel('uiZoom', [[0.9, '90%'], [1, '100%'], [1.1, '110%'], [1.25, '125%']]))}
      ${row(t('s_bg'), sel('bgQuality', ['high', 'medium', 'low', 'off'].map((k) => [k, t('bg_' + k)])))}
      ${chk('reduceMotion', t('s_motion'))}${chk('uiSounds', t('s_sounds'))}${chk('startWithSystem', t('s_startup'))}
      ${chk('hardwareAcceleration', t('s_hw'))}${chk('confirmDelete', t('s_confirm'))}${chk('autoUpdate', t('s_autoupdate'))}
      ${row(t('version_label') + ' ' + INFO.version, `<button class="btn" id="check-upd">${t('s_checkupd')}</button>`)}
    </div>
    <div class="card glass"><h4>${t('g_launch')}</h4>
      ${row(t('s_after'), sel('afterLaunch', [['keep', t('after_keep')], ['minimize', t('after_min')], ['hide', t('after_hide')]]))}
      ${row(t('s_autojoin'), sel('autoJoin', servers))}
      ${row(t('s_priority'), sel('priority', [['normal', t('pr_normal')], ['above', t('pr_above')], ['high', t('pr_high')]]))}
      ${chk('reopenAfterExit', t('s_reopen'))}${chk('consoleOnLaunch', t('s_console'))}
      ${row(t('s_threads'), sel('downloadThreads', [[8, '8'], [16, '16'], [32, '32'], [64, '64']]))}
      ${chk('showSnapshots', t('s_snap'))}${chk('showOld', t('s_old'))}
    </div>
    <div class="card glass"><h4>${t('g_game')}</h4>
      ${row(t('s_glang'), sel('gameLanguage', [['auto', t('auto')], ['en_us', 'English'], ['ru_ru', 'Русский'], ['uk_ua', 'Українська'], ['de_de', 'Deutsch'], ['es_es', 'Español']]))}
      ${row(t('s_gui'), sel('guiScale', [[0, t('auto')], [1, '1'], [2, '2'], [3, '3'], [4, '4']]))}
      ${row(t('s_fps'), sel('maxFps', [[60, '60'], [120, '120'], [144, '144'], [165, '165'], [240, '240'], [260, '∞']]))}
      ${row(t('s_rd'), sel('renderDistance', [[6, '6'], [8, '8'], [10, '10'], [12, '12'], [16, '16'], [20, '20'], [24, '24'], [32, '32']]))}
      ${row(t('s_window'), `<div class="inline"><input class="input small" type="number" min="640" data-set="width" value="${st.width}"> × <input class="input small" type="number" min="480" data-set="height" value="${st.height}"></div>`)}
      ${chk('fullscreen', t('s_fullscreen'))}
    </div>
    <div class="card glass"><h4>${t('g_java')}</h4>
      <label class="row"><span>${t('s_ram')} <b id="ram-val"></b></span><input type="range" data-set="ramMax" min="1024" max="${maxRam}" step="256" value="${st.ramMax}"></label>
      <label class="row"><span>${t('s_ram_min')} <b id="ram-min-val"></b></span><input type="range" data-set="ramMin" min="512" max="${Math.min(maxRam, 8192)}" step="256" value="${st.ramMin}"></label>
      <p class="hint">${t('ram_hint', { t: (INFO.totalMem / 1024).toFixed(1) })}</p>
      ${row(t('s_preset'), sel('jvmPreset', [['performance', t('preset_perf')], ['balanced', t('preset_bal')], ['zgc', t('preset_zgc')], ['off', t('preset_off')]]))}
      <label class="row col"><span>${t('s_java')}</span><div class="inline"><input class="input grow" data-set="javaPath" value="${esc(st.javaPath)}" placeholder="${t('java_ph')}"><button class="btn ghost" id="pick-java">${t('pick')}</button></div></label>
      <label class="row col"><span>${t('s_jvm')}</span><input class="input" data-set="jvmArgs" value="${esc(st.jvmArgs)}" placeholder="-XX:+UseZGC …"></label>
    </div>
    <div class="card glass"><h4>${t('g_network')}</h4>
      <label class="row col"><span>${t('s_net')}</span><input class="input" data-set="networkUrl" value="${esc(st.networkUrl)}" placeholder="https://aqua-network.mruliangg.workers.dev"></label>
      <p class="hint">${t('net_hint')}</p>
      ${chk('shareSkin', t('s_share'))}
    </div>
    <div class="card glass"><h4>${t('s_ender')}</h4>
      <p class="hint" id="ender-status">…</p>
      <div class="inline" style="margin-top:10px"><button class="btn ghost" id="aqua-build">${t('build_now')}</button></div>
    </div>
    <div class="card glass"><h4>${t('g_data')}</h4>
      <div class="inline"><button class="btn ghost" id="open-root">${t('open_folder')}</button><button class="btn ghost" id="clear-cache">${t('clear_cache')}</button><button class="btn ghost" id="restart-app">${t('restart_now')}</button></div>
      <p class="hint">${esc(INFO.root)} · v${esc(INFO.version)}</p>
    </div>`;

  const root = $('#settings-root');
  const showRam = () => {
    $('#ram-val').textContent = `${(st.ramMax / 1024).toFixed(1)} GB`;
    $('#ram-min-val').textContent = `${(st.ramMin / 1024).toFixed(1)} GB`;
    $$('input[type=range]', root).forEach((r) => r.style.setProperty('--p', ((r.value - r.min) / (r.max - r.min)) * 100 + '%'));
  };
  showRam();
  $$('[data-set]', root).forEach((el) => {
    const key = el.dataset.set;
    const handler = () => {
      let v = el.type === 'checkbox' ? el.checked : el.value;
      if (['uiZoom', 'guiScale', 'maxFps', 'renderDistance', 'downloadThreads', 'ramMax', 'ramMin', 'width', 'height'].includes(key)) v = Number(v);
      if (key === 'ramMin' && v > st.ramMax) v = st.ramMax;
      const needsRestart = key === 'hardwareAcceleration';
      saveSetting({ [key]: v });
      if (key === 'language') renderAll();
      if (key === 'ramMax' || key === 'ramMin') showRam();
      if (needsRestart) toast(t('restart_needed'));
    };
    el.addEventListener(el.type === 'range' || el.tagName === 'INPUT' && el.type !== 'checkbox' ? 'input' : 'change', handler);
  });
  $('#pick-java').onclick = async () => { const p = await safe(() => aqua.pickJava()); if (typeof p === 'string') { saveSetting({ javaPath: p }); renderSettings(); } };
  $('#open-root').onclick = () => safe(() => aqua.open('root'));
  $('#clear-cache').onclick = () => safe(() => aqua.clearCache(), t('cache_cleared'));
  $('#restart-app').onclick = () => aqua.restart();
  $('#aqua-build').onclick = async () => {
    const b = $('#aqua-build'); b.disabled = true; b.textContent = t('building');
    await safe(() => aqua.buildClient(), t('built'));
    b.disabled = false; b.textContent = t('build_now');
    refreshClient();
  };
  refreshClient();
}

let saveTimer;
function saveSetting(patch) {
  Object.assign(S.settings, patch);
  clearTimeout(saveTimer);
  saveTimer = setTimeout(() => aqua.saveSettings(S.settings).catch((e) => toast(e.message, 'err')), 300);
  applyLook();
}

async function refreshClient() {
  const st = await aqua.clientStatus().catch(() => ({ ready: false }));
  const el = $('#ender-status');
  if (el) el.innerHTML = st.ready ? t('ender_ok', { s: esc(st.source) }) : t('ender_wait');
}

// ======================================================================= СТАРТ
function drawTurtles() {
  const draw = (id, scale) => {
    const c = document.getElementById(id);
    if (!c || !window.Ocean) return;
    const g = c.getContext('2d');
    g.imageSmoothingEnabled = false;
    g.clearRect(0, 0, c.width, c.height);
    const spr = Ocean.sprite('turtle', 0, scale, true);
    g.drawImage(spr, Math.round((c.width - spr.width) / 2), Math.round((c.height - spr.height) / 2));
  };
  draw('logo-turtle', 3);
  draw('hero-turtle', 10);
}
function playBubbles() {
  $('.play-bubbles').innerHTML = Array.from({ length: 12 }, () => {
    const s = 4 + Math.random() * 8;
    return `<i style="left:${Math.random() * 100}%;width:${s}px;height:${s}px;animation-duration:${2 + Math.random() * 3}s;animation-delay:${-Math.random() * 4}s"></i>`;
  }).join('');
}

async function init() {
  INFO = await aqua.info();
  if (INFO.aquaVersions && INFO.aquaVersions.length) AQUA_MC = [...new Set([...AQUA_MC, ...INFO.aquaVersions])];
  S = await aqua.getState();
  applyI18n();

  $$('[data-win]').forEach((b) => b.onclick = () => aqua.win(b.dataset.win));
  $$('.nav').forEach((b) => b.onclick = () => go(b.dataset.page));
  $('#account-chip').onclick = accountsModal;
  $('#play-btn').onclick = () => play();
  $('#current-instance').onclick = () => go('instances');
  $('#quick-instance').onchange = (e) => safe(() => aqua.selectInstance(e.target.value));
  $('#new-instance').onclick = () => instanceModal(null);
  // волна от нажатия на кнопки и лёгкий 3D-наклон карточек
  document.addEventListener('pointerdown', (e) => {
    const b = e.target.closest('.btn, .play, .nav, .cos-card, .seg button');
    if (!b || S.settings.reduceMotion) return;
    const r = b.getBoundingClientRect(), d = Math.max(r.width, r.height);
    const w = document.createElement('span');
    w.className = 'ripple';
    w.style.cssText = `width:${d}px;height:${d}px;left:${e.clientX - r.left - d / 2}px;top:${e.clientY - r.top - d / 2}px`;
    b.appendChild(w);
    setTimeout(() => w.remove(), 600);
  });
  document.addEventListener('pointermove', (e) => {
    const c = e.target.closest('.inst-card, .tile, .news-card, .friend-card, .srv');
    $$('.tilt').forEach((x) => { if (x !== c) { x.classList.remove('tilt'); x.style.transform = ''; } });
    if (!c || S.settings.reduceMotion) return;
    const r = c.getBoundingClientRect();
    const rx = ((e.clientY - r.top) / r.height - 0.5) * -6, ry = ((e.clientX - r.left) / r.width - 0.5) * 6;
    c.classList.add('tilt');
    c.style.transform = `perspective(700px) rotateX(${rx}deg) rotateY(${ry}deg) translateY(-3px)`;
  });
  document.addEventListener('click', (e) => {
    const ext = e.target.closest('[data-ext]');
    if (ext) { e.preventDefault(); aqua.external(ext.dataset.ext); }
    if (e.target.closest('button')) blip(560);
  });

  segBind('mods-tab', 'tab');
  segBind('mods-source', 'source');
  segBind('mods-type', 'type');
  $('#mods-instance').onchange = () => loadMods(true);
  let qTimer;
  $('#mods-search').oninput = (e) => { clearTimeout(qTimer); qTimer = setTimeout(() => { MODS.q = e.target.value.trim(); loadMods(true); }, 350); };
  $('#mods-more').onclick = () => { MODS.offset += 20; loadMods(false); };

  $('#servers-refresh').onclick = () => renderServers(true);
  const addServer = () => {
    const address = $('#srv-addr').value.trim();
    if (!address) return toast(t('enter_address'), 'err');
    const name = $('#srv-name').value.trim() || address;
    safe(() => aqua.saveServers([...(S.servers || []), { name, address }]), t('server_added')).then(() => { $('#srv-addr').value = ''; $('#srv-name').value = ''; renderServers(true); });
  };
  $('#srv-add').onclick = addServer;
  $('#srv-addr').onkeydown = (e) => { if (e.key === 'Enter') addServer(); };

  // скины
  $('#skin-file').onchange = (e) => {
    const f = e.target.files[0];
    if (!f) return;
    const r = new FileReader();
    r.onload = () => { SKIN.pending = String(r.result).split(',')[1]; drawSkinPage(); };
    r.readAsDataURL(f);
    e.target.value = '';
  };
  $('#skin-get').onclick = async () => { const b64 = await safe(() => aqua.skinFromName($('#skin-nick').value.trim())); if (typeof b64 === 'string') { SKIN.pending = b64; drawSkinPage(); } };
  $$('#skin-model button').forEach((b) => b.onclick = async () => { SKIN.model = b.dataset.v; if (SKIN.has && !SKIN.pending) await safe(() => aqua.setSkinModel(SKIN.model)); drawSkinPage(); });
  $('#skin-save').onclick = async () => {
    const data = SKIN.pending || SKIN.data;
    if (!data) return;
    if (await safe(() => aqua.setSkin(data, SKIN.model), t('skin_saved'))) renderSkins();
  };
  $('#skin-mojang').onclick = () => safe(() => aqua.uploadSkinMojang(), t('skin_uploaded'));
  $('#skin-reset').onclick = async () => { if (await safe(() => aqua.clearSkin())) renderSkins(); };

  $('#log-clear').onclick = () => { logLines.length = 0; flushLog(); };
  $('#log-copy').onclick = () => { navigator.clipboard.writeText(logLines.join('\n')); toast(t('log_copied'), 'ok'); };
  $('#kill-btn').onclick = () => safe(() => aqua.kill());

  aqua.onState((s) => {
    S = s;
    renderAll();
    if ($('#page-friends').classList.contains('active')) renderFriends();
    if (!$('#welcome').hidden && S.accounts.length) welcome(false);
    if ($('#page-servers').classList.contains('active')) renderServers(false);
  });
  aqua.onProgress(onProgress);
  aqua.onIncoming((n) => { setBadge(n); if (n) toast(t('new_requests', { n }), 'ok'); });
  aqua.onLog(addLog);
  aqua.onStarted(() => toast(t('started'), 'ok'));
  aqua.onGameState((st) => {
    running = !!st.running;
    if (!st.running && st.code && st.code !== 0) toast(t('exit_code', { c: st.code }), 'err');
    updatePlay();
  });

  playBubbles();
  drawTurtles();
  // новый дизайн: анимированные рисунки, кнопки шапки, правая панель
  if (window.AquaArt) {
    AquaArt.crown($('#hero-crown'));
    AquaArt.squid($('#hero-squid'));
    $$('.feat-art').forEach((c) => AquaArt.card(c, c.dataset.art));
  }
  $$('.feat').forEach((f) => f.onclick = () => go(f.dataset.go));
  $('#gear-btn').onclick = () => go('settings');
  $('#bell-btn').onclick = async () => {
    const n = Number($('#friends-badge').textContent) || 0;
    if (n) { go('friends'); return; }
    if (UPD && ['ready', 'available'].includes(UPD.status)) { renderUpdate(); toast(t(UPD.status === 'ready' ? 'upd_ready' : 'upd_avail', { v: UPD.version })); return; }
    toast(t('notif_none'));
  };
  $('#af-ver').textContent = 'v' + INFO.version;
  renderOnline();
  setInterval(renderOnline, 30000);
  renderAll();
  aqua.versions().then((v) => { VERSIONS = v; renderHome(); }).catch(() => {});
  loadNews();
  setInterval(() => { if ($('#page-friends').classList.contains('active')) renderFriends(); }, 20000);
  if (!S.accounts.length) setTimeout(() => welcome(true), 400);
}

init().catch((e) => toast('UI: ' + e.message, 'err'));

// Светящееся пятно под курсором на стеклянных карточках
document.addEventListener('pointermove', (e) => {
  const el = e.target.closest?.('.card.glass, .cos-card');
  if (!el) return;
  const r = el.getBoundingClientRect();
  el.style.setProperty('--mx', `${e.clientX - r.left}px`);
  el.style.setProperty('--my', `${e.clientY - r.top}px`);
});
requestAnimationFrame(() => moveNavGlider());

// ======================================================================= АВТООБНОВЛЕНИЕ
let UPD = { status: 'idle' }, updManual = false, updShown = '';
/** Окно «Доступно обновление» — как в Lunar Client: один раз на каждую новую версию. */
function updatePopup() {
  const u = UPD, v = u.version || '';
  const key = u.status + v;
  if (!v || updShown === key || !['available', 'ready'].includes(u.status) || $('#modal-root').innerHTML) return;
  updShown = key;
  const ready = u.status === 'ready';
  const m = modal(`<div class="upd-pop">
      <div class="upd-icon">🐢<i></i><i></i><i></i></div>
      <h3>${t('upd_new_title')}</h3>
      <p class="upd-ver"><span>v${esc(u.current || INFO.version)}</span> → <b>v${esc(v)}</b></p>
      <p class="note">${t(ready ? 'upd_new_ready' : 'upd_new_text')}</p>
      <div class="upd-pop-bar" hidden><i></i></div>
      <div class="modal-foot"><button class="btn ghost" data-close>${t('upd_later')}</button><button class="btn primary" id="upd-now">${t('upd_now')}</button></div>
    </div>`, 'narrow');
  $('#upd-now', m.el).onclick = () => {
    $('#upd-now', m.el).disabled = true;
    $('.upd-pop-bar', m.el).hidden = false;
    aqua.installUpdate();
  };
}
function renderUpdate() {
  const bar = $('#update-bar');
  if (!bar) return;
  const u = UPD, v = u.version || '';
  let html = '';
  if (u.status === 'downloading') html = `<span>🐢 ${t('upd_down', { v, p: u.percent || 0 })}</span><i class="upd-progress" style="width:${u.percent || 0}%"></i>`;
  else if (u.status === 'ready' && !u.portable) html = `<span>✨ ${t('upd_ready', { v })}</span><button class="btn primary" id="upd-go">${t('upd_restart')}</button>`;
  else if (u.status === 'ready') html = `<span>✨ ${t('upd_restarting')}</span>`;
  else if (u.status === 'available') html = `<span>✨ ${t('upd_avail', { v })}</span><button class="btn primary" id="upd-go">${t('upd_now')}</button>`;
  bar.hidden = !html;
  bar.innerHTML = html;
  const go = $('#upd-go'); if (go) go.onclick = () => { go.disabled = true; aqua.installUpdate(); };
  const pop = $('.upd-pop-bar i');
  if (pop) pop.style.width = (u.status === 'ready' ? 100 : u.percent || 0) + '%';
  updatePopup();
  if (updManual) {
    if (u.status === 'latest') { toast(t('upd_latest'), 'ok'); updManual = false; }
    else if (u.status === 'error') { toast(t('upd_err') + (u.error ? ': ' + u.error : ''), 'err'); updManual = false; }
    else if (['ready', 'available', 'downloading'].includes(u.status)) updManual = false;
  }
}
if (window.aqua && aqua.onUpdate) {
  aqua.onUpdate((u) => { UPD = u; renderUpdate(); });
  aqua.updateState().then((u) => { UPD = u; renderUpdate(); }).catch(() => {});
}
document.addEventListener('click', (e) => {
  if (e.target && e.target.id === 'check-upd') { updManual = true; toast(t('upd_checking')); aqua.checkUpdate().catch(() => {}); }
});

// Запасная картинка, если аватар не загрузился (inline-обработчики запрещены CSP)
document.addEventListener('error', (e) => {
  const img = e.target;
  if (img && img.tagName === 'IMG' && /mc-heads\.net/.test(img.src)) img.src = 'assets/icons/turtle.png';
}, true);
