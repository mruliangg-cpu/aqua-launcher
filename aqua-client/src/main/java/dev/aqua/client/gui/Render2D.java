package dev.aqua.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Примитивы: скруглённые прямоугольники, круги, масштабируемый текст. */
public final class Render2D {
    private Render2D() {}

    public static void rect(DrawContext c, float x, float y, float w, float h, int color) {
        if (w <= 0 || h <= 0) return;
        c.fill(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h), color);
    }

    public static void roundRect(DrawContext c, float x, float y, float w, float h, float radius, int color) {
        int x1 = Math.round(x), y1 = Math.round(y), x2 = Math.round(x + w), y2 = Math.round(y + h);
        int r = (int) Math.min(radius, Math.min((x2 - x1) / 2f, (y2 - y1) / 2f));
        if (x2 <= x1 || y2 <= y1) return;
        if (r <= 0) { c.fill(x1, y1, x2, y2, color); return; }
        c.fill(x1 + r, y1, x2 - r, y2, color);
        c.fill(x1, y1 + r, x1 + r, y2 - r, color);
        c.fill(x2 - r, y1 + r, x2, y2 - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            c.fill(x1 + inset, y1 + i, x1 + r, y1 + i + 1, color);
            c.fill(x1 + inset, y2 - i - 1, x1 + r, y2 - i, color);
            c.fill(x2 - r, y1 + i, x2 - inset, y1 + i + 1, color);
            c.fill(x2 - r, y2 - i - 1, x2 - inset, y2 - i, color);
        }
    }

    /** Рамка скруглённого прямоугольника (толщина 1). */
    public static void roundOutline(DrawContext c, float x, float y, float w, float h, float r, int color) {
        int x1 = Math.round(x), y1 = Math.round(y), x2 = Math.round(x + w), y2 = Math.round(y + h);
        int rr = Math.round(r);
        c.fill(x1 + rr, y1, x2 - rr, y1 + 1, color);
        c.fill(x1 + rr, y2 - 1, x2 - rr, y2, color);
        c.fill(x1, y1 + rr, x1 + 1, y2 - rr, color);
        c.fill(x2 - 1, y1 + rr, x2, y2 - rr, color);
        if (rr > 0) {
            for (int i = 0; i < rr; i++) {
                double dy = rr - i - 0.5;
                int inset = rr - (int) Math.round(Math.sqrt(rr * rr - dy * dy));
                c.fill(x1 + inset, y1 + i, x1 + inset + 1, y1 + i + 1, color);
                c.fill(x2 - inset - 1, y1 + i, x2 - inset, y1 + i + 1, color);
                c.fill(x1 + inset, y2 - i - 1, x1 + inset + 1, y2 - i, color);
                c.fill(x2 - inset - 1, y2 - i - 1, x2 - inset, y2 - i, color);
            }
        }
    }

    public static void circle(DrawContext c, float cx, float cy, float radius, int color) {
        int r = Math.max(1, Math.round(radius));
        int x = Math.round(cx), y = Math.round(cy);
        for (int dy = -r; dy < r; dy++) {
            double yy = dy + 0.5;
            int half = (int) Math.round(Math.sqrt(r * r - yy * yy));
            if (half > 0) c.fill(x - half, y + dy, x + half, y + dy + 1, color);
        }
    }

    /** Мягкая тень вокруг панели. */
    public static void shadow(DrawContext c, float x, float y, float w, float h, float r, int layers, int color) {
        int a = (color >>> 24) & 0xFF;
        for (int i = layers; i >= 1; i--) {
            int la = Math.max(1, a / (i + 1));
            roundRect(c, x - i * 2, y - i * 2, w + i * 4, h + i * 4, r + i * 2, (la << 24) | (color & 0xFFFFFF));
        }
    }

    public static TextRenderer font() { return MinecraftClient.getInstance().textRenderer; }

    public static void text(DrawContext c, String s, float x, float y, int color) {
        c.drawText(font(), s, Math.round(x), Math.round(y), color, false);
    }

    public static void textShadow(DrawContext c, String s, float x, float y, int color) {
        c.drawText(font(), s, Math.round(x), Math.round(y), color, true);
    }

    public static void textScaled(DrawContext c, String s, float x, float y, float scale, int color, boolean shadow) {
        c.getMatrices().push();
        c.getMatrices().translate(x, y, 0);
        c.getMatrices().scale(scale, scale, 1);
        c.drawText(font(), s, 0, 0, color, shadow);
        c.getMatrices().pop();
    }

    public static void centered(DrawContext c, String s, float cx, float y, int color) {
        c.drawText(font(), s, Math.round(cx - font().getWidth(s) / 2f), Math.round(y), color, false);
    }

    public static int width(String s) { return font().getWidth(s); }

    public static boolean hover(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Текст с градиентом по буквам (для логотипа). */
    public static void gradientText(DrawContext c, String s, float x, float y, float scale, int from, int to, boolean shadow, float alpha) {
        c.getMatrices().push();
        c.getMatrices().translate(x, y, 0);
        c.getMatrices().scale(scale, scale, 1);
        int cx = 0;
        float t0 = (System.currentTimeMillis() % 3000) / 3000f;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            float t = (float) (0.5 + 0.5 * Math.sin((i / (float) Math.max(1, s.length() - 1) + t0) * Math.PI * 2));
            int col = dev.aqua.client.util.ColorUtil.textFade(dev.aqua.client.util.ColorUtil.lerp(from, to, t), alpha);
            c.drawText(font(), ch, cx, 0, col, shadow);
            cx += font().getWidth(ch);
        }
        c.getMatrices().pop();
    }
}
