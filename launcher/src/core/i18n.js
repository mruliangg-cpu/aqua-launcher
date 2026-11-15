/** Язык сообщений ядра: английский по умолчанию, русский по настройке. */
module.exports = function L(ru, en) {
  try {
    const s = require('./store').state.settings;
    return s.language === 'ru' ? ru : en;
  } catch { return en; }
};
