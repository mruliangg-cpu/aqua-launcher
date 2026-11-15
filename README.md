# 🐢 Aqua Launcher + 🟣 Aqua Client

**Aqua Launcher** — лаунчер Minecraft Java в морском стиле (рыбы, черепахи, ламинарии, пузырьки).
**Aqua Client** — внутриигровой PvP/QoL-клиент в стиле Энда, меню открывается на **правый Shift** (как в Lunar).

## Что умеет лаунчер
- Все версии Minecraft: релизы, снапшоты, beta/alpha (список прямо от Mojang).
- Загрузчики: **Vanilla, Fabric, Quilt, Forge, NeoForge** (Forge/NeoForge — бета), выбор версии загрузчика.
- Сборки (instances): у каждой свои моды, миры и настройки, иконка, своя ОЗУ.
- Моды, ресурспаки и шейдеры с **Modrinth** и **CurseForge** — только совместимые с версией сборки, с автоустановкой зависимостей; включение/выключение/удаление.
- **FPS Boost**: автоматически ставит Sodium/Embeddium, Lithium, FerriteCore, EntityCulling, ImmediatelyFast, ModernFix, Dynamic FPS, MoreCulling + оптимизированные флаги Java (G1GC).
- Java скачивается сама (Temurin 8 / 17 / 21 — под нужную версию игры).
- Вход через **Microsoft** (токены шифруются средствами Windows) и **офлайн-профили** — они открываются после одного входа через Microsoft с купленной игрой.
- Консоль логов, прогресс загрузки, настройки окна игры.

## Aqua Client (Fabric 1.21.1) — правый Shift
52 модуля в 7 категориях, у каждого свои настройки и клавиша:
- **HUD:** Watermark, FPS, CPS, Keystrokes, Coordinates, Direction HUD, Armor Status, Potion Effects, Item Counter, Ping, Clock, Server Address, Memory, Speed, Day Counter, Saturation, Pack Display, Session Time
- **Бой:** Reach Display, Combo Counter, Target HUD, Hit Particles
- **Визуал:** Zoom (C, колёсико — кратность), Freelook (Left Alt), Fullbright, Block Outline, Crosshair, Time Changer, Weather Changer, Low Fire, No Pumpkin, Hide Scoreboard, No Hurt Cam
- **Движение:** Toggle Sprint / Sneak  •  **Разное:** Auto GG, Death Coords  •  **FPS:** FPS Booster, Unfocused FPS

Меню: категории, поиск (просто начните печатать), карточки с анимацией, ползунки, переключатели, выбор цвета (HSB + прозрачность + радуга), бинды. **Редактор HUD**: перетаскивание, колёсико — размер, прилипание к центру/краям, ПКМ — настройки. Всё сохраняется в `config/aquaclient.json`.

## Версия 3.1
- Всё в стиле **Aqua**: внутриигровой клиент теперь **Aqua Client** (морская палитра, черепаха, пузырьки в меню), сеть — **Aqua Network**, сборка по умолчанию — **Aqua PvP**.
- **Друзья**: добавление по нику, заявки, принятие/отклонение, кто в сети и во что играет.
- **Аккаунт Aqua без Microsoft**: регистрация по нику и паролю, ник уникальный — занятый взять нельзя. Ник Microsoft всегда принадлежит владельцу лицензии (сервер проверяет его через Microsoft).
- Новые анимации: волна при нажатии, 3D-наклон карточек, плавное появление страниц, светящаяся кнопка «Играть».

## Версия 3.0
- Интерфейс на **английском (по умолчанию) и русском** — переключается в Настройках или на экране входа.
- **Гардероб** (бесплатная косметика): 14 плащей, 13 шляп, 7 вещей для лица, 6 видов крыльев, 6 рюкзаков, 7 питомцев, 9 аур, 5 значков у ника.
- **Скины**: загрузка PNG, копирование по нику, классическая/тонкая модель, загрузка в Minecraft для аккаунта Microsoft.
- **Значок 🐢 у ника и в TAB** у игроков Aqua Launcher, ник переливается океанским синим. **MrUlian_gg** — всегда зелёный, с 🐢 и меткой **OWNER**.
- **Aqua Network** (папка `aqua-network`, работает на Cloudflare Workers + D1: https://aqua-network.mruliangg.workers.dev, уже встроен по умолчанию) — через него игроки видят косметику и скины друг друга, и работает CurseForge без ключа у каждого игрока.
- Серверы из лаунчера сами появляются в игре в «Сетевой игре». Donut SMP в списке; вход через Microsoft — настоящая лицензия.
- 20+ новых настроек: тема, масштаб, звуки, качество фона, поведение после запуска, автовход на сервер, приоритет процесса, язык/FPS/дальность/масштаб игры, пресеты Java, потоки загрузки, автозапуск с Windows и др.
- Aqua Client: 79 модулей (20 новых: FPS Graph, Health HUD, Held Item Info, Nearby Players, Biome, Dimension, Player Counter, Hunger HUD, XP HUD, Fall Distance, Date, Death Counter, Distance Traveled, Jump Counter, Spawn Distance, TPS Meter, Block Info, Countdown, No Vignette, RAM Cleaner), перевод EN/RU (по языку игры), новые: Auto Respawn, Low Health Warning, Durability Alert, Own Nametag, Entity Counter, World Time, Aqua Network.

## Готовый .exe
`AquaLauncher-Portable-2.0.0.exe` — просто запустите, установка не нужна. При первом запуске сборки с Aqua Client лаунчер сам скачает JDK 21 и соберёт мод (3–6 минут, один раз).

Новое в 2.0: 52 модуля (Minimap, плащи и аура, Waypoints, TNT Timer, Cooldowns, FOV Changer, Snaplook, Auto Text, Chat Timestamps, Hitboxes, Chunk Borders, Light Level, Stopwatch…), страница «Серверы» с пингом и автовходом, экран входа, новый живой подводный фон, все 800+ версий видны сразу.

## Сборка из исходников (Windows)
1. Установите **Node.js LTS** — https://nodejs.org и **Java 21 JDK** — https://adoptium.net/temurin/releases/?version=21
2. Запустите **`build-mod.bat`** — соберёт Aqua Client и положит его в лаунчер (первый раз 3–10 минут).
3. Запустите **`start-launcher.bat`** — откроется Aqua Launcher.
4. Хотите `.exe`? Запустите **`build-launcher.bat`** → установщик и portable-версия появятся в `launcher\dist`.

**Без установки чего-либо:** загрузите папку в репозиторий GitHub — workflow `.github/workflows/build.yml` сам соберёт мод и `.exe` (вкладка *Actions → Artifacts*).

## CurseForge
Для каталога CurseForge нужен бесплатный API-ключ: https://console.curseforge.com → вставьте в *Настройки → CurseForge*. Modrinth работает без ключа.

## Структура
```
launcher/        Electron-лаунчер (main.js, preload.js, src/core — логика, src/ui — интерфейс)
aqua-client/    Fabric-мод Aqua Client (Java 21, Yarn-маппинги)
build-mod.bat    сборка мода → launcher/resources/aqua-client-1.21.1.jar
start-launcher.bat / build-launcher.bat
```

## Важно
- Aqua Client собран под **Fabric 1.21.1**; в других версиях лаунчер просто не подключает его (FPS Boost работает везде, где есть загрузчик модов).
- Модули — только честные функции уровня Lunar (без читов), их разрешают на большинстве серверов. Freelook и Zoom некоторые серверы запрещают — проверьте правила.
