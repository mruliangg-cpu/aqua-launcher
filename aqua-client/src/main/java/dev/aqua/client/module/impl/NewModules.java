package dev.aqua.client.module.impl;

import dev.aqua.client.gui.Render2D;
import dev.aqua.client.gui.Theme;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.HudModule;
import dev.aqua.client.module.Module;
import dev.aqua.client.module.TextHudModule;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ButtonSetting;
import dev.aqua.client.setting.KeySetting;
import dev.aqua.client.setting.NumberSetting;
import dev.aqua.client.util.ColorUtil;
import dev.aqua.client.util.Lang;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** 20 новых модулей Aqua Client (часть — текстовые HUD в ModuleManager). */
public final class NewModules {
    private NewModules() {}

    /** График FPS за последние секунды. */
    public static class FpsGraph extends HudModule {
        private final NumberSetting length = add(new NumberSetting("Ширина", 100, 60, 200, 10));
        private final int[] history = new int[200];
        private int idx;
        private long last;

        public FpsGraph() { super("FPS Graph", "График FPS — видно просадки и фризы", Category.HUD, 0.3f, 0.06f); }

        @Override
        protected void draw(DrawContext ctx, boolean editor) {
            long now = System.currentTimeMillis();
            if (now - last > 100) { last = now; history[idx] = mc.getCurrentFps(); idx = (idx + 1) % history.length; }
            int w = length.getInt(), h = 30;
            width = w + 8;
            height = h + 14;
            drawBackground(ctx, width, height);
            int max = 60;
            for (int v : history) max = Math.max(max, v);
            int prevY = -1;
            for (int i = 0; i < w; i++) {
                int v = history[Math.floorMod(idx - w + i, history.length)];
                int y = 4 + h - (int) (v / (float) max * h);
                int col = ColorUtil.lerp(Theme.OFF, Theme.ACCENT, Math.min(1, v / 120f));
                ctx.fill(4 + i, y, 5 + i, 4 + h, ColorUtil.withAlpha(col, 60));
                ctx.fill(4 + i, prevY < 0 ? y : Math.min(y, prevY), 5 + i, (prevY < 0 ? y : Math.max(y, prevY)) + 1, col);
                prevY = y;
            }
            Render2D.textScaled(ctx, mc.getCurrentFps() + " FPS  max " + max, 4, h + 6, 0.7f, textColor.color(), shadow.isOn());
        }
    }

    /** Здоровье и поглощение числами. */
    public static class HealthHud extends HudModule {
        public HealthHud() { super("Health HUD", "Здоровье и золотые сердца числами", Category.COMBAT, 0.45f, 0.82f); }

        @Override
        protected void draw(DrawContext ctx, boolean editor) {
            float hp = mc.player != null ? mc.player.getHealth() : 20, abs = mc.player != null ? mc.player.getAbsorptionAmount() : 4;
            String a = String.format("❤ %.1f", hp / 2), b = abs > 0 ? String.format(" +%.1f", abs / 2) : "";
            width = Render2D.width(a + b) + 10;
            height = 14;
            drawBackground(ctx, width, height);
            int x = text(ctx, a, 5, 3, ColorUtil.lerp(0xFFFF3B5C, 0xFFFF8FA3, hp / 20f));
            if (!b.isEmpty()) text(ctx, b, x - 1, 3, 0xFFFFD23F);
        }
    }

    /** Предмет в руке: название, прочность, чары. */
    public static class HeldItemInfo extends HudModule {
        public HeldItemInfo() { super("Held Item Info", "Название, прочность и чары предмета в руке", Category.HUD, 0.4f, 0.88f); }

        @Override
        protected void draw(DrawContext ctx, boolean editor) {
            ItemStack st = mc.player != null ? mc.player.getMainHandStack() : ItemStack.EMPTY;
            if (st.isEmpty() && !editor) { width = 0; height = 0; return; }
            String name = st.isEmpty() ? "Diamond Sword" : st.getName().getString();
            String sub = st.isEmpty() ? "1561/1561 · 3✦" : (st.isDamageable() ? (st.getMaxDamage() - st.getDamage()) + "/" + st.getMaxDamage() : "×" + st.getCount())
                    + (st.hasEnchantments() ? " · " + dev.aqua.client.compat.V.enchantCount(st) + "✦" : "");
            width = 24 + Math.max(Render2D.width(name), Render2D.width(sub)) + 6;
            height = 22;
            drawBackground(ctx, width, height);
            if (!st.isEmpty()) ctx.drawItem(st, 3, 3);
            text(ctx, name, 23, 3);
            text(ctx, sub, 23, 12, 0xFF9FD8EA);
        }
    }

    /** Игроки рядом с расстоянием. */
    public static class NearbyPlayers extends HudModule {
        private final NumberSetting radius = add(new NumberSetting("Радиус", 64, 16, 256, 16, "m"));

        public NearbyPlayers() { super("Nearby Players", "Список игроков рядом и расстояние до них", Category.HUD, 0.005f, 0.45f); }

        @Override
        protected void draw(DrawContext ctx, boolean editor) {
            List<String> lines = new ArrayList<>();
            if (mc.world != null && mc.player != null) {
                List<PlayerEntity> list = new ArrayList<>(mc.world.getPlayers());
                list.remove(mc.player);
                list.removeIf(p -> p.distanceTo(mc.player) > radius.get());
                list.sort((a, b) -> Float.compare(a.distanceTo(mc.player), b.distanceTo(mc.player)));
                for (PlayerEntity p : list.subList(0, Math.min(8, list.size())))
                    lines.add(p.getName().getString() + " §7" + Math.round(p.distanceTo(mc.player)) + "m");
            }
            if (lines.isEmpty()) { if (!editor) { width = 0; height = 0; return; } lines.add("Steve §712m"); lines.add("Alex §730m"); }
            int w = 0;
            for (String l : lines) w = Math.max(w, Render2D.width(l));
            width = w + 10;
            height = lines.size() * 10 + 6;
            drawBackground(ctx, width, height);
            for (int i = 0; i < lines.size(); i++) text(ctx, lines.get(i), 5, 4 + i * 10);
        }
    }

    /** Обратный отсчёт (например, до конца события). */
    public static class Countdown extends TextHudModule {
        private static long endAt;
        private final NumberSetting minutes = add(new NumberSetting("Минуты", 5, 1, 60, 1));
        private final KeySetting startKey = add(new KeySetting("Старт / стоп", -1));

        public Countdown() {
            super("Countdown", "Таймер обратного отсчёта по клавише", Category.MISC, 0.45f, 0.16f, Countdown::text, () -> "⏳ 04:59");
        }

        private static String text() {
            if (endAt == 0) return null;
            long left = Math.max(0, endAt - System.currentTimeMillis()) / 1000;
            return (left == 0 ? "⌛ " : "⏳ ") + String.format("%02d:%02d", left / 60, left % 60);
        }

        @Override
        public void onTick() {
            if (startKey.pressed()) endAt = endAt == 0 ? System.currentTimeMillis() + (long) (minutes.get() * 60000) : 0;
            if (endAt != 0 && System.currentTimeMillis() > endAt + 5000) endAt = 0;
        }
    }

    /** Убирает затемнение по краям экрана. */
    public static class NoVignette extends Module {
        public static volatile boolean ACTIVE;
        public NoVignette() { super("No Vignette", "Убирает тёмные края экрана", Category.RENDER); }
        @Override protected void onEnable() { ACTIVE = true; }
        @Override protected void onDisable() { ACTIVE = false; }
    }

    /** Освобождает память Java (по кнопке или каждые N минут). */
    public static class RamCleaner extends Module {
        private final BoolSetting auto = add(new BoolSetting("Автоматически", false));
        private final NumberSetting every = add(new NumberSetting("Каждые (мин)", 10, 2, 60, 1), auto::isOn);
        private long last = System.currentTimeMillis();

        public RamCleaner() {
            super("RAM Cleaner", "Очищает неиспользуемую память Java", Category.PERFORMANCE);
            add(new ButtonSetting("Очистить память сейчас", "Очистить", RamCleaner::clean));
        }

        private static void clean() {
            Runtime r = Runtime.getRuntime();
            long before = r.totalMemory() - r.freeMemory();
            System.gc();
            long freed = Math.max(0, before - (r.totalMemory() - r.freeMemory())) / 1048576;
            var mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc.player != null) mc.player.sendMessage(net.minecraft.text.Text.literal("§b[Aqua] §f" + Lang.t("Освобождено ", "Freed ") + freed + " MB"), true);
        }

        @Override
        public void onTick() {
            if (auto.isOn() && System.currentTimeMillis() - last > every.get() * 60000) { last = System.currentTimeMillis(); clean(); }
        }
    }
}
