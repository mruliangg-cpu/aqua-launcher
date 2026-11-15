# Aqua Network — один общий сервер для всех игроков

Запускаешь **только ты, один раз**. Все игроки Aqua Launcher подключаются к нему: друзья, аккаунты Aqua (уникальные ники), значок 🐢 в TAB, косметика, скины, CurseForge.

## ✅ Рекомендуется: Cloudflare Workers (бесплатно, не засыпает, данные не пропадают)
Файл: `cloudflare/worker.js`. Командная строка не нужна — всё в браузере:

1. Зарегистрируйся на **https://dash.cloudflare.com** (бесплатно).
2. **Storage & Databases → D1 → Create** → имя `aqua-network` → Create.
3. **Workers & Pages → Create → Create Worker** → имя `aqua-network` → **Deploy**.
4. Нажми **Edit code**, удали всё, вставь содержимое `cloudflare/worker.js` → **Deploy**.
5. В воркере: **Settings → Bindings → Add → D1 database** → Variable name: `DB`, база: `aqua-network` → Save.
6. (Для CurseForge) **Settings → Variables and Secrets → Add** → тип *Secret*, имя `CF_API_KEY`, значение — бесплатный ключ с https://console.curseforge.com.
7. Открой адрес воркера (например `https://aqua-network.ИМЯ.workers.dev`) — должно показать `"name":"Aqua Network","ok":true`.
8. Пришли этот адрес — его вошьют в лаунчер, и игрокам ничего не нужно будет настраивать.

Бесплатно: 100 000 запросов в день и база 5 ГБ — хватает на десятки игроков онлайн каждый день.

## Запасной вариант: свой ПК / VPS
`node server.js` (Node 18+, порт 8080, переменные `PORT`, `CF_API_KEY`, `DATA_DIR`).
⚠️ Бесплатный Render не подходит: засыпает и стирает данные при перезапуске.
