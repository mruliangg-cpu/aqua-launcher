package dev.aqua.client.module.impl;

import dev.aqua.client.gui.Render2D;
import dev.aqua.client.gui.Theme;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.HudModule;
import dev.aqua.client.module.Module;
import dev.aqua.client.module.TextHudModule;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.util.Lang;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Удобные модули (quality of life): компас, направление к спавну, стример-режим и т.д.
 * Всё безопасно для всех версий — только HUD и настройки игры, без читов.
 */
public final class QolModules {
    private QolModules() {}

    /** Стример-режим: прятать свой ник в HUD и над головой. */
    public static boolean hideName;

    private static MinecraftClient mc() { return MinecraftClient.getInstance(); }
    private static ClientPlayerEntity p() { return mc().player; }
    private static String t(String ru, String en) { return Lang.t(ru, en); }

    private static int idx;
    private static TextHudModule hud(String name, String desc, Category cat, Supplier<String> text, String preview) {
        int i = idx++;
        float x = 0.02f + (i % 3) * 0.24f, y = 0.60f + (i / 3 % 8) * 0.05f;
        return new TextHudModule(name, desc, cat, x, y, () -> { try { return p() == null ? null : text.get(); } catch (Exception e) { return null; } }, () -> preview);
    }

    public static List<Module> all() {
        List<Module> L = new ArrayList<>();
        Category H = Category.HUD;

        L.add(new CompassBar());
        L.add(hud("Direction To Spawn", "Угол и расстояние до точки появления мира", H, () -> {
            BlockPos s = dev.aqua.client.compat.V.spawnPos(mc().world);
            double dx = s.getX() + 0.5 - p().getX(), dz = s.getZ() + 0.5 - p().getZ();
            double dist = Math.hypot(dx, dz);
            if (dist < 2) return t("Ты на спавне", "At spawn");
            double ang = MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(-dx, dz)) - p().getYaw());
            return t("Спавн ", "Spawn ") + arrow(ang) + " " + (int) dist + t(" бл.", " bl."); }, "Spawn ↑ 240 bl."));
        L.add(hud("Facing Axis", "В какую сторону ты смотришь (для стройки)", H, () -> {
            float y = MathHelper.wrapDegrees(p().getYaw());
            String a = y >= -45 && y < 45 ? "+Z (" + t("юг", "south") + ")" : y >= 45 && y < 135 ? "-X (" + t("запад", "west") + ")" : y >= 135 || y < -135 ? "-Z (" + t("север", "north") + ")" : "+X (" + t("восток", "east") + ")";
            return t("Смотрю: ", "Facing: ") + a; }, "Facing: +Z (south)"));
        L.add(new SelectedItemName());
        L.add(new NickHider());
        L.add(new AutoSprint());
        L.add(new ClearChatOnJoin());
        return L;
    }

    // =================================================================== помощники
    private static String arrow(double ang) {
        String[] a = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
        return a[(int) Math.round((ang + 360) % 360 / 45.0) % 8];
    }

    // ---------------- компас сверху экрана
    public static final class CompassBar extends HudModule {
        public CompassBar() { super("Compass Bar", "Полоска компаса (С В Ю З) по центру сверху", Category.HUD, 0.5f, 0.04f); }
        @Override protected void draw(DrawContext ctx, boolean editor) {
            width = 160; height = 16;
            drawBackground(ctx, width, height);
            float yaw = p() == null ? 0 : MathHelper.wrapDegrees(p().getYaw());
            // yaw 0 = юг(+Z). Отметки каждые 45°.
            String[] marks = {"С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ"};
            String[] marksEn = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
            int cx = width / 2;
            for (int i = 0; i < 8; i++) {
                float dir = i * 45f - 180f; // север = -180
                float rel = MathHelper.wrapDegrees(dir - yaw);
                if (Math.abs(rel) > 90) continue;
                int x = cx + (int) (rel / 90f * (width / 2 - 8));
                String s = Lang.isRu() ? marks[i] : marksEn[i];
                boolean card = i % 2 == 0;
                Render2D.textScaled(ctx, s, x - Render2D.width(s) * (card ? 0.5f : 0.35f), card ? 3 : 5, card ? 1f : 0.7f, card ? (i == 0 ? 0xFFFF5566 : Theme.TEXT) : 0xFF9FB8C8, true);
            }
            Render2D.rect(ctx, cx, 1, 1, height - 2, Theme.ACCENT);
        }
    }

    // ---------------- имя предмета при смене (следим за стаком в руке, без версиезависимого номера слота)
    public static final class SelectedItemName extends HudModule {
        private ItemStack last = ItemStack.EMPTY;
        private long shown;
        private String name = "";
        public SelectedItemName() { super("Selected Item Name", "Название предмета над хотбаром при смене", Category.HUD, 0.5f, 0.80f); }
        @Override protected void draw(DrawContext ctx, boolean editor) {
            if (p() != null) {
                ItemStack cur = p().getMainHandStack();
                if (cur.getItem() != last.getItem()) { last = cur; if (!cur.isEmpty()) { name = cur.getName().getString(); shown = System.currentTimeMillis(); } }
            }
            long age = System.currentTimeMillis() - shown;
            if (!editor && (age > 2000 || name.isEmpty())) { width = 0; height = 0; return; }
            String s = editor && name.isEmpty() ? "Diamond Sword" : name;
            width = Render2D.width(s) + 10; height = 14;
            int a = editor ? 255 : (int) (255 * Math.min(1, (2000 - age) / 400f));
            drawBackground(ctx, width, height);
            ctx.drawText(mc.textRenderer, s, 5, 3, ((Math.max(a, 20)) << 24) | 0xFFFFFF, true);
        }
    }

    // ---------------- стример-режим (прячет ник/IP в элементах HUD Aqua Client)
    public static final class NickHider extends Module {
        public NickHider() { super("Streamer Mode", "Прячет адрес сервера в HUD — удобно для стримов", Category.MISC); }
        @Override protected void onEnable() { hideName = true; }
        @Override protected void onDisable() { hideName = false; }
        /** Чем заменить свой ник. */
        public static String mask(String real) { return hideName ? "You" : real; }
        /** Спрятать адрес сервера. */
        public static String maskIp(String ip) { return hideName ? "•••••" : ip; }
    }

    // ---------------- авто-спринт
    public static final class AutoSprint extends Module {
        private final BoolSetting always = add(new BoolSetting("Даже стоя на месте", false));
        public AutoSprint() { super("Auto Sprint", "Всегда бежать вперёд без зажатия Ctrl", Category.MOVEMENT); }
        @Override public void onTick() {
            if (mc.player == null || mc.currentScreen != null) return;
            boolean moving = always.isOn() || mc.options.forwardKey.isPressed() || mc.player.forwardSpeed > 0;
            if (moving && !mc.player.horizontalCollision && mc.player.getHungerManager().getFoodLevel() > 6 && !mc.player.isSneaking())
                mc.player.setSprinting(true);
        }
    }

    // ---------------- очистка чата при входе в мир
    public static final class ClearChatOnJoin extends Module {
        private Object lastWorld;
        public ClearChatOnJoin() { super("Clear Chat On Join", "Чистить чат при заходе на сервер или в мир", Category.MISC); }
        @Override public void onTick() {
            if (mc.world != lastWorld) {
                if (lastWorld != null && mc.world != null) mc.inGameHud.getChatHud().clear(false);
                lastWorld = mc.world;
            }
        }
        @Override protected void onDisable() { lastWorld = null; }
    }
}
