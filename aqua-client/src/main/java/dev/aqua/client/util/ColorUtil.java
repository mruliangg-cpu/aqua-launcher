package dev.aqua.client.util;

public final class ColorUtil {
    private ColorUtil() {}

    public static int alpha(int c) { return (c >>> 24) & 0xFF; }
    public static int red(int c) { return (c >> 16) & 0xFF; }
    public static int green(int c) { return (c >> 8) & 0xFF; }
    public static int blue(int c) { return c & 0xFF; }

    public static int argb(int a, int r, int g, int b) {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    /** Умножает альфу цвета (для плавного появления интерфейса). */
    public static int fade(int c, float mul) {
        return argb(Math.round(alpha(c) * Math.max(0, Math.min(1, mul))), red(c), green(c), blue(c));
    }

    /** Для текста: альфа < 4 Minecraft трактует как непрозрачный цвет. */
    public static int textFade(int c, float mul) {
        int a = Math.round(alpha(c) * Math.max(0, Math.min(1, mul)));
        return argb(Math.max(a, 5), red(c), green(c), blue(c));
    }

    public static int withAlpha(int c, int a) { return argb(a, red(c), green(c), blue(c)); }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        return argb(
                Math.round(alpha(a) + (alpha(b) - alpha(a)) * t),
                Math.round(red(a) + (red(b) - red(a)) * t),
                Math.round(green(a) + (green(b) - green(a)) * t),
                Math.round(blue(a) + (blue(b) - blue(a)) * t));
    }

    public static float[] toHsb(int c) {
        float r = red(c) / 255f, g = green(c) / 255f, b = blue(c) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float d = max - min, h = 0;
        if (d != 0) {
            if (max == r) h = ((g - b) / d) % 6;
            else if (max == g) h = (b - r) / d + 2;
            else h = (r - g) / d + 4;
            h /= 6f;
            if (h < 0) h += 1;
        }
        return new float[]{h, max == 0 ? 0 : d / max, max};
    }

    public static int fromHsb(float h, float s, float v, int alpha) {
        h = ((h % 1f) + 1f) % 1f;
        float c = v * s, x = c * (1 - Math.abs((h * 6) % 2 - 1)), m = v - c;
        float r, g, b;
        int i = (int) (h * 6);
        switch (i) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        return argb(alpha, Math.round((r + m) * 255), Math.round((g + m) * 255), Math.round((b + m) * 255));
    }
}
