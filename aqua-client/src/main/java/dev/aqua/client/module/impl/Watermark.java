package dev.aqua.client.module.impl;

import dev.aqua.client.gui.Render2D;
import dev.aqua.client.gui.Theme;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.HudModule;
import dev.aqua.client.setting.BoolSetting;
import net.minecraft.client.gui.DrawContext;

public class Watermark extends HudModule {
    private final BoolSetting showFps = add(new BoolSetting("Показывать FPS", true));

    public Watermark() {
        super("Watermark", "Логотип Aqua Client с черепахой", Category.HUD, 0.005f, 0.008f);
    }

    @Override
    protected void draw(DrawContext ctx, boolean editor) {
        String title = "AQUA CLIENT";
        String fps = showFps.isOn() ? "  " + mc.getCurrentFps() + " fps" : "";
        int tw = Render2D.width(title) + Render2D.width(fps);
        width = tw + 24;
        height = 16;
        drawBackground(ctx, width, height);
        drawTurtle(ctx, 4, 4);
        Render2D.gradientText(ctx, title, 16, 4, 1f, Theme.ACCENT, Theme.EYE, shadow.isOn(), 1f);
        if (!fps.isEmpty()) ctx.drawText(mc.textRenderer, fps, 16 + Render2D.width(title), 4, Theme.SUB, shadow.isOn());
    }

    /** Пиксельная черепаха 8×8 — логотип Aqua. */
    public static void drawTurtle(DrawContext c, int x, int y) {
        String[] px = {"..GGGG..", ".GPGGPG.", "HGGGGGG.", "EHGPGGG.", "HHGGGGPG", ".GGGGGG.", ".F....F.", "F......F"};
        for (int j = 0; j < 8; j++)
            for (int i = 0; i < 8; i++) {
                int col = switch (px[j].charAt(i)) {
                    case 'G' -> 0xFF2F7D3A;
                    case 'P' -> 0xFF6FBF5B;
                    case 'H' -> 0xFF6FBF5B;
                    case 'E' -> 0xFF14280F;
                    case 'F' -> 0xFF5AA84A;
                    default -> 0;
                };
                if (col != 0) c.fill(x + i, y + j, x + i + 1, y + j + 1, col);
            }
    }
}
