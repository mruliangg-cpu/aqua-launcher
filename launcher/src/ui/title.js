/**
 * Правила пользовательского титула над ником (общие для лаунчера; копия — в Aqua Network и в моде).
 * До 12 символов: буквы, цифры, пробел и _ - ! ? .  Слова OWNER, ADMIN, MODER (в любом виде) запрещены.
 */
(function (root) {
  const COLORS = ['gold', 'aqua', 'blue', 'green', 'red', 'pink', 'purple', 'white', 'rainbow'];
  const HEX = { gold: '#FFC83D', aqua: '#3FE0FF', blue: '#4D8DFF', green: '#5CFF8A', red: '#FF5566', pink: '#FF7AD9', purple: '#B57CFF', white: '#FFFFFF', rainbow: '#FFFFFF' };
  const CYR2LAT = { А: 'A', В: 'B', Е: 'E', Ё: 'E', К: 'K', М: 'M', Н: 'H', О: 'O', Р: 'P', С: 'C', Т: 'T', Х: 'X', У: 'Y', Д: 'D', И: 'N', Ш: 'W', Л: 'L', Г: 'R', З: 'E', П: 'N', Ч: 'A', Б: 'B' };
  const LEET = { 0: 'O', 1: 'I', 3: 'E', 4: 'A', 5: 'S', 6: 'G', 7: 'T', 8: 'B', 9: 'G', '!': 'I', L: 'I' };
  const LAT2CYR = { A: 'А', B: 'В', E: 'Е', K: 'К', M: 'М', H: 'Н', O: 'О', P: 'Р', C: 'С', T: 'Т', X: 'Х', Y: 'У', 0: 'О', Ё: 'Е', N: 'И', D: 'Д' };
  const BAN_LAT = ['OWNER', 'OWNR', 'ADMIN', 'ADMN', 'MODER', 'MODR'];
  const BAN_LAT_EXACT = ['MOD', 'MODS', 'ADM', 'OWN'];
  const BAN_CYR = ['АДМИН', 'МОДЕР', 'ВЛАДЕЛ', 'ОВНЕР', 'ОУНЕР', 'ОВНР'];

  function check(input) {
    const t = String(input == null ? '' : input).replace(/\s+/g, ' ').trim();
    if (!t) return { ok: true, title: '' };
    if (t.length > 12) return { ok: false, reason: 'long' };
    if (!/^[A-Za-zА-Яа-яЁё0-9 _\-!?.]+$/.test(t)) return { ok: false, reason: 'chars' };
    const up = t.toUpperCase();
    const lat = [...up].map((c) => CYR2LAT[c] || c).map((c) => LEET[c] || c).join('').replace(/VV/g, 'W').replace(/[^A-Z]/g, '');
    const cyr = [...up].map((c) => LAT2CYR[c] || c).join('').replace(/[^А-Я]/g, '');
    if (BAN_LAT.some((w) => lat.includes(w)) || BAN_LAT_EXACT.includes(lat) || BAN_CYR.some((w) => cyr.includes(w))) return { ok: false, reason: 'banned' };
    return { ok: true, title: t };
  }
  const color = (c) => (COLORS.includes(c) ? c : 'gold');
  const api = { check, color, COLORS, HEX };
  if (typeof module !== 'undefined' && module.exports) module.exports = api; else root.AquaTitle = api;
})(typeof window !== 'undefined' ? window : this);
