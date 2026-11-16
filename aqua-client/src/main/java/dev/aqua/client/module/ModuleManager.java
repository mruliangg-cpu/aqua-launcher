package dev.aqua.client.module;

import dev.aqua.client.module.impl.*;
import dev.aqua.client.util.ClickTracker;
import dev.aqua.client.util.CombatTracker;
import dev.aqua.client.util.Keys;
import dev.aqua.client.util.Lang;
import dev.aqua.client.util.Stats;
import dev.aqua.client.util.SessionStats;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.resource.ResourcePackProfile;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();
    private static final Map<Module, Boolean> KEY_STATE = new HashMap<>();
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    private ModuleManager() {}

    private static MinecraftClient mc() { return MinecraftClient.getInstance(); }

    public static void init() {
        // ---------- HUD ----------
        add(new Watermark(), true);
        add(new TextHudModule("FPS", "Кадры в секунду", Category.HUD, 0.005f, 0.06f,
                () -> mc().getCurrentFps() + " FPS"), true);
        add(new TextHudModule("CPS", "Клики в секунду (левая | правая)", Category.HUD, 0.08f, 0.06f,
                () -> ClickTracker.left() + " | " + ClickTracker.right() + " CPS"), true);
        add(new Keystrokes(), true);
        add(new Coordinates(), false);
        add(new Compass(), false);
        add(new ArmorStatus(), true);
        add(new PotionHud(), true);
        add(new ItemCounter(), false);
        add(new TextHudModule("Ping", "Задержка до сервера", Category.HUD, 0.155f, 0.06f, ModuleManager::ping, () -> "42 ms"), false);
        add(new TextHudModule("Clock", "Реальное время", Category.HUD, 0.9f, 0.008f,
                () -> LocalTime.now().format(CLOCK)), false);
        add(new TextHudModule("Server Address", "IP сервера, на котором вы играете", Category.HUD, 0.8f, 0.06f,
                ModuleManager::server, () -> "mc.hypixel.net"), false);
        add(new TextHudModule("Memory", "Использование оперативной памяти", Category.HUD, 0.8f, 0.12f,
                ModuleManager::memory), false);
        add(new TextHudModule("Speed", "Скорость движения (блоков/с)", Category.HUD, 0.8f, 0.18f,
                () -> mc().player == null ? null : String.format(Lang.t("%.2f м/с", "%.2f m/s"), Stats.speed()), () -> Lang.t("4.32 м/с", "4.32 m/s")), false);
        add(new TextHudModule("Day Counter", "Номер игрового дня", Category.HUD, 0.8f, 0.24f,
                () -> mc().world == null ? null : Lang.t("День ", "Day ") + (mc().world.getTimeOfDay() / 24000L), () -> Lang.t("День 12", "Day 12")), false);
        add(new TextHudModule("Saturation", "Насыщение (скрытая шкала голода)", Category.HUD, 0.8f, 0.36f,
                () -> mc().player == null ? null : String.format(Lang.t("Насыщ.: %.1f", "Sat.: %.1f"), mc().player.getHungerManager().getSaturationLevel()), () -> Lang.t("Насыщ.: 5.0", "Sat.: 5.0")), false);
        add(new TextHudModule("Pack Display", "Активные ресурспаки", Category.HUD, 0.8f, 0.42f,
                ModuleManager::packs, () -> "Aqua 16x"), false);
        add(new Minimap(), false);
        add(new Stopwatch(), false);
        add(new TextHudModule("Light Level", "Уровень освещения под ногами (спавн мобов)", Category.HUD, 0.8f, 0.54f,
                ModuleManager::light, () -> Lang.t("Свет: 7 ☀ 15", "Light: 7 ☀ 15")), false);
        add(new TextHudModule("Entity Counter", "Сколько сущностей вокруг (мобы, предметы)", Category.HUD, 0.8f, 0.6f,
                ModuleManager::entities, () -> "Entities: 128"), false);
        add(new TextHudModule("World Time", "Время в игре и фаза луны", Category.HUD, 0.8f, 0.66f,
                ModuleManager::worldTime, () -> "☀ 12:30"), false);
        // ---------- 20 новых модулей ----------
        add(new NewModules.FpsGraph(), false);
        add(new NewModules.HeldItemInfo(), false);
        add(new NewModules.NearbyPlayers(), false);
        add(new TextHudModule("Biome", "Биом, в котором вы стоите", Category.HUD, 0.8f, 0.72f, ModuleManager::biome, () -> "Biome: Ocean"), false);
        add(new TextHudModule("Dimension", "Текущее измерение", Category.HUD, 0.8f, 0.78f, ModuleManager::dimension, () -> "Overworld"), false);
        add(new TextHudModule("Player Counter", "Сколько игроков на сервере", Category.HUD, 0.8f, 0.84f,
                () -> mc().getNetworkHandler() == null ? null : Lang.t("Игроков: ", "Players: ") + mc().getNetworkHandler().getPlayerList().size(), () -> "Players: 42"), false);
        add(new TextHudModule("Hunger HUD", "Голод и насыщение числами", Category.HUD, 0.55f, 0.88f,
                () -> mc().player == null ? null : String.format("🍗 %d/20 (%.1f)", mc().player.getHungerManager().getFoodLevel(), mc().player.getHungerManager().getSaturationLevel()), () -> "🍗 18/20 (5.0)"), false);
        add(new TextHudModule("XP HUD", "Уровень опыта и прогресс", Category.HUD, 0.55f, 0.94f,
                () -> mc().player == null ? null : Lang.t("Ур. ", "Lvl ") + mc().player.experienceLevel + " (" + Math.round(mc().player.experienceProgress * 100) + "%)", () -> "Lvl 30 (45%)"), false);
        add(new TextHudModule("Fall Distance", "Высота падения (осторожно с уроном)", Category.MOVEMENT, 0.45f, 0.45f,
                () -> mc().player == null || mc().player.fallDistance < 1 ? null : String.format(Lang.t("Падение: %.1f", "Fall: %.1f"), mc().player.fallDistance), () -> "Fall: 6.2"), false);
        add(new TextHudModule("Date", "Сегодняшняя дата", Category.HUD, 0.9f, 0.06f,
                () -> java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))), false);
        add(new TextHudModule("Death Counter", "Смертей за сессию", Category.MISC, 0.8f, 0.9f,
                () -> Lang.t("Смертей: ", "Deaths: ") + SessionStats.deaths, () -> "Deaths: 3"), false);
        add(new TextHudModule("Distance Traveled", "Сколько блоков вы прошли за сессию", Category.MOVEMENT, 0.8f, 0.96f,
                () -> String.format(Lang.t("Пройдено: %.0f м", "Traveled: %.0f m"), SessionStats.distance), () -> "Traveled: 1234 m"), false);
        add(new TextHudModule("Jump Counter", "Прыжков за сессию", Category.MOVEMENT, 0.6f, 0.06f,
                () -> Lang.t("Прыжков: ", "Jumps: ") + SessionStats.jumps, () -> "Jumps: 87"), false);
        add(new TextHudModule("Spawn Distance", "Расстояние до точки спавна мира", Category.HUD, 0.6f, 0.12f, ModuleManager::spawn, () -> "Spawn: 230 m"), false);
        add(new TextHudModule("TPS Meter", "Насколько лагает сервер (20 = идеально)", Category.MISC, 0.6f, 0.18f,
                () -> mc().world == null ? null : String.format("TPS %.1f", SessionStats.tps()), () -> "TPS 19.8"), false);
        add(new TextHudModule("Block Info", "Блок под прицелом и расстояние до него", Category.HUD, 0.45f, 0.38f, ModuleManager::blockInfo, () -> "Diamond Ore · 4.2 m"), false);
        add(new NewModules.Countdown(), false);
        add(new NewModules.NoVignette(), false);
        add(new NewModules.RamCleaner(), false);
        add(new Skin3D(), true);
        add(new TextHudModule("Session Time", "Сколько вы уже в игре", Category.HUD, 0.8f, 0.48f,
                Stats::session, () -> "12:34"), false);

        // ---------- Бой ----------
        add(new TextHudModule("Reach Display", "Дистанция последнего удара", Category.COMBAT, 0.45f, 0.62f,
                () -> CombatTracker.lastReach >= 0 && System.currentTimeMillis() - CombatTracker.lastReachTime < 3000
                        ? String.format(Lang.t("%.2f блока", "%.2f blocks"), CombatTracker.lastReach) : null, () -> Lang.t("3.00 блока", "3.00 blocks")), false);
        add(new TextHudModule("Combo Counter", "Серия ударов без получения урона", Category.COMBAT, 0.45f, 0.68f,
                () -> CombatTracker.combo > 1 ? CombatTracker.combo + Lang.t(" комбо", " combo") : null, () -> Lang.t("5 комбо", "5 combo")), false);
        add(new TargetHud(), false);
        add(new HitParticles(), false);
        add(new Cooldowns(), false);
        add(new NewModules.HealthHud(), false);
        add(new MoreModules.LowHealth(), true);
        add(new MoreModules.DurabilityAlert(), true);

        // ---------- Визуал ----------
        add(new Zoom(), true);
        add(new Freelook(), true);
        add(new Fullbright(), false);
        add(new BlockOutline(), false);
        add(new Crosshair(), false);
        add(new BreakingParticles(), false);
        add(new ShulkerTooltip(), false);
        add(new AppleSkin(), false);
        add(new TimeChanger(), false);
        add(new WeatherChanger(), false);
        add(new LowFire(), false);
        add(new SimpleToggles.NoPumpkin(), false);
        add(new SimpleToggles.HideScoreboard(), false);
        add(new SimpleToggles.NoHurtCam(), false);
        add(new FovChanger(), false);
        add(new Snaplook(), true);
        add(new TntTimer(), true);
        add(new SimpleToggles.HideBossbar(), false);
        add(new SimpleToggles.Hitboxes(), false);
        add(new SimpleToggles.ChunkBorders(), false);
        add(new MoreModules.OwnNametag(), false);

        // ---------- Косметика ----------
        add(new Cosmetics(), true);
        add(new AquaNetwork(), true);

        // ---------- Движение / Разное / FPS ----------
        add(new ToggleSprint(), false);
        add(new AutoGG(), false);
        add(new AutoText(), false);
        add(new MoreModules.AutoRespawn(), false);
        add(new Waypoints(), true);
        add(new SimpleToggles.ChatTimestamps(), false);
        add(new DeathCoords(), false);
        add(new FpsBooster(), true);
        add(new UnfocusedFps(), true);

        // ---------- ещё 100 модулей ----------
        for (Module m : ExtraModules.all()) add(m, false);

        // ---------- 50 PvP-модулей ----------
        for (Module m : PvpModules.all()) add(m, false);

        // ---------- удобные модули ----------
        for (Module m : QolModules.all()) add(m, false);
    }

    private static void add(Module m, boolean enabledByDefault) {
        MODULES.add(m);
        if (enabledByDefault) m.setEnabled(true);
    }

    public static List<Module> all() { return Collections.unmodifiableList(MODULES); }

    public static List<Module> byCategory(Category c) {
        return MODULES.stream().filter(m -> c == null || m.getCategory() == c).collect(Collectors.toList());
    }

    public static List<HudModule> huds() {
        return MODULES.stream().filter(m -> m instanceof HudModule).map(m -> (HudModule) m).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T get(Class<T> type) {
        for (Module m : MODULES) if (type.isInstance(m)) return (T) m;
        return null;
    }

    public static Module byName(String name) {
        for (Module m : MODULES) if (m.getName().equalsIgnoreCase(name)) return m;
        return null;
    }

    public static void tick(MinecraftClient mc) {
        Stats.tick();
        dev.aqua.client.util.SessionStats.tick();
        CombatTracker.tick();
        for (Module m : MODULES) {
            if (m.isEnabled()) {
                try { m.onTick(); } catch (Exception e) { dev.aqua.client.AquaClient.LOG.warn("Module {} tick failed", m.getName(), e); }
            }
            if (m.getKey() > 0) {
                boolean down = mc.currentScreen == null && Keys.isKeyDown(m.getKey());
                boolean was = KEY_STATE.getOrDefault(m, false);
                if (m.isHoldKey()) {
                    // работает, только пока кнопка зажата
                    if (down && !was) m.setEnabled(true);
                    else if (!down && was) m.setEnabled(false);
                } else if (down && !was) {
                    m.toggle();
                    if (mc.player != null) {
                        try {
                            mc.player.sendMessage(net.minecraft.text.Text.literal("§b" + m.getName() + (m.isEnabled() ? Lang.t(" §aвключён", " §aON") : Lang.t(" §cвыключен", " §cOFF"))), true);
                        } catch (Exception ignored) {}
                    }
                }
                KEY_STATE.put(m, down);
            }
        }
    }

    // ---------- источники текста ----------
    private static String ping() {
        MinecraftClient mc = mc();
        if (mc.player == null || mc.getNetworkHandler() == null || mc.isInSingleplayer()) return null;
        PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return e == null ? null : e.getLatency() + " ms";
    }

    private static String server() {
        MinecraftClient mc = mc();
        if (mc.world == null) return null;
        if (mc.isInSingleplayer()) return Lang.t("Одиночная игра", "Singleplayer");
        return mc.getCurrentServerEntry() != null ? dev.aqua.client.module.impl.QolModules.NickHider.maskIp(mc.getCurrentServerEntry().address) : null;
    }

    private static String entities() {
        MinecraftClient mc = mc();
        if (mc.world == null) return null;
        int n = 0;
        for (var ignored : mc.world.getEntities()) n++;
        return Lang.t("Сущностей: ", "Entities: ") + n;
    }

    private static final String[] MOON = {"🌕", "🌖", "🌗", "🌘", "🌑", "🌒", "🌓", "🌔"};

    private static String worldTime() {
        MinecraftClient mc = mc();
        if (mc.world == null) return null;
        long t = mc.world.getTimeOfDay();
        long day = t % 24000;
        int h = (int) ((day / 1000 + 6) % 24), m = (int) (day % 1000 * 60 / 1000);
        boolean night = day >= 13000 && day < 23000;
        String moon = night ? " " + Lang.t("Луна ", "Moon ") + ((t / 24000) % 8 + 1) + "/8" : "";
        return (night ? "☾ " : "☀ ") + String.format("%02d:%02d", h, m) + moon;
    }

    private static String biome() {
        MinecraftClient mc = mc();
        if (mc.player == null || mc.world == null) return null;
        String id = mc.world.getBiome(mc.player.getBlockPos()).getKey().map(k -> k.getValue().getPath()).orElse("?");
        StringBuilder sb = new StringBuilder();
        for (String p : id.split("_")) if (!p.isEmpty()) sb.append(sb.length() > 0 ? " " : "").append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        return Lang.t("Биом: ", "Biome: ") + sb;
    }

    private static String dimension() {
        MinecraftClient mc = mc();
        if (mc.world == null) return null;
        String d = mc.world.getRegistryKey().getValue().getPath();
        return switch (d) {
            case "overworld" -> Lang.t("🌍 Верхний мир", "🌍 Overworld");
            case "the_nether" -> Lang.t("🔥 Незер", "🔥 Nether");
            case "the_end" -> Lang.t("🌌 Край", "🌌 The End");
            default -> d;
        };
    }

    private static String spawn() {
        MinecraftClient mc = mc();
        if (mc.player == null || mc.world == null) return null;
        //#if MC >= 1.21.9
        net.minecraft.util.math.BlockPos sp = net.minecraft.util.math.BlockPos.ORIGIN; // точка спавна больше не передаётся клиенту
        if (true) return null;
        //#else
        var sp = mc.world.getSpawnPos();
        //#endif
        double d = Math.sqrt(mc.player.getBlockPos().getSquaredDistance(sp));
        return String.format(Lang.t("Спавн: %.0f м", "Spawn: %.0f m"), d);
    }

    private static String blockInfo() {
        MinecraftClient mc = mc();
        if (mc.player == null || mc.world == null || !(mc.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult bh)
                || mc.crosshairTarget.getType() != net.minecraft.util.hit.HitResult.Type.BLOCK) return null;
        var state = mc.world.getBlockState(bh.getBlockPos());
        double dist = mc.player.getEyePos().distanceTo(bh.getPos());
        return state.getBlock().getName().getString() + String.format(" · %.1f m", dist);
    }

    private static String light() {
        MinecraftClient mc = mc();
        if (mc.player == null || mc.world == null) return null;
        var pos = mc.player.getBlockPos();
        int block = mc.world.getLightLevel(net.minecraft.world.LightType.BLOCK, pos);
        int sky = mc.world.getLightLevel(net.minecraft.world.LightType.SKY, pos);
        return Lang.t("Свет: ", "Light: ") + block + (block == 0 ? " ⚠" : "") + "  ☀ " + sky;
    }

    private static String memory() {
        Runtime r = Runtime.getRuntime();
        long used = r.totalMemory() - r.freeMemory(), max = r.maxMemory();
        return String.format(Lang.t("RAM %d%% %.1f/%.1fГБ", "RAM %d%% %.1f/%.1fGB"), used * 100 / max, used / 1073741824.0, max / 1073741824.0);
    }

    private static String packs() {
        List<String> names = new ArrayList<>();
        for (ResourcePackProfile p : mc().getResourcePackManager().getEnabledProfiles()) {
            String id = dev.aqua.client.compat.V.packId(p);
            if (id.equals("vanilla") || id.startsWith("fabric") || id.equals("server") || id.contains("programmer")) continue;
            names.add(p.getDisplayName().getString());
        }
        return names.isEmpty() ? null : String.join(", ", names);
    }
}
