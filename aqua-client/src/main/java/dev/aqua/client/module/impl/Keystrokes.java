package dev.aqua.client.module.impl;

import dev.aqua.client.gui.Render2D;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.HudModule;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.util.Anim;
import dev.aqua.client.util.ClickTracker;
import dev.aqua.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;

public class Keystrokes extends HudModule {
    private final BoolSetting showMouse = add(new BoolSetting("Кнопки мыши", true));
    private final BoolSetting showCps = add(new BoolSetting("CPS на кнопках", true), showMouse::isOn);
    private final BoolSetting showSpace = add(new BoolSetting("Пробел", true));
    private final ColorSetting pressedColor = add(new ColorSetting("Цвет нажатия", 0xD03EE6FF));
    private final ColorSetting pressedText = add(new ColorSetting("Текст при нажатии", 0xFF061A2C));
    private final Anim[] anims = new Anim[7];
    private long lastNs = System.nanoTime();

    public Keystrokes() {
        super("Keystrokes", "Показывает нажатия WASD, мыши и пробела", Category.HUD, 0.005f, 0.12f);
        for (int i = 0; i < anims.length; i++) anims[i] = new Anim(0);
    }

    @Override
    protected void draw(DrawContext ctx, boolean editor) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNs) / 1e9f);
        lastNs = now;
        int k = 22, g = 2, w = k * 3 + g * 2;
        int y = 0;
        key(ctx, 0, mc.options.forwardKey, "W", k + g, y, k, k, dt);
        y += k + g;
        key(ctx, 1, mc.options.leftKey, "A", 0, y, k, k, dt);
        key(ctx, 2, mc.options.backKey, "S", k + g, y, k, k, dt);
        key(ctx, 3, mc.options.rightKey, "D", (k + g) * 2, y, k, k, dt);
        y += k + g;
        if (showMouse.isOn()) {
            int mw = (w - g) / 2;
            key(ctx, 4, mc.options.attackKey, showCps.isOn() ? "LMB|" + ClickTracker.left() : "LMB", 0, y, mw, k, dt);
            key(ctx, 5, mc.options.useKey, showCps.isOn() ? "RMB|" + ClickTracker.right() : "RMB", mw + g, y, w - mw - g, k, dt);
            y += k + g;
        }
        if (showSpace.isOn()) {
            key(ctx, 6, mc.options.jumpKey, "—", 0, y, w, 12, dt);
            y += 12 + g;
        }
        width = w;
        height = y - g;
    }

    private void key(DrawContext ctx, int idx, KeyBinding binding, String label, int x, int y, int w, int h, float dt) {
        Anim a = anims[idx];
        a.to(binding.isPressed() ? 1 : 0);
        float t = a.update(dt, 18);
        int bg = ColorUtil.lerp(background.isOn() ? bgColor.color() : 0x00000000, pressedColor.color(), t);
        Render2D.roundRect(ctx, x, y, w, h, 3, bg);
        int tc = ColorUtil.lerp(textColor.color(), pressedText.color(), t);
        if (label.contains("|")) {
            String[] p = label.split("\\|");
            Render2D.textScaled(ctx, p[0], x + (w - Render2D.width(p[0]) * 0.8f) / 2f, y + 4, 0.8f, tc, false);
            String cps = p[1] + " CPS";
            Render2D.textScaled(ctx, cps, x + (w - Render2D.width(cps) * 0.6f) / 2f, y + 13, 0.6f, tc, false);
        } else {
            ctx.drawText(mc.textRenderer, label, x + (w - Render2D.width(label)) / 2, y + (h - 8) / 2, tc, shadow.isOn() && t < 0.5f);
        }
    }
}
