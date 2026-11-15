package dev.aqua.client.module.impl;

import dev.aqua.client.gui.Render2D;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.HudModule;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

public class Compass extends HudModule {
    private final NumberSetting widthSet = add(new NumberSetting("Ширина", 160, 80, 300, 10));
    private final BoolSetting degrees = add(new BoolSetting("Градусы", true));
    private static final String[] NAMES = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    public Compass() {
        super("Direction HUD", "Компас-полоса как в Lunar", Category.HUD, 0.4f, 0.005f);
    }

    @Override
    protected void draw(DrawContext ctx, boolean editor) {
        int w = widthSet.getInt(), h = degrees.isOn() ? 24 : 16;
        width = w;
        height = h;
        drawBackground(ctx, w, h);
        float yaw = mc.player != null ? mc.player.getYaw() : 0;
        float heading = ((yaw + 180) % 360 + 360) % 360;
        float range = 80;
        for (int deg = 0; deg < 360; deg += 15) {
            float diff = MathHelper.wrapDegrees(deg - heading);
            if (Math.abs(diff) > range) continue;
            float x = w / 2f + diff * (w / 2f) / range;
            float fade = 1f - Math.abs(diff) / range;
            int alpha = (int) (255 * Math.min(1, fade * 1.4f));
            if (Math.abs(x - w / 2f) > w / 2f - 6) continue;
            if (deg % 45 == 0) {
                String n = NAMES[deg / 45];
                int col = n.length() == 1 ? (n.equals("N") ? 0xFF4D7A : accent()) : textColor.color();
                col = (alpha << 24) | (col & 0xFFFFFF);
                if (alpha > 8) ctx.drawText(mc.textRenderer, n, Math.round(x - Render2D.width(n) / 2f), 4, col, shadow.isOn());
            } else {
                if (alpha > 8) Render2D.rect(ctx, x, 6, 1, 4, (alpha << 24) | 0xAAAAAA);
            }
        }
        Render2D.rect(ctx, w / 2f - 0.5f, 0, 1, 3, accent());
        if (degrees.isOn()) {
            String d = Math.round(heading) % 360 + "°";
            ctx.drawText(mc.textRenderer, d, Math.round(w / 2f - Render2D.width(d) / 2f), 14, 0xFFBBBBBB, shadow.isOn());
        }
    }
}
