package dev.aqua.client.module.impl;

import dev.aqua.client.gui.Render2D;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.ModeSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.gui.DrawContext;

public class Crosshair extends Module {
    private static Crosshair instance;
    private final ModeSetting mode = add(new ModeSetting("Вид", "Крест", "Крест", "Точка", "Круг", "Т-образный", "Крест с точкой", "Крестик X", "Квадрат", "Ромб", "Стрелки"));
    private final NumberSetting length = add(new NumberSetting("Длина", 4, 1, 12, 1));
    private final NumberSetting gap = add(new NumberSetting("Отступ", 2, 0, 8, 1));
    private final NumberSetting thickness = add(new NumberSetting("Толщина", 1, 1, 4, 1));
    private final ColorSetting color = add(new ColorSetting("Цвет", 0xFFFFFFFF));
    private final BoolSetting outline = add(new BoolSetting("Обводка", true));
    private final BoolSetting dot = add(new BoolSetting("Точка в центре", false));
    private final BoolSetting thirdPerson = add(new BoolSetting("В третьем лице", false));

    public Crosshair() {
        super("Crosshair", "Свой прицел: крест, точка, круг", Category.RENDER);
        instance = this;
    }

    public static boolean hideVanilla() { return instance != null && instance.isEnabled(); }

    public static void renderHud(DrawContext ctx) {
        if (!hideVanilla()) return;
        var mc = instance.mc;
        if (mc.options.hudHidden || mc.player == null) return;
        if (!mc.options.getPerspective().isFirstPerson() && !instance.thirdPerson.isOn()) return;
        instance.draw(ctx, mc.getWindow().getScaledWidth() / 2, mc.getWindow().getScaledHeight() / 2);
    }

    private void draw(DrawContext c, int cx, int cy) {
        int l = length.getInt(), g = gap.getInt(), t = thickness.getInt(), col = color.color();
        int half = t / 2;
        if (mode.is("Точка")) {
            Render2D.circle(c, cx + 0.5f, cy + 0.5f, t + 1, outline.isOn() ? 0xFF000000 : 0);
            Render2D.circle(c, cx + 0.5f, cy + 0.5f, t, col);
            return;
        }
        if (mode.is("Круг")) {
            int r = g + l;
            for (int i = 0; i < 48; i++) {
                double an = i * Math.PI * 2 / 48;
                float x = cx + (float) Math.cos(an) * r, y = cy + (float) Math.sin(an) * r;
                if (outline.isOn()) Render2D.rect(c, x - 1, y - 1, t + 2, t + 2, 0x90000000);
                Render2D.rect(c, x, y, t, t, col);
            }
            if (dot.isOn()) Render2D.rect(c, cx - half, cy - half, t, t, col);
            return;
        }
        if (mode.is("Квадрат")) {
            int r = g + l;
            bar(c, cx - r, cy - r, 2 * r, t, col, true);              // верх
            bar(c, cx - r, cy + r - t + 1, 2 * r, t, col, true);      // низ
            bar(c, cx - r, cy - r, t, 2 * r, col, true);             // лево
            bar(c, cx + r - t + 1, cy - r, t, 2 * r, col, true);     // право
            if (dot.isOn()) bar(c, cx - half, cy - half, t, t, col, true);
            return;
        }
        if (mode.is("Ромб")) {
            int r = g + l;
            for (int i = 0; i < r; i++) {
                int d = r - i;
                bar(c, cx + i, cy - d, t, t, col, true); bar(c, cx - i, cy - d, t, t, col, true);
                bar(c, cx + i, cy + d, t, t, col, true); bar(c, cx - i, cy + d, t, t, col, true);
            }
            if (dot.isOn()) bar(c, cx - half, cy - half, t, t, col, true);
            return;
        }
        if (mode.is("Крестик X")) {
            for (int i = g; i <= g + l; i++) {
                bar(c, cx + i - half, cy + i - half, t, t, col, true); bar(c, cx - i - half, cy - i - half, t, t, col, true);
                bar(c, cx + i - half, cy - i - half, t, t, col, true); bar(c, cx - i - half, cy + i - half, t, t, col, true);
            }
            if (dot.isOn()) bar(c, cx - half, cy - half, t, t, col, true);
            return;
        }
        if (mode.is("Стрелки")) {  // < > ^ v, остриём к центру
            for (int i = 0; i < l; i++) {
                bar(c, cx - g - l + i - half, cy - (l - i) - half, t, t, col, true); bar(c, cx - g - l + i - half, cy + (l - i) - half, t, t, col, true); // лево
                bar(c, cx + g + l - i - half, cy - (l - i) - half, t, t, col, true); bar(c, cx + g + l - i - half, cy + (l - i) - half, t, t, col, true); // право
            }
            if (dot.isOn()) bar(c, cx - half, cy - half, t, t, col, true);
            return;
        }
        boolean tee = mode.is("Т-образный");
        boolean centerDot = dot.isOn() || mode.is("Крест с точкой");
        bar(c, cx - half, cy - g - l - half, t, l, col, !tee);  // верх
        bar(c, cx - half, cy + g + 1 - half, t, l, col, true);   // низ
        bar(c, cx - g - l - half, cy - half, l, t, col, true);   // лево
        bar(c, cx + g + 1 - half, cy - half, l, t, col, true);   // право
        if (centerDot) bar(c, cx - half, cy - half, t, t, col, true);
    }

    private void bar(DrawContext c, int x, int y, int w, int h, int col, boolean draw) {
        if (!draw) return;
        if (outline.isOn()) c.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xC0000000);
        c.fill(x, y, x + w, y + h, col);
    }
}
