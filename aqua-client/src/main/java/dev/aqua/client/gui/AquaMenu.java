package dev.aqua.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Тёмное главное меню в стиле Lunar: фон вместо панорамы + брендинг Aqua. */
public final class AquaMenu {
    private AquaMenu() {}

    private static int argb(int rgb, float a) {
        int al = Math.max(0, Math.min(255, (int) (a * 255)));
        return (al << 24) | (rgb & 0xFFFFFF);
    }

    /** Рисуется как самый первый drawable — поверх панорамы, но под кнопками. */
    public static void renderBackground(DrawContext c, int w, int h) {
        // базовый тёмный градиент — полностью перекрывает панораму и ванильный логотип
        c.fillGradient(0, 0, w, h, 0xFF0B1220, 0xFF070A12);

        // мягкое aqua-свечение сверху по центру
        float gx = w / 2f, gy = h * 0.30f;
        for (int i = 6; i >= 1; i--) Render2D.circle(c, gx, gy, i * 34f, argb(0x1AA7D6, 0.03f));

        // лёгкая виньетка сверху и снизу
        c.fillGradient(0, 0, w, (int) (h * 0.18f), argb(0x02050A, 0.80f), argb(0x02050A, 0f));
        c.fillGradient(0, (int) (h * 0.82f), w, h, argb(0x02050A, 0f), argb(0x02050A, 0.88f));

        // поднимающиеся пузырьки
        long t = System.currentTimeMillis();
        for (int i = 0; i < 26; i++) {
            float seed = i * 127.1f;
            float x = (float) ((Math.sin(seed) * 0.5 + 0.5) * w);
            float speed = 8000f + (i % 5) * 2600f;
            float frac = (float) (((t / speed) + (seed - (int) seed)) % 1.0);
            float y = h - frac * h;
            Render2D.circle(c, x, y, 1.5f + (i % 3), argb(0x3EE6FF, 0.12f));
        }

        // брендинг
        String title = "AQUA CLIENT";
        float sc = 2.4f;
        float tw = Render2D.width(title) * sc;
        Render2D.gradientText(c, title, w / 2f - tw / 2f, h * 0.13f, sc, 0x3EE6FF, 0x5FF2C8, true, 1f);
        Render2D.centered(c, "PLAY TOGETHER — BE THE BEST", w / 2f, h * 0.13f + 10f * sc + 8f, argb(0x8EC4D8, 0.9f));

        // углы: ник и подпись
        String user = null;
        try { user = MinecraftClient.getInstance().getSession().getUsername(); } catch (Throwable ignored) {}
        if (user != null && !user.isEmpty()) Render2D.text(c, "▸ " + user, 8, h - 14, argb(0x9FE8FF, 0.95f));
        String ver = "Aqua Client";
        Render2D.text(c, ver, w - 8 - Render2D.width(ver), h - 14, argb(0x7FA8D8, 0.9f));
    }
}
