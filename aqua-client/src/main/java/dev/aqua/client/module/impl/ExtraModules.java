package dev.aqua.client.module.impl;

import dev.aqua.client.gui.Render2D;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.module.TextHudModule;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.NumberSetting;
import dev.aqua.client.util.Lang;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Ещё 100 модулей Aqua Client: HUD-информация, предупреждения, звук, чат, действия, движение,
 * производительность и сеть. Всё только визуальное и удобное — никаких преимуществ над другими игроками.
 */
public final class ExtraModules {
    private ExtraModules() {}

    public static volatile long lastServerPacket;
    private static final long START = System.currentTimeMillis();
    private static final List<Alert> ALERTS = new ArrayList<>();
    private static MinecraftClient mc() { return MinecraftClient.getInstance(); }
    private static ClientPlayerEntity p() { return mc().player; }
    private static String t(String ru, String en) { return Lang.t(ru, en); }

    // =================================================================== регистрация
    private static int hudIndex;
    private static TextHudModule hud(String name, String desc, Category cat, Supplier<String> text, String preview) {
        int i = hudIndex++;
        float x = 0.02f + (i % 4) * 0.24f, y = 0.30f + (i / 4 % 12) * 0.05f;
        return new TextHudModule(name, desc, cat, x, y, () -> { try { return p() == null ? null : text.get(); } catch (Exception e) { return null; } }, () -> preview);
    }

    public static List<Module> all() {
        List<Module> L = new ArrayList<>();
        Category H = Category.HUD, C = Category.COMBAT, M = Category.MOVEMENT, R = Category.RENDER, X = Category.MISC, P = Category.PERFORMANCE;

        // ---------------- HUD: игрок и мир (45)
        L.add(hud("Rotation", "Поворот головы: yaw и pitch", H, () -> String.format("Yaw %.1f° · Pitch %.1f°", wrap(p().getYaw()), p().getPitch()), "Yaw 182.4° · Pitch -12.0°"));
        L.add(hud("Chunk Coords", "Номер чанка и позиция в нём", H, () -> { var b = p().getBlockPos(); return t("Чанк ", "Chunk ") + (b.getX() >> 4) + " " + (b.getZ() >> 4) + "  (" + (b.getX() & 15) + ", " + (b.getZ() & 15) + ")"; }, "Chunk 8 -19  (3, 7)"));
        L.add(hud("Nether Coords", "Координаты в Незере ↔ Верхнем мире", H, () -> {
            var b = p().getBlockPos(); boolean nether = mc().world.getRegistryKey() == net.minecraft.world.World.NETHER;
            return nether ? t("Верх: ", "Overworld: ") + b.getX() * 8 + " " + b.getZ() * 8 : t("Незер: ", "Nether: ") + (b.getX() >> 3) + " " + (b.getZ() >> 3); }, "Nether: 16 -40"));
        L.add(hud("Distance To 0 0", "Расстояние до центра мира", H, () -> String.format(t("До 0,0: %.0f м", "To 0,0: %.0f m"), Math.hypot(p().getX(), p().getZ())), "To 0,0: 1234 m"));
        L.add(hud("Vertical Speed", "Скорость подъёма/падения", H, () -> String.format(t("Верт.: %+.2f м/с", "Vert.: %+.2f m/s"), p().getVelocity().y * 20), "Vert.: -3.92 m/s"));
        L.add(hud("Speed KMH", "Скорость в км/ч", H, () -> String.format(t("%.1f км/ч", "%.1f km/h"), dev.aqua.client.util.Stats.speed() * 3.6), "15.6 km/h"));
        L.add(hud("Altitude", "Высота и подсказка для поиска руд", H, () -> { int y = p().getBlockPos().getY(); return "Y " + y + (y <= -50 ? t(" · уровень алмазов", " · diamond level") : y <= 16 && y >= -16 ? t(" · уровень железа", " · iron level") : ""); }, "Y -58 · diamond level"));
        L.add(hud("Game Mode", "Текущий режим игры", H, () -> { var m = mc().interactionManager == null ? null : mc().interactionManager.getCurrentGameMode(); return m == null ? null : t("Режим: ", "Mode: ") + m.getTranslatableName().getString(); }, "Mode: Survival"));
        L.add(hud("Difficulty", "Сложность мира", H, () -> t("Сложность: ", "Difficulty: ") + mc().world.getDifficulty().getTranslatableName().getString(), "Difficulty: Hard"));
        L.add(hud("Weather", "Погода сейчас", H, () -> mc().world.isThundering() ? t("⛈ Гроза", "⛈ Thunder") : mc().world.isRaining() ? t("🌧 Дождь", "🌧 Rain") : t("☀ Ясно", "☀ Clear"), "☀ Clear"));
        L.add(hud("Moon Phase", "Фаза луны (важно для слизней)", H, () -> moon((int) (mc().world.getTimeOfDay() / 24000L % 8)), "🌕 Full moon"));
        L.add(hud("Day Night", "День или ночь и сколько осталось", H, () -> {
            long tod = mc().world.getTimeOfDay() % 24000L; boolean day = tod < 12542 || tod > 23460;
            long left = day ? ((12542 - tod + 24000) % 24000) : ((23460 - tod + 24000) % 24000);
            return (day ? "☀ " + t("День", "Day") : "☾ " + t("Ночь", "Night")) + " · " + left / 1200 + t(" мин", " min"); }, "☀ Day · 6 min"));
        L.add(hud("Clock Seconds", "Часы с секундами", H, () -> LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), "18:42:07"));
        L.add(hud("Clock 12h", "Часы в 12-часовом формате", H, () -> LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a")), "6:42 PM"));
        L.add(hud("Armor Points", "Очки брони", H, () -> "🛡 " + p().getArmor(), "🛡 20"));
        L.add(hud("Health Exact", "Точное здоровье и максимум", C, () -> String.format("❤ %.1f / %.0f", p().getHealth(), p().getMaxHealth()), "❤ 17.5 / 20"));
        L.add(hud("Absorption", "Жёлтые сердечки (поглощение)", C, () -> p().getAbsorptionAmount() > 0 ? String.format("💛 %.0f", p().getAbsorptionAmount()) : null, "💛 4"));
        L.add(hud("Air Meter", "Запас воздуха под водой", H, () -> p().getAir() < p().getMaxAir() ? t("Воздух ", "Air ") + Math.max(0, p().getAir() * 100 / p().getMaxAir()) + "%" : null, "Air 60%"));
        L.add(hud("Fire Timer", "Сколько ещё гореть", H, () -> p().getFireTicks() > 0 ? String.format(t("🔥 Горишь %.1f с", "🔥 Burning %.1fs"), p().getFireTicks() / 20f) : null, "🔥 Burning 3.5s"));
        L.add(hud("Freeze Meter", "Замерзание в рыхлом снегу", H, () -> p().getFrozenTicks() > 0 ? t("❄ Холод ", "❄ Freezing ") + p().getFrozenTicks() * 100 / 140 + "%" : null, "❄ Freezing 40%"));
        L.add(hud("Held Durability", "Прочность предмета в руке", C, () -> durability(p().getMainHandStack()), "Diamond Sword 87%"));
        L.add(hud("Offhand Item", "Предмет во второй руке", C, () -> { ItemStack s = p().getOffHandStack(); return s.isEmpty() ? null : t("Левая: ", "Offhand: ") + s.getName().getString() + (s.getCount() > 1 ? " ×" + s.getCount() : ""); }, "Offhand: Totem of Undying"));
        L.add(hud("Totem Counter", "Сколько тотемов бессмертия", C, () -> count(Items.TOTEM_OF_UNDYING, t("Тотемы", "Totems")), "Totems: 3"));
        L.add(hud("Pearl Counter", "Сколько жемчуга Края", C, () -> count(Items.ENDER_PEARL, t("Жемчуг", "Pearls")), "Pearls: 16"));
        L.add(hud("Gapple Counter", "Сколько золотых яблок", C, () -> count(Items.GOLDEN_APPLE, t("Яблоки", "Gapples")), "Gapples: 12"));
        L.add(hud("Arrow Counter", "Сколько стрел", C, () -> count(Items.ARROW, t("Стрелы", "Arrows")), "Arrows: 64"));
        L.add(hud("Block Counter", "Сколько блоков для строительства", C, () -> { int n = 0; var inv = p().getInventory(); for (int i = 0; i < inv.size(); i++) if (inv.getStack(i).getItem() instanceof BlockItem) n += inv.getStack(i).getCount(); return t("Блоки: ", "Blocks: ") + n; }, "Blocks: 128"));
        L.add(hud("XP Level", "Уровень опыта и прогресс", H, () -> String.format(t("Ур. %d · %d%%", "Lvl %d · %d%%"), p().experienceLevel, (int) (p().experienceProgress * 100)), "Lvl 30 · 45%"));
        L.add(hud("Total XP", "Всего очков опыта", H, () -> t("Опыт: ", "XP: ") + p().totalExperience, "XP: 1395"));
        L.add(hud("Food Level", "Уровень сытости", H, () -> "🍗 " + p().getHungerManager().getFoodLevel() + "/20", "🍗 18/20"));
        L.add(hud("Attack Cooldown", "Готовность удара в процентах", C, () -> { float c = p().getAttackCooldownProgress(0); return c < 1 ? t("Удар ", "Attack ") + (int) (c * 100) + "%" : null; }, "Attack 72%"));
        L.add(hud("Item Use Timer", "Натяжение лука, еда, щит", C, () -> p().isUsingItem() ? String.format(t("Использую %.1f с", "Using %.1fs"), p().getItemUseTime() / 20f) : null, "Using 0.9s"));
        L.add(hud("Effects Count", "Сколько эффектов зелий", H, () -> p().getStatusEffects().isEmpty() ? null : t("Эффекты: ", "Effects: ") + p().getStatusEffects().size(), "Effects: 3"));
        L.add(hud("Armor Durability", "Общая прочность брони", C, () -> {
            int cur = 0, max = 0; for (var slot : new net.minecraft.entity.EquipmentSlot[]{net.minecraft.entity.EquipmentSlot.HEAD, net.minecraft.entity.EquipmentSlot.CHEST, net.minecraft.entity.EquipmentSlot.LEGS, net.minecraft.entity.EquipmentSlot.FEET}) {
                ItemStack s = p().getEquippedStack(slot); if (s.isDamageable()) { cur += s.getMaxDamage() - s.getDamage(); max += s.getMaxDamage(); } }
            return max == 0 ? null : t("Броня ", "Armor ") + cur * 100 / max + "%"; }, "Armor 91%"));
        L.add(hud("Looking At Entity", "На кого смотришь и его здоровье", C, () -> {
            HitResult h = mc().crosshairTarget; if (!(h instanceof EntityHitResult eh)) return null; Entity e = eh.getEntity();
            return e.getName().getString() + (e instanceof LivingEntity le ? String.format(" ❤ %.0f", le.getHealth()) : ""); }, "Zombie ❤ 14"));
        L.add(hud("Target Distance", "Расстояние до того, на что смотришь", C, () -> { HitResult h = mc().crosshairTarget; return h == null || h.getType() == HitResult.Type.MISS ? null : String.format(t("Цель: %.1f бл.", "Target: %.1f bl."), p().getEyePos().distanceTo(h.getPos())); }, "Target: 3.4 bl."));
        L.add(hud("Looking At Block Pos", "Координаты блока под прицелом", H, () -> mc().crosshairTarget instanceof BlockHitResult b && b.getType() == HitResult.Type.BLOCK ? t("Блок: ", "Block: ") + b.getBlockPos().toShortString() : null, "Block: 12, 64, -30"));
        L.add(hud("Render Distance", "Дальность прорисовки и симуляции", H, () -> t("Прорисовка ", "Render ") + mc().options.getViewDistance().getValue() + " · " + t("симуляция ", "sim ") + mc().options.getSimulationDistance().getValue(), "Render 12 · sim 8"));
        L.add(hud("Loaded Chunks", "Загруженные чанки", H, () -> t("Чанки: ", "Chunks: ") + mc().world.getChunkManager().getLoadedChunkCount(), "Chunks: 441"));
        L.add(hud("Hostile Mobs", "Враждебные мобы рядом", C, () -> { int n = mc().world.getEntitiesByClass(HostileEntity.class, p().getBoundingBox().expand(24), e -> true).size(); return n == 0 ? null : t("Мобы рядом: ", "Hostiles: ") + n; }, "Hostiles: 4"));
        L.add(hud("Dropped Items", "Предметы на земле рядом", H, () -> { int n = mc().world.getEntitiesByClass(net.minecraft.entity.ItemEntity.class, p().getBoundingBox().expand(16), e -> true).size(); return n == 0 ? null : t("Предметы: ", "Items: ") + n; }, "Items: 7"));
        L.add(hud("Sprint Status", "Бежишь ли сейчас", M, () -> p().isSprinting() ? t("🏃 Бег", "🏃 Sprinting") : null, "🏃 Sprinting"));
        L.add(hud("Sneak Status", "Крадёшься ли сейчас", M, () -> p().isSneaking() ? t("🐢 Красться", "🐢 Sneaking") : null, "🐢 Sneaking"));
        L.add(hud("Air Time", "Сколько секунд в воздухе", M, () -> airTicks > 10 ? String.format(t("В воздухе %.1f с", "Airborne %.1fs"), airTicks / 20f) : null, "Airborne 1.2s"));

        // ---------------- HUD: система и производительность (8)
        L.add(hud("FPS Min Max", "Минимальный и максимальный FPS за 10 с", P, () -> { fpsSample(); return "FPS " + fpsMin + " / " + fpsMax; }, "FPS 112 / 240"));
        L.add(hud("Frame Time", "Время кадра в миллисекундах", P, () -> String.format("%.1f ms", 1000f / Math.max(1, mc().getCurrentFps())), "6.9 ms"));
        L.add(hud("CPU Threads", "Потоки процессора", P, () -> "CPU " + Runtime.getRuntime().availableProcessors() + t(" потоков", " threads"), "CPU 16 threads"));
        L.add(hud("Java Version", "Версия Java", P, () -> "Java " + System.getProperty("java.version"), "Java 21.0.5"));
        L.add(hud("Window Size", "Размер окна игры", P, () -> mc().getWindow().getWidth() + "×" + mc().getWindow().getHeight(), "1920×1080"));
        L.add(hud("Uptime", "Сколько запущена игра", P, () -> { long s = (System.currentTimeMillis() - START) / 1000; return t("Игра ", "Up ") + s / 3600 + ":" + String.format("%02d:%02d", s / 60 % 60, s % 60); }, "Up 1:24:09"));
        L.add(hud("Memory Percent", "Занятая память в процентах", P, () -> { Runtime r = Runtime.getRuntime(); return "RAM " + (r.totalMemory() - r.freeMemory()) * 100 / r.maxMemory() + "%"; }, "RAM 38%"));
        L.add(hud("Allocated RAM", "Выделено памяти Java", P, () -> "RAM max " + Runtime.getRuntime().maxMemory() / 1048576 + " MB", "RAM max 4096 MB"));

        // ---------------- Сеть (4)
        L.add(hud("Ping Monitor", "Пинг: средний, минимум, максимум и скачки", X, ExtraModules::pingStats, "Ping 42 · 38–55 · ±4"));
        L.add(hud("Connection Quality", "Качество соединения по пингу", X, () -> { int p = ping(); return p < 0 ? null : t("Связь: ", "Net: ") + (p < 60 ? t("отличная", "excellent") : p < 120 ? t("хорошая", "good") : p < 200 ? t("средняя", "fair") : t("плохая", "poor")); }, "Net: excellent"));
        L.add(hud("Server Brand", "Тип сервера (Paper, Fabric…)", X, () -> { String b = brand(); return b == null || mc().isInSingleplayer() ? null : t("Сервер: ", "Server: ") + b; }, "Server: Paper"));
        L.add(new PingStabilizer());
        L.add(new Alert("Lag Detector", "Предупреждает, если сервер завис", () -> {
            if (mc().isInSingleplayer() || lastServerPacket == 0) return null;
            long d = System.currentTimeMillis() - lastServerPacket;
            return d > 2500 ? String.format(t("Сервер не отвечает %.1f с", "Server not responding %.1fs"), d / 1000f) : null; }, false));

        // ---------------- Предупреждения (12)
        L.add(new Alert("Low Food Alert", "Пора поесть", () -> p().getHungerManager().getFoodLevel() <= 6 ? t("Пора поесть!", "Eat something!") : null, true));
        L.add(new Alert("Low Air Alert", "Заканчивается воздух под водой", () -> p().getAir() < p().getMaxAir() * 0.3 && p().isSubmergedInWater() ? t("Воздух заканчивается!", "Running out of air!") : null, true));
        L.add(new Alert("Fire Alert", "Ты горишь", () -> p().isOnFire() && !p().isFireImmune() ? t("Ты горишь!", "You are on fire!") : null, true));
        L.add(new Alert("Freeze Alert", "Ты замерзаешь", () -> p().getFrozenTicks() > 100 ? t("Замерзаешь!", "Freezing!") : null, true));
        L.add(new Alert("Fall Warning", "Падение с опасной высоты", () -> (double) p().fallDistance > 4 && !p().isOnGround() && !p().getAbilities().flying ? String.format(t("Падение %.0f бл.!", "Falling %.0f blocks!"), (double) p().fallDistance) : null, false));
        L.add(new Alert("Night Warning", "Скоро ночь — появятся мобы", () -> { long tod = mc().world.getTimeOfDay() % 24000L; return tod > 11300 && tod < 12542 ? t("Скоро ночь", "Night is coming") : null; }, false));
        L.add(new Alert("Full Inventory", "Инвентарь заполнен", () -> p().getInventory().getEmptySlot() < 0 ? t("Инвентарь полон", "Inventory full") : null, false));
        L.add(new Alert("Creeper Alert", "Крипер рядом", () -> mc().world.getEntitiesByClass(CreeperEntity.class, p().getBoundingBox().expand(8), e -> true).isEmpty() ? null : t("Крипер рядом!", "Creeper nearby!"), true));
        L.add(new Alert("Hostile Alert", "Враждебный моб совсем близко", () -> mc().world.getEntitiesByClass(HostileEntity.class, p().getBoundingBox().expand(4), e -> !(e instanceof CreeperEntity)).isEmpty() ? null : t("Моб рядом!", "Mob nearby!"), false));
        L.add(new Alert("Totem Reminder", "Мало здоровья и нет тотема в руке", () -> p().getHealth() < 8 && !p().getOffHandStack().isOf(Items.TOTEM_OF_UNDYING) && p().getInventory().count(Items.TOTEM_OF_UNDYING) > 0 ? t("Возьми тотем в руку!", "Hold a totem!") : null, true));
        L.add(new Alert("Potion Expiring", "Эффект зелья скоро закончится", () -> { for (var e : p().getStatusEffects()) if (e.getDuration() > 0 && e.getDuration() < 100) return t("Эффект заканчивается: ", "Effect ending: ") + dev.aqua.client.compat.V.effectName(e); return null; }, false));
        L.add(new Alert("Void Warning", "Под тобой пустота", () -> p().getY() < mc().world.getBottomY() + 4 ? t("Внизу пустота!", "Void below!") : null, true));
        L.add(new Alert("Low Durability Tool", "Инструмент в руке почти сломан", () -> { ItemStack s = p().getMainHandStack(); return s.isDamageable() && s.getMaxDamage() - s.getDamage() < Math.max(5, s.getMaxDamage() / 20) ? t("Инструмент ломается!", "Tool breaking!") : null; }, true));

        // ---------------- Визуал: настройки игры (9)
        L.add(new OptionToggle<>("No View Bobbing", "Без покачивания камеры при ходьбе", R, o -> o.getBobView(), false));
        L.add(new OptionToggle<>("No Nausea", "Без искажения экрана от тошноты и портала", R, o -> o.getDistortionEffectScale(), 0.0));
        L.add(new OptionToggle<>("No FOV Effects", "Без изменения обзора от бега и зелий", R, o -> o.getFovEffectScale(), 0.0));
        L.add(new OptionToggle<>("No Damage Tilt", "Без наклона камеры при уроне", R, o -> o.getDamageTiltStrength(), 0.0));
        L.add(new OptionToggle<>("No Enchant Glint", "Без блеска зачарованных вещей", R, o -> o.getGlintStrength(), 0.0));
        L.add(new OptionToggle<>("Clear Chat", "Прозрачный фон чата", R, o -> o.getTextBackgroundOpacity(), 0.0));
        L.add(new OptionToggle<>("Entity Shadows Off", "Без теней под мобами", P, o -> o.getEntityShadows(), false));
        L.add(new OptionToggle<>("Smooth Lighting Off", "Без мягкого освещения (FPS)", P, o -> o.getAo(), false));
        L.add(new OptionToggle<>("VSync Off", "Выключить вертикальную синхронизацию", P, o -> o.getEnableVsync(), false));

        // ---------------- Звук (6)
        L.add(new Volume("Mute Music", "Выключить музыку", SoundCategory.MUSIC, 0));
        L.add(new Volume("Mute Weather", "Тихий дождь и гром", SoundCategory.WEATHER, 0.1));
        L.add(new Volume("Mute Ambient", "Без звуков пещер", SoundCategory.AMBIENT, 0));
        L.add(new Volume("Quiet Mobs", "Тише враждебные мобы", SoundCategory.HOSTILE, 0.3));
        L.add(new Volume("Quiet Blocks", "Тише звуки блоков", SoundCategory.BLOCKS, 0.4));
        L.add(new Volume("Mute Jukebox", "Без пластинок и нотных блоков", SoundCategory.RECORDS, 0));

        // ---------------- Чат (4)
        L.add(new ChatModule("Mention Ping", "Звук, когда в чате упоминают твой ник", ChatKind.MENTION));
        L.add(new ChatModule("Chat Sound", "Звук на каждое сообщение в чате", ChatKind.SOUND));
        L.add(new ChatModule("Chat Logger", "Сохраняет чат в файл config/aquaclient-chat.log", ChatKind.LOG));
        L.add(new ChatModule("Friend Join Sound", "Звук, когда друг из Aqua заходит на сервер", ChatKind.JOIN));

        // ---------------- Действия: включи модуль — сработает один раз (6)
        L.add(new Action("Copy Coords", "Скопировать координаты в буфер", X, () -> { var b = p().getBlockPos(); clip(b.getX() + " " + b.getY() + " " + b.getZ()); return t("Координаты скопированы", "Coordinates copied"); }));
        L.add(new Action("Copy Server IP", "Скопировать адрес сервера", X, () -> { var s = mc().getCurrentServerEntry(); if (s == null) return t("Это одиночная игра", "Singleplayer"); clip(s.address); return t("Адрес скопирован", "Address copied"); }));
        L.add(new Action("Copy Look Block", "Скопировать координаты блока под прицелом", X, () -> { if (!(mc().crosshairTarget instanceof BlockHitResult b) || b.getType() != HitResult.Type.BLOCK) return t("Нет блока под прицелом", "No block targeted"); var bp = b.getBlockPos(); clip(bp.getX() + " " + bp.getY() + " " + bp.getZ()); return t("Скопировано", "Copied"); }));
        L.add(new Action("Clear Chat History", "Очистить чат", X, () -> { mc().inGameHud.getChatHud().clear(false); return t("Чат очищен", "Chat cleared"); }));
        L.add(new Action("Reload Chunks", "Перезагрузить чанки (как F3+A)", X, () -> { mc().worldRenderer.reload(); return t("Чанки перезагружены", "Chunks reloaded"); }));
        L.add(new Action("Copy Ping", "Скопировать пинг и сервер", X, () -> { int p = ping(); clip((mc().getCurrentServerEntry() == null ? "singleplayer" : mc().getCurrentServerEntry().address) + " " + p + " ms"); return t("Скопировано", "Copied"); }));

        // ---------------- Движение (3)
        L.add(new HoldKey("Toggle Sneak", "Красться без зажатия Shift", M, o -> o.sneakKey));
        L.add(new HoldKey("Auto Walk", "Идти вперёд без зажатия W", M, o -> o.forwardKey));
        L.add(new OptionToggle<>("Auto Jump Off", "Выключить автопрыжок", M, o -> o.getAutoJump(), false));

        // ---------------- Производительность (3)
        L.add(new SmartRender());
        L.add(new FpsCap());
        L.add(new BackgroundMute());
        return L;
    }

    // =================================================================== помощники
    private static int airTicks, fpsMin = 999, fpsMax;
    private static long fpsWindow;
    private static void fpsSample() {
        int f = mc().getCurrentFps(); long now = System.currentTimeMillis();
        if (now - fpsWindow > 10000) { fpsWindow = now; fpsMin = f; fpsMax = f; }
        fpsMin = Math.min(fpsMin, f); fpsMax = Math.max(fpsMax, f);
    }
    /** Тикается из Alert-цикла: время в воздухе. */
    private static void tickAir() { if (p() != null) airTicks = p().isOnGround() || p().isTouchingWater() ? 0 : airTicks + 1; }

    private static float wrap(float yaw) { float y = yaw % 360; return y < 0 ? y + 360 : y; }
    private static String moon(int ph) {
        String[] ru = {"🌕 Полнолуние", "🌖 Убывающая", "🌗 Последняя четверть", "🌘 Старая луна", "🌑 Новолуние", "🌒 Молодая луна", "🌓 Первая четверть", "🌔 Растущая"};
        String[] en = {"🌕 Full moon", "🌖 Waning gibbous", "🌗 Last quarter", "🌘 Waning crescent", "🌑 New moon", "🌒 Waxing crescent", "🌓 First quarter", "🌔 Waxing gibbous"};
        return Lang.isRu() ? ru[ph] : en[ph];
    }
    private static String durability(ItemStack s) {
        if (s.isEmpty() || !s.isDamageable()) return null;
        return s.getName().getString() + " " + (s.getMaxDamage() - s.getDamage()) * 100 / s.getMaxDamage() + "%";
    }
    private static String count(Item item, String label) { int n = p().getInventory().count(item); return n == 0 ? null : label + ": " + n; }
    private static void clip(String s) { mc().keyboard.setClipboard(s); }
    private static void ding(float pitch) {
        //#if MC >= 1.21.11
        mc().getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, pitch));
        //#else
        mc().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, pitch));
        //#endif
    }
    private static String brand() {
        //#if MC >= 1.20.2
        var h = mc().getNetworkHandler(); return h == null ? null : h.getBrand();
        //#else
        return p() == null ? null : p().getServerBrand();
        //#endif
    }

    private static int ping() {
        if (p() == null || mc().getNetworkHandler() == null || mc().isInSingleplayer()) return -1;
        PlayerListEntry e = mc().getNetworkHandler().getPlayerListEntry(p().getUuid());
        return e == null ? -1 : e.getLatency();
    }
    private static final ArrayDeque<Integer> PINGS = new ArrayDeque<>();
    private static long lastPingSample;
    private static String pingStats() {
        int now = ping();
        if (now < 0) return null;
        if (System.currentTimeMillis() - lastPingSample > 1000) { lastPingSample = System.currentTimeMillis(); PINGS.addLast(now); if (PINGS.size() > 60) PINGS.removeFirst(); }
        int min = Integer.MAX_VALUE, max = 0; long sum = 0;
        for (int v : PINGS) { min = Math.min(min, v); max = Math.max(max, v); sum += v; }
        double avg = PINGS.isEmpty() ? now : sum / (double) PINGS.size(), dev = 0;
        for (int v : PINGS) dev += Math.abs(v - avg);
        dev = PINGS.isEmpty() ? 0 : dev / PINGS.size();
        return String.format(t("Пинг %d · %d–%d · ±%.0f", "Ping %d · %d–%d · ±%.0f"), now, min == Integer.MAX_VALUE ? now : min, max, dev);
    }

    public static void tick() { tickAir(); }

    /** Предупреждения рисуются по центру экрана, одно под другим. */
    public static void renderAlerts(DrawContext ctx) {
        if (p() == null) return;
        int y = 52, w = ctx.getScaledWindowWidth();
        for (Alert a : ALERTS) {
            if (!a.isEnabled()) continue;
            String msg;
            try { msg = a.check.get(); } catch (Exception e) { msg = null; }
            if (msg == null) { a.shown = false; continue; }
            if (!a.shown) { a.shown = true; if (a.sound.isOn()) ding(0.6f); }
            if (a.blink && (System.currentTimeMillis() / 450) % 2 == 0) { y += 17; continue; }
            String s = "⚠ " + msg;
            float tw = Render2D.width(s);
            Render2D.roundRect(ctx, w / 2f - tw / 2f - 6, y, tw + 12, 14, 4, 0xC0081E33);
            ctx.drawText(mc().textRenderer, s, (int) (w / 2f - tw / 2f), y + 3, 0xFFFFD36B, true);
            y += 17;
        }
    }

    public static void registerEvents() {
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, time) -> onChat(message, sender == null ? null : dev.aqua.client.compat.V.profileName(sender)));
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> { if (!overlay) onChat(message, null); });
    }

    private static void onChat(Text message, String sender) {
        if (p() == null) return;
        String text = message.getString(), me = dev.aqua.client.compat.V.profileName(p().getGameProfile());
        for (var m : dev.aqua.client.module.ModuleManager.all()) {
            if (!(m instanceof ChatModule c) || !c.isEnabled()) continue;
            switch (c.kind) {
                case MENTION -> { if (sender != null && !sender.equalsIgnoreCase(me) && text.toLowerCase().contains(me.toLowerCase())) ding(1.6f); }
                case SOUND -> ding(1.2f);
                case LOG -> {
                    try {
                        Files.writeString(FabricLoader.getInstance().getConfigDir().resolve("aquaclient-chat.log"),
                                "[" + LocalTime.now().withNano(0) + "] " + text + System.lineSeparator(), StandardCharsets.UTF_8,
                                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (Exception ignored) {}
                }
                case JOIN -> { if (text.contains(" joined") || text.contains(" зашёл") || text.contains(" присоединился")) ding(1.0f); }
            }
        }
    }

    // =================================================================== типы модулей
    /** Предупреждение по условию: текст по центру экрана и звук при появлении. */
    public static final class Alert extends Module {
        final Supplier<String> check;
        final BoolSetting sound = add(new BoolSetting("Звук", true));
        final boolean blink;
        boolean shown;
        Alert(String name, String desc, Supplier<String> check, boolean blink) { this(name, desc, Category.MISC, check, blink); }
        Alert(String name, String desc, Category cat, Supplier<String> check, boolean blink) {
            super(name, desc, cat);
            this.check = () -> p() == null ? null : check.get();
            this.blink = blink;
            ALERTS.add(this);
        }
    }

    /** Модуль, который держит настройку игры в нужном значении, а при выключении возвращает её. */
    public static final class OptionToggle<T> extends Module {
        private final Function<net.minecraft.client.option.GameOptions, SimpleOption<T>> option;
        private final T value;
        private T saved;
        OptionToggle(String name, String desc, Category cat, Function<net.minecraft.client.option.GameOptions, SimpleOption<T>> option, T value) {
            super(name, desc, cat);
            this.option = option;
            this.value = value;
        }
        @Override protected void onEnable() { SimpleOption<T> o = option.apply(mc.options); saved = o.getValue(); o.setValue(value); }
        @Override public void onTick() { SimpleOption<T> o = option.apply(mc.options); if (!value.equals(o.getValue())) o.setValue(value); }
        @Override protected void onDisable() { if (saved != null) option.apply(mc.options).setValue(saved); saved = null; }
    }

    /** Громкость категории звуков, пока модуль включён. */
    public static final class Volume extends Module {
        private final SoundCategory category;
        private final NumberSetting level;
        private Double saved;
        Volume(String name, String desc, SoundCategory category, double def) {
            super(name, desc, Category.MISC);
            this.category = category;
            level = add(new NumberSetting("Громкость", def * 100, 0, 100, 5, "%"));
        }
        @Override protected void onEnable() { saved = mc.options.getSoundVolumeOption(category).getValue(); }
        @Override public void onTick() {
            var o = mc.options.getSoundVolumeOption(category);
            double v = level.get() / 100.0;
            if (Math.abs(o.getValue() - v) > 0.001) o.setValue(v);
        }
        @Override protected void onDisable() { if (saved != null) mc.options.getSoundVolumeOption(category).setValue(saved); saved = null; }
    }

    enum ChatKind { MENTION, SOUND, LOG, JOIN }

    /** Модули чата (логика в onChat). */
    public static final class ChatModule extends Module {
        final ChatKind kind;
        ChatModule(String name, String desc, ChatKind kind) { super(name, desc, Category.MISC); this.kind = kind; }
    }

    /** Одноразовое действие: включил — сработало — модуль сам выключился. */
    public static final class Action extends Module {
        private final Supplier<String> run;
        Action(String name, String desc, Category cat, Supplier<String> run) { super(name, desc, cat); this.run = run; }
        @Override public void onTick() {
            if (mc.player != null) {
                try { mc.player.sendMessage(Text.literal("§b[Aqua] §f" + run.get()), true); } catch (Exception ignored) {}
            }
            setEnabled(false);
        }
    }

    /** Держит клавишу «нажатой» (красться, идти вперёд). */
    public static final class HoldKey extends Module {
        private final Function<net.minecraft.client.option.GameOptions, net.minecraft.client.option.KeyBinding> key;
        HoldKey(String name, String desc, Category cat, Function<net.minecraft.client.option.GameOptions, net.minecraft.client.option.KeyBinding> key) { super(name, desc, cat); this.key = key; }
        @Override public void onTick() { if (mc.player != null && mc.currentScreen == null) key.apply(mc.options).setPressed(true); }
        @Override protected void onDisable() { key.apply(mc.options).setPressed(false); }
    }

    /** Если FPS проседает ниже цели — плавно уменьшает дальность прорисовки, потом возвращает. */
    public static final class SmartRender extends Module {
        private final NumberSetting target = add(new NumberSetting("Целевой FPS", 60, 30, 144, 5));
        private final NumberSetting min = add(new NumberSetting("Минимум дальности", 6, 2, 16, 1, " ч."));
        private Integer saved;
        private int cool;
        SmartRender() { super("Smart Render Distance", "Сама снижает дальность прорисовки при низком FPS", Category.PERFORMANCE); }
        @Override public void onTick() {
            if (mc.world == null || cool-- > 0) return;
            cool = 60;
            var o = mc.options.getViewDistance();
            if (saved == null) saved = o.getValue();
            int fps = mc.getCurrentFps(), v = o.getValue();
            if (fps < target.get() && v > min.get()) o.setValue(v - 1);
            else if (fps > target.get() * 1.4 && v < saved) o.setValue(v + 1);
        }
        @Override protected void onDisable() { if (saved != null) mc.options.getViewDistance().setValue(saved); saved = null; }
    }

    /**
     * Ping Stabilizer. Сам пинг (расстояние до сервера) ни один мод не уменьшит, но скачки пинга часто бывают,
     * когда канал забит загрузкой чанков. Модуль замечает скачки и на время снижает дальность прорисовки —
     * сервер шлёт меньше данных, соединение разгружается. Когда пинг ровный 15 секунд, дальность возвращается.
     */
    public static final class PingStabilizer extends Module {
        private final NumberSetting high = add(new NumberSetting("Высокий пинг от", 150, 60, 400, 10, " ms"));
        private final NumberSetting minView = add(new NumberSetting("Минимальная дальность", 6, 2, 16, 1, " ч."));
        private final BoolSetting notify = add(new BoolSetting("Сообщать на экране", true));
        private final ArrayDeque<Integer> hist = new ArrayDeque<>();
        private Integer saved;
        private long lastSample, calmSince;
        PingStabilizer() { super("Ping Stabilizer", "Убирает скачки пинга: при лагах разгружает соединение (меньше чанков)", Category.MISC); }

        @Override public void onTick() {
            if (mc.player == null || mc.isInSingleplayer()) return;
            long now = System.currentTimeMillis();
            if (now - lastSample < 1000) return;
            lastSample = now;
            int cur = ping();
            if (cur <= 0) return;
            double avg = hist.isEmpty() ? cur : hist.stream().mapToInt(Integer::intValue).average().orElse(cur);
            hist.addLast(cur); if (hist.size() > 15) hist.removeFirst();
            var o = mc.options.getViewDistance();
            boolean spike = cur > high.get() || (hist.size() > 5 && cur > Math.max(avg * 1.6, avg + 60));
            boolean frozen = lastServerPacket > 0 && now - lastServerPacket > 2500;
            if (spike || frozen) {
                calmSince = now;
                if (o.getValue() > minView.getInt()) {
                    if (saved == null) saved = o.getValue();
                    o.setValue(Math.max(minView.getInt(), o.getValue() - 2));
                    if (notify.isOn()) say(t("Пинг скачет — разгружаю соединение (прорисовка ", "Ping spike — easing the connection (render ") + o.getValue() + ")");
                }
            } else if (saved != null && now - calmSince > 15000) {
                calmSince = now;
                o.setValue(Math.min(saved, o.getValue() + 1));
                if (o.getValue() >= saved) { saved = null; if (notify.isOn()) say(t("Пинг стабилен — прорисовка возвращена", "Ping stable — render distance restored")); }
            }
        }
        private void say(String s) { try { mc.player.sendMessage(Text.literal("§b[Aqua] §f" + s), true); } catch (Exception ignored) {} }
        @Override protected void onDisable() { if (saved != null) mc.options.getViewDistance().setValue(saved); saved = null; hist.clear(); }
    }

    /** Ограничение FPS — меньше нагрев и шум ноутбука. */
    public static final class FpsCap extends Module {
        private final NumberSetting cap = add(new NumberSetting("Предел FPS", 120, 30, 250, 10));
        private Integer saved;
        FpsCap() { super("FPS Limiter", "Ограничивает FPS — тише и холоднее ноутбук", Category.PERFORMANCE); }
        @Override protected void onEnable() { saved = mc.options.getMaxFps().getValue(); }
        @Override public void onTick() { int v = cap.get().intValue(); if (mc.options.getMaxFps().getValue() != v) mc.options.getMaxFps().setValue(v); }
        @Override protected void onDisable() { if (saved != null) mc.options.getMaxFps().setValue(saved); saved = null; }
    }

    /** Без звука, когда окно игры не в фокусе. */
    public static final class BackgroundMute extends Module {
        private Double saved;
        BackgroundMute() { super("Background Mute", "Выключает звук, когда игра свёрнута", Category.PERFORMANCE); }
        @Override public void onTick() {
            var o = mc.options.getSoundVolumeOption(SoundCategory.MASTER);
            if (!mc.isWindowFocused()) { if (saved == null) { saved = o.getValue(); o.setValue(0.0); } }
            else if (saved != null) { o.setValue(saved); saved = null; }
        }
        @Override protected void onDisable() { if (saved != null) mc.options.getSoundVolumeOption(SoundCategory.MASTER).setValue(saved); saved = null; }
    }
}
