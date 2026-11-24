package dev.aqua.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Тёмное главное меню в стиле Lunar/Aqua: тёмный фон + свечение, рыбы, водоросли и брендинг. */
public final class AquaMenu {
    private AquaMenu() {}

    private static final int[] FISH = {0xFF3EE6FF, 0xFFFF8A3D, 0xFF5FF2C8, 0xFFFFD86B, 0xFFFF7A8A, 0xFF9FE8FF};

    private static int argb(int rgb, float a) {
        int al = Math.max(0, Math.min(255, (int) (a * 255)));
        return (al << 24) | (rgb & 0xFFFFFF);
    }

    /** Рисуется как самый первый drawable — поверх панорамы, но под кнопками. */
    public static void renderBackground(DrawContext c, int w, int h) {
        long t = System.currentTimeMillis();

        // базовый тёмный градиент — полностью перекрывает панораму и ванильный логотип
        c.fillGradient(0, 0, w, h, 0xFF0B1220, 0xFF060A12);

        // мягкое aqua-свечение сверху по центру
        float gx = w / 2f, gy = h * 0.28f;
        for (int i = 7; i >= 1; i--) Render2D.circle(c, gx, gy, i * 36f, argb(0x1AA7D6, 0.028f));

        // водоросли у дна (покачиваются)
        for (int i = 0; i < 7; i++) {
            float sx = (i + 0.5f) / 7f * w;
            int segs = 7 + (i % 3);
            for (int s = 0; s < segs; s++) {
                float sway = (float) Math.sin(t / 820.0 + i + s * 0.45) * (s * 1.3f);
                float yy = h - 8 - s * 7f;
                Render2D.rect(c, sx + sway, yy, 3, 7, argb(s % 2 == 0 ? 0x1E7B4A : 0x2F9D55, 0.45f));
            }
        }

        // рыбы плавают (под кнопками, как в аквариуме)
        for (int i = 0; i < 6; i++) {
            float sz = 3f + (i % 3);
            int dir = (i % 2 == 0) ? 1 : -1;
            float speed = 17000f + i * 4200f;
            float phase = (float) (((t / speed) + i * 0.19f) % 1.0);
            float travel = w + 100f;
            float x = dir > 0 ? (phase * travel - 50f) : (w + 50f - phase * travel);
            float y = h * 0.22f + i * (h * 0.56f / 6f) + (float) Math.sin(t / 680.0 + i) * 4f;
            fish(c, x, y, sz, FISH[i % FISH.length], dir);
        }

        // поднимающиеся пузырьки
        for (int i = 0; i < 26; i++) {
            float seed = i * 127.1f;
            float x = (float) ((Math.sin(seed) * 0.5 + 0.5) * w);
            float speed = 8000f + (i % 5) * 2600f;
            float frac = (float) (((t / speed) + (seed - (int) seed)) % 1.0);
            Render2D.circle(c, x, h - frac * h, 1.5f + (i % 3), argb(0x3EE6FF, 0.12f));
        }

        // виньетка сверху и снизу
        c.fillGradient(0, 0, w, (int) (h * 0.18f), argb(0x02050A, 0.78f), argb(0x02050A, 0f));
        c.fillGradient(0, (int) (h * 0.82f), w, h, argb(0x02050A, 0f), argb(0x02050A, 0.88f));

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

    /** Простая пиксельная рыбка: тело-кружок, хвост-клин, спинной плавник и глаз. dir=+1 вправо, -1 влево. */
    private static void fish(DrawContext c, float x, float y, float s, int col, int dir) {
        int body = argb(col, 0.95f);
        Render2D.circle(c, x, y, s, body);                               // тело
        float tx = x - dir * s;                                          // сторона хвоста
        for (int i = 1; i <= (int) (s * 1.4f); i++) {                    // хвост-клин
            float hh = i * 0.8f;
            Render2D.rect(c, tx - dir * i, y - hh / 2f, 1.2f, hh, argb(col, 0.85f));
        }
        Render2D.rect(c, x - s * 0.4f, y - s - 1, s * 0.8f, 2, argb(col, 0.7f)); // спинной плавник
        Render2D.rect(c, x + dir * s * 0.45f, y - s * 0.25f, 1.6f, 1.6f, 0xFFFFFFFF); // глаз
    }
}
