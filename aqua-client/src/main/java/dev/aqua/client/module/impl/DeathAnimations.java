package dev.aqua.client.module.impl;

import dev.aqua.client.gui.Render2D;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.Random;

/**
 * Death Animations (как мод DeathAnimationPlus, но встроено): при смерти поверх экрана
 * смерти проигрывается эффектная 2D-анимация. 10 детальных анимаций, выбор одной в меню.
 * Рисуется через стабильный DrawContext — работает на всех версиях 1.20–1.21.11, без краша.
 */
public class DeathAnimations extends Module {
    private static DeathAnimations instance;
    public static final String[] LIST = {"Взрыв", "Пустота", "Призрак", "Молния", "Заморозка", "Пиксели", "Кровь", "Воронка", "GAME OVER", "Разлом",
            "Наковальня", "Чёрная дыра", "Глитч", "Небеса", "Кислота", "Портал", "Киллкам", "GG"};

    private final ModeSetting anim;

    private static long deathStart;
    private static String chosen = "Взрыв";
    private static final long DURATION = 2300;

    public DeathAnimations() {
        super("Death Animations", "Эффектная анимация при смерти (выбери одну в меню)", Category.RENDER);
        String[] opts = new String[LIST.length + 1];
        opts[0] = "Случайная";
        System.arraycopy(LIST, 0, opts, 1, LIST.length);
        anim = add(new ModeSetting("Анимация смерти", "Взрыв", opts));
        instance = this;
    }

    public static boolean on() { return instance != null && instance.isEnabled(); }

    /** Вызывается при открытии экрана смерти. */
    public static void onDeath() {
        if (!on()) return;
        deathStart = System.currentTimeMillis();
        String a = instance.anim.get();
        chosen = a.equals("Случайная") ? LIST[new Random().nextInt(LIST.length)] : a;
    }

    /** Рисуется поверх экрана смерти каждый кадр. */
    public static void renderOverlay(DrawContext c, int w, int h) {
        if (!on() || deathStart == 0) return;
        float t = (System.currentTimeMillis() - deathStart) / (float) DURATION;
        if (t < 0 || t > 1f) return;
        long seed = deathStart;
        switch (chosen) {
            case "Взрыв"     -> explosion(c, w, h, t, seed);
            case "Пустота"   -> voidAnim(c, w, h, t);
            case "Призрак"   -> ghost(c, w, h, t, seed);
            case "Молния"    -> lightning(c, w, h, t, seed);
            case "Заморозка" -> freeze(c, w, h, t, seed);
            case "Пиксели"   -> pixelate(c, w, h, t, seed);
            case "Кровь"     -> blood(c, w, h, t, seed);
            case "Воронка"   -> whirlpool(c, w, h, t);
            case "GAME OVER" -> gameOver(c, w, h, t);
            case "Разлом"    -> shatter(c, w, h, t, seed);
            case "Наковальня" -> anvil(c, w, h, t, seed);
            case "Чёрная дыра" -> blackHole(c, w, h, t, seed);
            case "Глитч"     -> glitch(c, w, h, t, seed);
            case "Небеса"    -> heaven(c, w, h, t, seed);
            case "Кислота"   -> acid(c, w, h, t, seed);
            case "Портал"    -> portal(c, w, h, t);
            case "Киллкам"   -> killcam(c, w, h, t);
            case "GG"        -> confetti(c, w, h, t, seed);
            default          -> explosion(c, w, h, t, seed);
        }
    }

    // ============================ помощники ============================
    private static int col(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }
    private static float ease(float x) { return 1 - (1 - x) * (1 - x); }         // out-quad
    private static float reveal(float t) { return t < 0.82f ? 1f : Math.max(0f, 1 - (t - 0.82f) / 0.18f); }

    private static void line(DrawContext c, float x1, float y1, float x2, float y2, float th, int color) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        int steps = Math.max(1, (int) (len / 2f));
        for (int i = 0; i <= steps; i++) {
            float f = i / (float) steps, px = x1 + dx * f, py = y1 + dy * f;
            c.fill((int) (px - th), (int) (py - th), (int) (px + th), (int) (py + th), color);
        }
    }
    private static void ring(DrawContext c, float cx, float cy, float r, float th, int color) {
        int steps = Math.max(10, (int) (r * 0.7f));
        for (int i = 0; i < steps; i++) {
            double an = i / (double) steps * Math.PI * 2;
            float px = cx + (float) Math.cos(an) * r, py = cy + (float) Math.sin(an) * r;
            c.fill((int) (px - th), (int) (py - th), (int) (px + th), (int) (py + th), color);
        }
    }

    // ============================ анимации ============================

    private static void explosion(DrawContext c, int w, int h, float t, long seed) {
        float cx = w / 2f, cy = h / 2f;
        if (t < 0.12f) c.fill(0, 0, w, h, col(0xFFFFFF, (0.12f - t) / 0.12f));                 // вспышка
        float rv = reveal(t);
        float r = ease(Math.min(1, t * 1.4f)) * Math.max(w, h) * 0.7f;
        Render2D.circle(c, cx, cy, r * 0.55f, col(interp(0xFFF7B0, 0xD5340B, t), 0.5f * rv));   // огненный шар
        Render2D.circle(c, cx, cy, r * 0.35f, col(0xFFF0A0, 0.6f * rv * (1 - t)));
        ring(c, cx, cy, r, 3f, col(0xFFE08A, 0.8f * rv * (1 - t)));                              // ударная волна
        ring(c, cx, cy, r * 1.25f, 2f, col(0xFF9A3A, 0.5f * rv * (1 - t)));
        Random rr = new Random(seed);
        for (int i = 0; i < 70; i++) {                                                           // угли
            double an = rr.nextDouble() * Math.PI * 2;
            float sp = 0.4f + rr.nextFloat();
            float d = ease(t) * Math.max(w, h) * 0.6f * sp;
            float px = cx + (float) Math.cos(an) * d, py = cy + (float) Math.sin(an) * d + t * t * 120;
            float s = 3f * (1 - t);
            c.fill((int) px, (int) py, (int) (px + s), (int) (py + s), col(interp(0xFFD070, 0x882000, t), (1 - t) * rv));
        }
        c.fill(0, 0, w, h, col(0x1A0300, 0.35f * t * rv));
    }

    private static void voidAnim(DrawContext c, int w, int h, float t) {
        float e = ease(t);
        int band = (int) (Math.min(w, h) * 0.6f * e);
        c.fill(0, 0, w, band, col(0x05010A, 0.96f));                 // тьма с краёв
        c.fill(0, h - band, w, h, col(0x05010A, 0.96f));
        c.fill(0, 0, band, h, col(0x05010A, 0.96f));
        c.fill(w - band, 0, w, h, col(0x05010A, 0.96f));
        float cx = w / 2f, cy = h / 2f;
        ring(c, cx, cy, Math.max(2, (1 - e) * Math.min(w, h) * 0.55f), 2.5f, col(0x8A3BE0, (1 - t) * 0.9f)); // фиолетовая кромка
        ring(c, cx, cy, Math.max(1, (1 - e) * Math.min(w, h) * 0.45f), 1.5f, col(0xC79BFF, (1 - t) * 0.7f));
        float pt = Math.max(0, 1 - e * 1.2f);
        Render2D.circle(c, cx, cy, 2 + pt * 6, col(0xEAD6FF, pt));   // гаснущая точка света
        if (t > 0.7f) c.fill(0, 0, w, h, col(0x05010A, Math.min(0.96f, (t - 0.7f) / 0.3f * 0.96f)));
    }

    private static void ghost(DrawContext c, int w, int h, float t, long seed) {
        c.fillGradient(0, 0, w, h, col(0x0A0C1A, 0.3f + 0.45f * t), col(0x02030A, 0.4f + 0.5f * t));
        float cx = w / 2f + (float) Math.sin(t * 6) * 26;
        float gy = h * (0.85f - t * 0.8f);
        float rv = reveal(t), fade = (1 - t) * rv;
        Render2D.circle(c, cx, gy - 18, 34, col(0xBFE6FF, 0.10f * fade));     // ореол
        Render2D.circle(c, cx, gy - 20, 22, col(0xEAF6FF, 0.5f * fade));      // голова
        c.fill((int) (cx - 24), (int) gy - 20, (int) (cx + 24), (int) (gy + 30), col(0xEAF6FF, 0.42f * fade)); // тело
        Render2D.circle(c, cx - 22, gy + 28, 10, col(0xEAF6FF, 0.42f * fade)); // низ «простыни»
        Render2D.circle(c, cx, gy + 30, 10, col(0xEAF6FF, 0.42f * fade));
        Render2D.circle(c, cx + 22, gy + 28, 10, col(0xEAF6FF, 0.42f * fade));
        c.fill((int) (cx - 10), (int) gy - 24, (int) (cx - 4), (int) gy - 16, col(0x12203A, 0.9f * fade)); // глаза
        c.fill((int) (cx + 4), (int) gy - 24, (int) (cx + 10), (int) gy - 16, col(0x12203A, 0.9f * fade));
        Random rr = new Random(seed);
        for (int i = 0; i < 24; i++) {                                        // душа-искры
            float px = cx + (rr.nextFloat() - 0.5f) * 60;
            float py = gy + 40 - ((t * 1.3f + rr.nextFloat()) % 1f) * h * 0.5f;
            c.fill((int) px, (int) py, (int) px + 2, (int) py + 2, col(0xBFE6FF, 0.6f * fade));
        }
    }

    private static void lightning(DrawContext c, int w, int h, float t, long seed) {
        c.fill(0, 0, w, h, col(0x060814, 0.55f * reveal(t)));
        Random rr = new Random(seed);
        int flashes = 4;
        for (int f = 0; f < flashes; f++) {
            float ft = f / (float) flashes;
            float d = Math.abs(t - ft);
            if (d < 0.05f) {
                float a = (0.05f - d) / 0.05f;
                c.fill(0, 0, w, h, col(0xDDE8FF, a * 0.85f));                 // строб
                float bx = rr.nextFloat() * w;
                float y = 0;
                float x = bx;
                while (y < h) {                                               // зигзаг-молния
                    float nx = x + (rr.nextFloat() - 0.5f) * 70;
                    float ny = y + 30 + rr.nextFloat() * 40;
                    line(c, x, y, nx, Math.min(ny, h), 2.4f, col(0xFFFFFF, a));
                    line(c, x, y, nx, Math.min(ny, h), 5f, col(0x9FC0FF, a * 0.4f));
                    x = nx; y = ny;
                }
            }
        }
        if (t > 0.55f) c.fill(0, 0, w, h, col(0x060814, Math.min(0.8f, (t - 0.55f) / 0.45f) * reveal(t)));
    }

    private static void freeze(DrawContext c, int w, int h, float t, long seed) {
        float e = ease(t), rv = reveal(t);
        c.fillGradient(0, 0, w, h, col(0xBFE4FF, 0.15f * e * rv), col(0x6FA8D8, 0.3f * e * rv));
        Random rr = new Random(seed);
        float[][] corners = {{0, 0}, {w, 0}, {0, h}, {w, h}};
        for (float[] cor : corners) {
            for (int i = 0; i < 26; i++) {
                float ang = rr.nextFloat() * (float) Math.PI * 2;
                float reach = e * Math.max(w, h) * (0.35f + rr.nextFloat() * 0.4f);
                float ex = cor[0] + (float) Math.cos(ang) * reach * (cor[0] == 0 ? 1 : -1) * 0.3f + (cor[0] == 0 ? reach : -reach);
                float ey = cor[1] + (cor[1] == 0 ? reach : -reach) * rr.nextFloat();
                line(c, cor[0], cor[1], ex, ey, 1.2f, col(0xEAF8FF, 0.55f * e * rv));          // иней-иглы
                line(c, cor[0], cor[1], ex * 0.7f + cor[0] * 0.3f, ey, 2f, col(0xCDEBFF, 0.35f * e * rv));
            }
        }
        c.fill(0, 0, w, h, col(0xEAF8FF, 0.12f * e * rv));
        if (t > 0.78f) { float cr = (t - 0.78f) / 0.22f; line(c, 0, h * 0.5f, w, h * 0.42f, 2f, col(0x8FB8DD, cr * 0.6f * rv)); } // трещина
    }

    private static void pixelate(DrawContext c, int w, int h, float t, long seed) {
        int cell = 18;
        int cols = w / cell + 1, rows = h / cell + 1;
        Random rr = new Random(seed);
        for (int gx = 0; gx < cols; gx++) for (int gy = 0; gy < rows; gy++) {
            float delay = rr.nextFloat() * 0.5f;
            float lt = (t - delay) / 0.5f;
            if (lt <= 0) { c.fill(gx * cell, gy * cell, gx * cell + cell - 1, gy * cell + cell - 1, col(0x0A0A0F, 0.9f)); continue; }
            if (lt >= 1) continue;
            float fall = ease(lt) * h * 0.6f;
            int x = gx * cell, y = (int) (gy * cell + fall);
            int shade = 0x203040 + ((gx * 7 + gy * 13) % 40) * 0x010101;
            c.fill(x, y, x + cell - 1, y + cell - 1, col(shade, (1 - lt) * 0.9f));             // падающие блоки
            c.fill(gx * cell, gy * cell, gx * cell + cell - 1, gy * cell + cell - 1, col(0x05050A, Math.min(1, lt) * 0.9f));
        }
    }

    private static void blood(DrawContext c, int w, int h, float t, long seed) {
        float cx = w / 2f, cy = h / 2f, e = ease(Math.min(1, t * 1.5f)), rv = reveal(t);
        Random rr = new Random(seed);
        for (int i = 0; i < 14; i++) {                                                          // брызги из центра
            float ang = rr.nextFloat() * (float) Math.PI * 2, dist = e * Math.max(w, h) * 0.3f * (0.3f + rr.nextFloat());
            float px = cx + (float) Math.cos(ang) * dist, py = cy + (float) Math.sin(ang) * dist;
            Render2D.circle(c, px, py, (6 + rr.nextFloat() * 22) * e, col(0x8A0310, 0.8f * rv));
        }
        Render2D.circle(c, cx, cy, 40 * e, col(0xAE0A16, 0.85f * rv));
        for (int i = 0; i < 16; i++) {                                                          // потёки сверху
            float dx = (i + 0.5f) / 16f * w;
            float drip = ease(Math.max(0, (t - 0.15f) / 0.85f)) * h * (0.3f + rr.nextFloat() * 0.7f);
            c.fill((int) dx - 3, 0, (int) dx + 3, (int) drip, col(0x8A0310, 0.8f * rv));
            Render2D.circle(c, dx, drip, 4, col(0x8A0310, 0.8f * rv));
        }
        c.fill(0, 0, w, h, col(0x3A0006, 0.3f * t * rv));                                        // виньетка
    }

    private static void whirlpool(DrawContext c, int w, int h, float t) {
        c.fillGradient(0, 0, w, h, col(0x071428, 0.4f + 0.4f * t), col(0x02060F, 0.5f + 0.4f * t));
        float cx = w / 2f, cy = h / 2f, rv = reveal(t);
        int arms = 120;
        for (int i = 0; i < arms; i++) {
            float f = i / (float) arms;
            float ang = f * (float) Math.PI * 10 + t * 10;                                       // закрутка
            float rad = (1 - f) * Math.max(w, h) * 0.6f * (1 - t * 0.6f);
            float px = cx + (float) Math.cos(ang) * rad, py = cy + (float) Math.sin(ang) * rad;
            float s = 2.4f * (1 - f);
            c.fill((int) (px - s), (int) (py - s), (int) (px + s), (int) (py + s), col(interp(0x6FC3FF, 0x14325C, f), 0.8f * rv));
        }
        Render2D.circle(c, cx, cy, Math.max(2, 10 * (1 - t)), col(0xDAF0FF, (1 - t) * rv));
    }

    private static void gameOver(DrawContext c, int w, int h, float t) {
        c.fill(0, 0, w, h, col(0x000000, Math.min(0.82f, t * 2) * reveal(t)));
        for (int y = 0; y < h; y += 3) c.fill(0, y, w, y + 1, col(0x000000, 0.25f * reveal(t))); // скан-линии
        float pop = t < 0.25f ? ease(t / 0.25f) : 1f;
        float flick = (t > 0.5f && ((System.currentTimeMillis() / 90) % 2 == 0)) ? 0.65f : 1f;
        float scale = 3.2f * pop;
        String s = "GAME OVER";
        float tw = Render2D.width(s) * scale;
        Render2D.textScaled(c, s, w / 2f - tw / 2f + 2, h / 2f - 18 * scale / 3.2f + 2, scale, col(0x3A0000, 0.9f * reveal(t)), false); // тень
        Render2D.gradientText(c, s, w / 2f - tw / 2f, h / 2f - 18 * scale / 3.2f, scale, 0xFF4D4D, 0xB00020, false, pop * flick * reveal(t));
        String name = MinecraftClient.getInstance().getSession() != null ? MinecraftClient.getInstance().getSession().getUsername() : "Player";
        if (t > 0.35f) Render2D.centered(c, "☠ " + name, w / 2f, h / 2f + 34, col(0xD9A0A0, Math.min(1, (t - 0.35f) / 0.3f) * reveal(t)));
    }

    private static void shatter(DrawContext c, int w, int h, float t, long seed) {
        float cx = w / 2f, cy = h / 2f;
        if (t < 0.1f) c.fill(0, 0, w, h, col(0xFFFFFF, (0.1f - t) / 0.1f));
        Random rr = new Random(seed);
        int cracks = 11;
        float grow = ease(Math.min(1, t * 2.2f));
        for (int i = 0; i < cracks; i++) {                                                       // трещины из центра
            float ang = i / (float) cracks * (float) Math.PI * 2 + rr.nextFloat() * 0.3f;
            float x = cx, y = cy, len = grow * Math.max(w, h) * 0.75f;
            int segs = 6;
            for (int s = 0; s < segs; s++) {
                float nx = x + (float) Math.cos(ang) * (len / segs) + (rr.nextFloat() - 0.5f) * 24;
                float ny = y + (float) Math.sin(ang) * (len / segs) + (rr.nextFloat() - 0.5f) * 24;
                line(c, x, y, nx, ny, 1.6f, col(0xE8F4FF, 0.85f * reveal(t)));
                x = nx; y = ny;
            }
        }
        if (t > 0.5f) {                                                                          // осколки падают
            float ft = (t - 0.5f) / 0.5f;
            int cell = 44;
            for (int gx = 0; gx < w / cell + 1; gx++) for (int gy = 0; gy < h / cell + 1; gy++) {
                float delay = rr.nextFloat() * 0.3f;
                float lt = (ft - delay) / 0.7f;
                if (lt <= 0) continue;
                float fall = ease(Math.min(1, lt)) * h * 0.8f;
                int x = gx * cell, y = (int) (gy * cell + fall);
                c.fill(x, y, x + cell - 2, y + cell - 2, col(0x1A2430, (1 - Math.min(1, lt)) * 0.8f));
                line(c, x, y, x + cell, y, 1f, col(0xBFE0FF, (1 - Math.min(1, lt)) * 0.5f));
            }
        }
    }

    private static void anvil(DrawContext c, int w, int h, float t, long seed) {
        float cx = w / 2f, rv = reveal(t);
        c.fill(0, 0, w, h, col(0x0A0C12, 0.25f * t * rv));
        float dropT = Math.min(1, t / 0.4f);
        float ay = -60 + dropT * dropT * (h / 2f + 40);                                   // падение с ускорением
        int aw = 90, ah = 54;
        c.fill((int) (cx - aw / 2), (int) ay, (int) (cx + aw / 2), (int) (ay + 16), col(0x3A3E46, rv));       // верх наковальни
        c.fill((int) (cx - aw / 2 + 14), (int) (ay + 16), (int) (cx + aw / 2 - 14), (int) (ay + 30), col(0x24272E, rv));
        c.fill((int) (cx - aw / 2 + 22), (int) (ay + 30), (int) (cx + aw / 2 - 22), (int) (ay + ah), col(0x3A3E46, rv));
        if (t >= 0.4f) {                                                                   // удар
            float it = (t - 0.4f) / 0.6f;
            if (it < 0.12f) c.fill(0, 0, w, h, col(0xFFFFFF, (0.12f - it) / 0.12f));
            float impactY = h / 2f + 40;
            for (int i = 0; i < 8; i++) {                                                  // трещины от удара
                float ang = (float) (Math.PI + i / 7.0 * Math.PI);
                line(c, cx, impactY, cx + (float) Math.cos(ang) * ease(it) * 160, impactY + (float) Math.sin(ang) * ease(it) * 60, 1.6f, col(0xBFC6D0, (1 - it) * rv));
            }
            for (int i = 0; i < 10; i++) {                                                 // звёзды вокруг
                double sa = i / 10.0 * Math.PI * 2 + it * 6;
                float sr = 40 + it * 30;
                float sxp = cx + (float) Math.cos(sa) * sr, syp = impactY - 20 + (float) Math.sin(sa) * sr * 0.5f;
                Render2D.textScaled(c, "✦", sxp, syp, 1.3f, col(0xFFE36B, (1 - it) * rv), false);
            }
        }
    }

    private static void blackHole(DrawContext c, int w, int h, float t, long seed) {
        float cx = w / 2f, cy = h / 2f, e = ease(t), rv = reveal(t);
        c.fill(0, 0, w, h, col(0x01010A, 0.5f * e * rv));
        Random rr = new Random(seed);
        for (int i = 0; i < 90; i++) {                                                     // материя закручивается внутрь
            float f = rr.nextFloat();
            float ang = f * (float) Math.PI * 14 + t * 9;
            float rad = (1 - Math.min(1, t * 1.1f)) * Math.max(w, h) * 0.6f * (0.25f + f);
            float px = cx + (float) Math.cos(ang) * rad, py = cy + (float) Math.sin(ang) * rad;
            c.fill((int) px, (int) py, (int) px + 2, (int) py + 2, col(interp(0xFFB060, 0x6A1B9A, f), 0.9f * rv));
        }
        float hr = e * Math.min(w, h) * 0.3f;
        ring(c, cx, cy, hr * 1.25f, 3f, col(0xFF8C3A, (1 - t) * rv));                       // аккреционный диск
        ring(c, cx, cy, hr * 1.1f, 2f, col(0xC24BE0, (1 - t) * rv));
        Render2D.circle(c, cx, cy, hr, col(0x000000, rv));                                 // горизонт событий
    }

    private static void glitch(DrawContext c, int w, int h, float t, long seed) {
        float rv = reveal(t);
        c.fill(0, 0, w, h, col(0x030308, 0.6f * rv));
        Random rr = new Random(seed + (long) (t * 40));                                    // дёргается каждый кадр
        for (int i = 0; i < 18; i++) {                                                     // глитч-полосы
            int gy = rr.nextInt(h), gh = 4 + rr.nextInt(26), off = rr.nextInt(60) - 30;
            int cc = new int[]{0x00E5FF, 0xFF00A8, 0x00FF6A, 0xFFFFFF}[rr.nextInt(4)];
            c.fill(off, gy, w + off, gy + gh, col(cc, 0.18f * rv));
        }
        for (int y = 0; y < h; y += 3) c.fill(0, y, w, y + 1, col(0x000000, 0.3f * rv));   // скан-линии
        float sc = 2.6f;
        String s = "SYSTEM FAILURE";
        float tw = Render2D.width(s) * sc, bx = w / 2f - tw / 2f, by = h / 2f - 14;
        float j = (rr.nextFloat() - 0.5f) * 8;                                             // RGB-сдвиг
        Render2D.textScaled(c, s, bx - 3 + j, by, sc, col(0xFF0040, 0.8f * rv), false);
        Render2D.textScaled(c, s, bx + 3 - j, by, sc, col(0x00E5FF, 0.8f * rv), false);
        Render2D.textScaled(c, s, bx, by, sc, col(0xFFFFFF, 0.95f * rv), false);
    }

    private static void heaven(DrawContext c, int w, int h, float t, long seed) {
        float cx = w / 2f, e = ease(t), rv = reveal(t);
        c.fillGradient(0, 0, w, h, col(0xFFF6D8, 0.6f * e * rv), col(0xFFE9A8, 0.15f * e * rv));
        for (int i = 0; i < 16; i++) {                                                     // золотые лучи сверху
            float lx = (i + 0.5f) / 16f * w;
            line(c, cx, -20, lx, h, 2f + (i % 3), col(0xFFF0B0, 0.12f * e * rv));
        }
        float gy = h * (0.8f - e * 0.65f);                                                 // восходящий силуэт
        Render2D.circle(c, cx, gy - 16, 26, col(0xFFFFFF, 0.18f * rv));
        Render2D.circle(c, cx, gy - 20, 15, col(0xFFFFFF, 0.8f * (1 - t) * rv));
        c.fill((int) (cx - 16), (int) gy - 18, (int) (cx + 16), (int) (gy + 26), col(0xFFFFFF, 0.6f * (1 - t) * rv));
        ring(c, cx, gy - 30, 16, 2f, col(0xFFE36B, (1 - t) * rv));                          // нимб
        Random rr = new Random(seed);
        for (int i = 0; i < 30; i++) {                                                     // золотые частицы вверх
            float px = rr.nextFloat() * w, py = h - ((t * 1.2f + rr.nextFloat()) % 1f) * h;
            c.fill((int) px, (int) py, (int) px + 2, (int) py + 2, col(0xFFE9A8, 0.7f * rv));
        }
        if (t > 0.8f) c.fill(0, 0, w, h, col(0xFFFFFF, (t - 0.8f) / 0.2f * 0.5f));
    }

    private static void acid(DrawContext c, int w, int h, float t, long seed) {
        float e = ease(t), rv = reveal(t);
        Random rr = new Random(seed);
        for (int i = 0; i < 20; i++) {                                                     // потёки кислоты сверху
            float dx = (i + 0.5f) / 20f * w;
            float drip = ease(Math.min(1, t * 1.3f)) * h * (0.3f + rr.nextFloat() * 0.7f);
            c.fill((int) dx - 4, 0, (int) dx + 4, (int) drip, col(0x66FF33, 0.55f * rv));
            Render2D.circle(c, dx, drip, 5, col(0x55DD22, 0.7f * rv));
        }
        int rise = (int) (e * h * 0.9f);
        c.fillGradient(0, h - rise, w, h, col(0x3BBF1A, 0.55f * rv), col(0x173A0C, 0.7f * rv)); // поднимается снизу
        for (int i = 0; i < 26; i++) {                                                     // пузыри
            float bx = rr.nextFloat() * w;
            float by = h - ((t * 1.5f + rr.nextFloat()) % 1f) * rise;
            Render2D.circle(c, bx, by, 2 + rr.nextFloat() * 4, col(0x9FFF6A, 0.5f * rv));
        }
        c.fill(0, 0, w, h, col(0x1A3A08, 0.25f * t * rv));
    }

    private static void portal(DrawContext c, int w, int h, float t) {
        float cx = w / 2f, cy = h / 2f, e = ease(t), rv = reveal(t);
        c.fill(0, 0, w, h, col(0x0A0215, 0.55f * e * rv));
        for (int r = 0; r < 5; r++) {                                                      // вращающиеся кольца портала
            float rad = e * Math.min(w, h) * (0.12f + r * 0.11f);
            int steps = 40 + r * 10;
            for (int i = 0; i < steps; i++) {
                double an = i / (double) steps * Math.PI * 2 + t * (r % 2 == 0 ? 8 : -8);
                float px = cx + (float) Math.cos(an) * rad, py = cy + (float) Math.sin(an) * rad;
                c.fill((int) px, (int) py, (int) px + 2, (int) py + 2, col(interp(0xD17BFF, 0x3A0A66, r / 4f), 0.85f * rv));
            }
        }
        Render2D.circle(c, cx, cy, e * Math.min(w, h) * 0.1f, col(0x160826, 0.9f * rv));
        if (t > 0.65f) {                                                                    // поглощает экран
            float g = (t - 0.65f) / 0.35f;
            Render2D.circle(c, cx, cy, g * Math.max(w, h), col(0x160826, 0.85f * rv));
        }
    }

    private static void killcam(DrawContext c, int w, int h, float t) {
        float cx = w / 2f, cy = h / 2f, rv = reveal(t);
        c.fill(0, 0, w, h, col(0x0A0000, 0.45f * t * rv));
        float conv = 1 - ease(Math.min(1, t / 0.4f));                                       // прицел сходится
        float gap = 60 + conv * 220;
        int red = col(0xFF2A2A, rv), redd = col(0xFF2A2A, 0.5f * rv);
        line(c, cx - gap - 40, cy, cx - gap, cy, 2f, red);
        line(c, cx + gap, cy, cx + gap + 40, cy, 2f, red);
        line(c, cx, cy - gap - 40, cx, cy - gap, 2f, red);
        line(c, cx, cy + gap, cx, cy + gap + 40, 2f, red);
        ring(c, cx, cy, 40 + conv * 120, 2f, redd);
        if (t > 0.42f) {                                                                    // ELIMINATED
            float it = (t - 0.42f) / 0.58f;
            line(c, cx - 26, cy - 26, cx + 26, cy + 26, 3f, col(0xFF2A2A, rv));             // красный крест
            line(c, cx + 26, cy - 26, cx - 26, cy + 26, 3f, col(0xFF2A2A, rv));
            float sc = 2.6f * ease(Math.min(1, it * 2));
            String s = "ELIMINATED";
            float tw = Render2D.width(s) * sc;
            Render2D.textScaled(c, s, cx - tw / 2f, cy + 54, sc, col(0xFF3030, rv), true);
        }
    }

    private static void confetti(DrawContext c, int w, int h, float t, long seed) {
        float rv = reveal(t);
        c.fill(0, 0, w, h, col(0x101024, 0.3f * t * rv));
        Random rr = new Random(seed);
        int[] palette = {0xFF5252, 0xFFD740, 0x69F0AE, 0x40C4FF, 0xE040FB, 0xFFFFFF};
        for (int i = 0; i < 120; i++) {                                                     // конфетти
            float speed = 0.5f + rr.nextFloat();
            float px = (rr.nextFloat() * w + (float) Math.sin(t * 6 + i) * 20) % w;
            float py = ((t * speed + rr.nextFloat()) % 1f) * h;
            int s = 3 + rr.nextInt(4);
            c.fill((int) px, (int) py, (int) px + s, (int) py + s, col(palette[i % palette.length], 0.9f * rv));
        }
        for (int i = 0; i < 6; i++) {                                                       // шарики вверх
            float bx = (i + 0.5f) / 6f * w;
            float by = h - ease(Math.min(1, t * 1.1f)) * h * (0.6f + (i % 3) * 0.15f);
            Render2D.circle(c, bx, by, 14, col(palette[i % palette.length], 0.8f * rv));
            line(c, bx, by + 14, bx, by + 34, 1f, col(0xFFFFFF, 0.5f * rv));
        }
        float sc = 4f * ease(Math.min(1, t * 2.5f));
        String s = "GG";
        float tw = Render2D.width(s) * sc;
        Render2D.gradientText(c, s, w / 2f - tw / 2f, h / 2f - 22, sc, 0x69F0AE, 0x40C4FF, true, rv);
    }

    private static int interp(int from, int to, float t) {
        t = Math.max(0, Math.min(1, t));
        int fr = (from >> 16) & 0xFF, fg = (from >> 8) & 0xFF, fb = from & 0xFF;
        int tr = (to >> 16) & 0xFF, tg = (to >> 8) & 0xFF, tb = to & 0xFF;
        int r = (int) (fr + (tr - fr) * t), g = (int) (fg + (tg - fg) * t), b = (int) (fb + (tb - fb) * t);
        return (r << 16) | (g << 8) | b;
    }
}
