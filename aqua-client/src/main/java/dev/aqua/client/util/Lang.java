package dev.aqua.client.util;

import net.minecraft.client.MinecraftClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Двуязычный интерфейс Aqua Client: русский, если язык игры русский, иначе английский.
 * Тексты в коде написаны по-русски; tr() подставляет английский перевод.
 */
public final class Lang {
    private static final Map<String, String> EN = new HashMap<>();
    private static final Map<String, String> RU = new HashMap<>();

    private Lang() {}

    public static boolean isRu() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            String l = mc != null && mc.getLanguageManager() != null ? mc.getLanguageManager().getLanguage() : "en_us";
            return l != null && (l.startsWith("ru") || l.startsWith("uk") || l.startsWith("be"));
        } catch (Exception e) { return false; }
    }

    /** Перевод статичной строки (название настройки, описание, режим). */
    public static String tr(String s) {
        if (s == null) return "";
        if (isRu()) return RU.getOrDefault(s, s);
        return EN.getOrDefault(s, s);
    }

    /** Динамическая строка: выбрать вариант по языку. */
    public static String t(String ru, String en) { return isRu() ? ru : en; }

    private static void p(String ru, String en) { EN.put(ru, en); RU.put(en, ru); }

    static {
        String[][] d = {
            // категории и интерфейс
            {"Бой", "Combat"}, {"Визуал", "Render"}, {"Движение", "Movement"}, {"Разное", "Misc"}, {"Косметика", "Cosmetics"},
            {"Все модули", "All modules"}, {"Поиск", "Search"}, {"⌕ Поиск...", "⌕ Search..."}, {"ВКЛЮЧЁН", "ENABLED"}, {"ВЫКЛЮЧЕН", "DISABLED"},
            {"‹ Назад", "‹ Back"}, {"✎ Редактор HUD", "✎ HUD Editor"}, {"Редактор HUD", "HUD Editor"}, {"Модуль включён", "Module enabled"},
            {"Клавиша", "Keybind"}, {"Нажмите клавишу...", "Press a key..."}, {"Нет", "None"}, {"Позиция на экране", "Screen position"},
            {"Сбросить", "Reset"}, {"Ничего не найдено", "Nothing found"}, {"нажмите, чтобы ввести", "click to type"},
            {"Оттенок", "Hue"}, {"Насыщенность", "Saturation"}, {"Яркость", "Brightness"}, {"Прозрачность", "Opacity"}, {"Хрома (радуга)", "Chroma (rainbow)"},
            {"‹ Меню модулей", "‹ Modules"}, {"Сбросить всё", "Reset all"},
            {"ЛКМ — двигать   •   Колёсико — размер   •   ПКМ — настройки   •   ESC — назад", "LMB — move   •   Wheel — size   •   RMB — settings   •   ESC — back"},
            // общие настройки HUD
            {"Размер", "Scale"}, {"Фон", "Background"}, {"Цвет фона", "Background color"}, {"Цвет текста", "Text color"}, {"Тень текста", "Text shadow"},
            {"Стиль", "Style"}, {"Плашка", "Box"}, {"Скобки", "Brackets"}, {"Текст", "Text"},
            // описания модулей
            {"Логотип Aqua Client с черепахой", "Aqua Client logo with a turtle"}, {"Кадры в секунду", "Frames per second"},
            {"Клики в секунду (левая | правая)", "Clicks per second (left | right)"}, {"Показывает нажатия WASD, мыши и пробела", "Shows WASD, mouse and space presses"},
            {"Координаты XYZ, направление и биом", "XYZ coordinates, facing and biome"}, {"Компас-полоса как в Lunar", "Lunar-style compass bar"},
            {"Броня и предмет в руке с прочностью", "Armor and held item durability"}, {"Активные эффекты зелий и их время", "Active potion effects and timers"},
            {"Сколько у вас жемчуга, яблок, стрел и тотемов", "Counts pearls, apples, arrows and totems"}, {"Задержка до сервера", "Server latency"},
            {"Реальное время", "Real-life clock"}, {"IP сервера, на котором вы играете", "Address of the current server"},
            {"Использование оперативной памяти", "Memory usage"}, {"Скорость движения (блоков/с)", "Movement speed (blocks/s)"},
            {"Номер игрового дня", "In-game day number"}, {"Насыщение (скрытая шкала голода)", "Saturation (hidden hunger bar)"},
            {"Активные ресурспаки", "Active resource packs"}, {"Сколько вы уже в игре", "Session play time"},
            {"Дистанция последнего удара", "Distance of your last hit"}, {"Серия ударов без получения урона", "Hits in a row without taking damage"},
            {"Здоровье и ник цели, которую вы бьёте", "Health and name of your target"}, {"Дополнительные частицы при ударе", "Extra particles on hit"},
            {"Перезарядка жемчуга Края, щита и др.", "Ender Pearl, shield and other cooldowns"},
            {"Приближение на клавишу C (меняется в управлении)", "Zoom with C (rebind in Controls)"}, {"Осмотреться, не поворачивая персонажа (Left Alt)", "Look around without turning (Left Alt)"},
            {"Видно всё даже в темноте и пещерах", "See everything, even in dark caves"}, {"Своя обводка и заливка блока под прицелом", "Custom outline and fill for the targeted block"},
            {"Свой прицел: крест, точка, круг", "Custom crosshair: cross, dot, circle"}, {"Своё время суток (только визуально)", "Custom time of day (visual only)"},
            {"Убирает дождь или включает его (визуально)", "Removes or adds rain (visual only)"}, {"Огонь на экране не закрывает обзор", "Fire overlay no longer blocks your view"},
            {"Убирает затемнение от тыквы на голове", "Removes the pumpkin overlay"}, {"Прячет табло справа", "Hides the sidebar scoreboard"},
            {"Без тряски камеры при получении урона", "No camera shake when hurt"}, {"Поле зрения до 150° и без эффекта бега", "FOV up to 150° and no sprint FOV"},
            {"Посмотреть на себя спереди, пока зажата Z", "Look at yourself from the front while holding Z"}, {"Секунды до взрыва над динамитом", "Seconds until TNT explodes"},
            {"Прячет полоску здоровья босса", "Hides boss bars"}, {"Показывает хитбоксы сущностей (как F3+B)", "Shows entity hitboxes (like F3+B)"},
            {"Границы чанков (как F3+G)", "Chunk borders (like F3+G)"}, {"Плащ Aqua Client и аура частиц", "Aqua Client capes, hats, wings, pets and auras"},
            {"Бег и приседание по одному нажатию", "Sprint and sneak with one press"}, {"Автоматически пишет gg после победы/конца игры", "Says gg automatically after a game"},
            {"Сообщение или команда по нажатию клавиши", "Message or command on a key press"}, {"Точки маршрута: B — поставить, видно сквозь стены", "Waypoints: press B, visible through walls"},
            {"Время перед сообщениями в чате", "Time before every chat message"}, {"Координаты последней смерти и расстояние", "Last death position and distance"},
            {"Снижает тяжёлые настройки графики ради FPS", "Lowers heavy graphics settings for FPS"}, {"Меньше FPS, когда вы в другом окне", "Lower FPS while the game is unfocused"},
            {"Мини-карта местности с игроками и мобами", "Minimap with players and mobs"}, {"Секундомер: клавиша старт/стоп в настройках", "Stopwatch: start/stop key in settings"},
            {"Уровень освещения под ногами (спавн мобов)", "Light level at your feet (mob spawning)"},
            // настройки
            {"Показывать FPS", "Show FPS"}, {"Кнопки мыши", "Mouse buttons"}, {"CPS на кнопках", "CPS on buttons"}, {"Пробел", "Space bar"},
            {"Цвет нажатия", "Pressed color"}, {"Текст при нажатии", "Pressed text"}, {"Направление", "Facing"}, {"Биом", "Biome"},
            {"Координаты Незера", "Nether coordinates"}, {"Ширина", "Width"}, {"Градусы", "Degrees"}, {"Прочность", "Durability"}, {"Число", "Number"},
            {"Процент", "Percent"}, {"Предмет в руке", "Held item"}, {"Горизонтально", "Horizontal"}, {"Иконки", "Icons"}, {"Мигать в конце", "Blink when ending"},
            {"Жемчуг Края", "Ender Pearls"}, {"Золотые яблоки", "Golden apples"}, {"Стрелы", "Arrows"}, {"Тотемы", "Totems"}, {"Пузырьки опыта", "XP bottles"},
            {"Паутина", "Cobwebs"}, {"При наведении", "On look"}, {"Частицы", "Particles"}, {"Крит", "Crit"}, {"Магия", "Magic"}, {"Крит + магия", "Crit + magic"},
            {"Портал", "Portal"}, {"Множитель", "Multiplier"}, {"Звук попадания", "Hit sound"}, {"Кратность", "Zoom factor"}, {"Плавный зум", "Smooth zoom"},
            {"Колёсико мыши", "Scroll to zoom"}, {"Кинематографичная камера", "Cinematic camera"}, {"Снижать чувствительность", "Lower sensitivity"},
            {"Режим", "Mode"}, {"Удерживать", "Hold"}, {"Переключать", "Toggle"}, {"Инверсия по вертикали", "Invert vertical"}, {"Цвет линий", "Line color"},
            {"Заливка", "Fill"}, {"Цвет заливки", "Fill color"}, {"Вид", "Shape"}, {"Крест", "Cross"}, {"Точка", "Dot"}, {"Круг", "Circle"}, {"Т-образный", "T-shape"},
            {"Длина", "Length"}, {"Отступ", "Gap"}, {"Толщина", "Thickness"}, {"Цвет", "Color"}, {"Обводка", "Outline"}, {"Точка в центре", "Center dot"},
            {"В третьем лице", "In third person"}, {"Время (тики)", "Time (ticks)"}, {"Погода", "Weather"}, {"Ясно", "Clear"}, {"Дождь", "Rain"}, {"Гроза", "Thunder"},
            {"Опустить на", "Lower by"}, {"Сила тряски", "Shake strength"}, {"Поле зрения", "Field of view"}, {"Без эффекта скорости", "No speed FOV effect"},
            {"Переключаемый бег", "Toggle sprint"}, {"Переключаемое приседание", "Toggle sneak"}, {"Текст на экране", "Show text"},
            {"Сообщение", "Message"}, {"Задержка", "Delay"}, {"Убрать облака", "Disable clouds"}, {"Минимум частиц", "Minimal particles"},
            {"Без теней мобов", "No entity shadows"}, {"Без смешивания биомов", "No biome blend"}, {"Мобы ближе (75%)", "Entity distance 75%"},
            {"Быстрая графика", "Fast graphics"}, {"FPS без фокуса", "Unfocused FPS"}, {"Размер карты", "Map size"}, {"Масштаб (блоков на пиксель)", "Zoom (blocks per pixel)"},
            {"Игроки", "Players"}, {"Враждебные мобы", "Hostile mobs"}, {"Координаты под картой", "Coordinates under map"}, {"Луч в небо", "Sky beam"},
            {"Точка смерти", "Death point"}, {"Макс. дистанция", "Max distance"}, {"Размер надписей", "Label size"}, {"Удалить ближайшую точку", "Remove nearest waypoint"},
            {"Удалить все точки этого мира", "Remove all waypoints in this world"}, {"Удалить", "Remove"}, {"Очистить", "Clear"}, {"Секунды", "Seconds"},
            {"Старт / стоп", "Start / stop"}, {"Сброс", "Reset"}, {"Плащ", "Cape"}, {"Узор на элитрах", "Pattern on elytra"}, {"Аура", "Aura"}, {"Сила ауры", "Aura amount"},
            {"Эндер", "Ender"}, {"Портал (анимация)", "Portal (animated)"}, {"Око Края", "Eye of Ender"}, {"Жемчуг", "Pearl"}, {"Аква", "Aqua"}, {"Дракон", "Dragon"},
            {"Энд-род", "End Rod"}, {"Сердечки", "Hearts"}, {"Ноты", "Notes"}, {"Пузыри", "Bubbles"},
            {"Шляпа", "Hat"}, {"Крылья", "Wings"}, {"Спина", "Back"}, {"Питомец", "Pet"}, {"Лицо", "Face"}, {"Значок у ника", "Name icon"},
            {"Показывать косметику других", "Show others' cosmetics"}, {"Делиться моим скином", "Share my skin"}, {"Сеть Ender", "Aqua Network"},
            {"Показывать свой ник (F5)", "Show own nametag (F5)"}, {"Порог здоровья", "Health threshold"}, {"Порог прочности", "Durability threshold"},
            {"Минуты", "Minutes"}, {"Автоматически", "Automatic"}, {"Каждые (мин)", "Every (min)"}, {"Очистить память сейчас", "Free memory now"}, {"Радиус", "Radius"},
            {"График FPS — видно просадки и фризы", "FPS graph — spot drops and stutters"}, {"Здоровье и золотые сердца числами", "Health and absorption as numbers"},
            {"Название, прочность и чары предмета в руке", "Name, durability and enchants of the held item"}, {"Список игроков рядом и расстояние до них", "Nearby players and their distance"},
            {"Биом, в котором вы стоите", "Biome you are standing in"}, {"Текущее измерение", "Current dimension"}, {"Сколько игроков на сервере", "Players on the server"},
            {"Голод и насыщение числами", "Hunger and saturation as numbers"}, {"Уровень опыта и прогресс", "XP level and progress"},
            {"Высота падения (осторожно с уроном)", "Fall distance (watch out for damage)"}, {"Сегодняшняя дата", "Today's date"}, {"Смертей за сессию", "Deaths this session"},
            {"Сколько блоков вы прошли за сессию", "Blocks traveled this session"}, {"Прыжков за сессию", "Jumps this session"},
            {"Расстояние до точки спавна мира", "Distance to world spawn"}, {"Насколько лагает сервер (20 = идеально)", "Server lag (20 = perfect)"},
            {"Блок под прицелом и расстояние до него", "Targeted block and its distance"}, {"Таймер обратного отсчёта по клавише", "Countdown timer on a key"},
            {"Убирает тёмные края экрана", "Removes dark screen edges"}, {"Очищает неиспользуемую память Java", "Frees unused Java memory"},
            {" модулей", " modules"}, {" включено", " enabled"}, {"ПКМ — настройки", "RMB — settings"},
            {"Звук", "Sound"}, {"Задержка (с)", "Delay (s)"}, {"Сервер", "Server URL"},
            {"Значок черепахи и косметика игроков Aqua Launcher", "Turtle icon and cosmetics of Aqua Launcher players"},
            {"Автоматически возрождает после смерти", "Respawns automatically after death"}, {"Красная рамка экрана при низком здоровье", "Red screen border at low health"},
            {"Предупреждает, что броня или инструмент ломается", "Warns when armor or tools are about to break"}, {"Показывает ваш ник в F5", "Shows your own nametag in F5"},
            {"Сколько сущностей вокруг (мобы, предметы)", "How many entities are around"}, {"Время в игре и фаза луны", "In-game time and moon phase"},
            {"Плащ Aqua Client и аура частиц", "Aqua Client capes, hats, wings, pets and auras"},
        };
        for (String[] e : d) p(e[0], e[1]);
    }
}
