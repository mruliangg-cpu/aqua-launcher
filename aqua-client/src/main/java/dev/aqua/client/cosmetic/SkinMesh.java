package dev.aqua.client.cosmetic;

import dev.aqua.client.AquaClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 3D-слой скина: каждый непрозрачный пиксель второго слоя (шапка, куртка, рукава, штаны) превращается в объёмный кубик.
 * Пиксели берутся из загруженной текстуры, из адреса скина Mojang или из ресурсов игры; сетка кубиков строится один раз и кэшируется.
 */
public final class SkinMesh {
    /** Кубик в координатах части модели (пиксели), цвет ARGB. */
    public record Box(float x1, float y1, float z1, float x2, float y2, float z2, int color) {}
    public record Mesh(List<Box> head, List<Box> body, List<Box> rightArm, List<Box> leftArm, List<Box> rightLeg, List<Box> leftLeg) {}

    private static final Map<String, Optional<Mesh>> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> LOADING = ConcurrentHashMap.newKeySet();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).followRedirects(HttpClient.Redirect.NORMAL).build();

    private SkinMesh() {}

    /** Готовая сетка или null (пока грузится / нет второго слоя). */
    public static Mesh get(dev.aqua.client.compat.V.Skin s, float thickness) {
        if (s == null) return null;
        boolean slim = s.slim();
        String key = s.texture() + "|" + slim + "|" + thickness;
        Optional<Mesh> m = CACHE.get(key);
        if (m != null) return m.orElse(null);
        if (LOADING.add(key)) load(s, slim, thickness, key);
        return null;
    }

    public static void clear() { CACHE.clear(); }

    private static void load(dev.aqua.client.compat.V.Skin s, boolean slim, float thickness, String key) {
        // 1) уже загруженная текстура с картинкой (скины Aqua, свой скин)
        int[] px = fromTexture(s.texture());
        if (px != null) { finish(key, px, slim, thickness); return; }
        // 2) скин по адресу Mojang — качаем в фоне
        if (s.url() != null) {
            String url = s.url().replace("http://", "https://");
            HTTP.sendAsync(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).build(), HttpResponse.BodyHandlers.ofInputStream())
                    .thenAccept(r -> { try (InputStream in = r.body()) { finish(key, fromStream(in), slim, thickness); } catch (Exception e) { finish(key, null, slim, thickness); } })
                    .exceptionally(e -> { finish(key, null, slim, thickness); return null; });
            return;
        }
        // 3) стандартные скины из ресурсов игры
        try {
            var res = MinecraftClient.getInstance().getResourceManager().getResource(s.texture());
            if (res.isPresent()) { try (InputStream in = res.get().getInputStream()) { finish(key, fromStream(in), slim, thickness); return; } }
        } catch (Exception ignored) {}
        finish(key, null, slim, thickness);
    }

    private static void finish(String key, int[] px, boolean slim, float thickness) {
        Mesh mesh = null;
        try { if (px != null) mesh = build(px, px.length == 64 * 32 ? 32 : 64, slim, thickness); }
        catch (Exception e) { AquaClient.LOG.warn("3D Skins: не удалось построить слой", e); }
        CACHE.put(key, Optional.ofNullable(mesh));
        LOADING.remove(key);
    }

    private static int[] fromTexture(Identifier id) {
        try {
            AbstractTexture t = MinecraftClient.getInstance().getTextureManager().getTexture(id);
            if (t instanceof NativeImageBackedTexture n && n.getImage() != null) {
                NativeImage img = n.getImage();
                int w = img.getWidth(), h = img.getHeight();
                if (w != 64 || (h != 64 && h != 32)) return null;
                int[] out = new int[w * h];
                for (int y = 0; y < h; y++)
                    for (int x = 0; x < w; x++) {
                        int abgr = img.getColor(x, y);
                        out[y * w + x] = (abgr & 0xFF00FF00) | ((abgr & 0xFF) << 16) | ((abgr >> 16) & 0xFF);
                    }
                return out;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static int[] fromStream(InputStream in) throws Exception {
        BufferedImage b = ImageIO.read(in);
        if (b == null || b.getWidth() != 64 || (b.getHeight() != 64 && b.getHeight() != 32)) return null;
        int[] out = new int[64 * b.getHeight()];
        b.getRGB(0, 0, 64, b.getHeight(), out, 0, 64);
        return out;
    }

    // ------------------------------------------------------------------ построение сетки
    private static Mesh build(int[] px, int texH, boolean slim, float t) {
        int aw = slim ? 3 : 4;
        List<Box> head = part(px, texH, 32, 0, 8, 8, 8, -4, -8, -4, 0.5f, t * 1.1f);
        if (texH == 32) return new Mesh(head, List.of(), List.of(), List.of(), List.of(), List.of());
        return new Mesh(head,
                part(px, texH, 16, 32, 8, 12, 4, -4, 0, -2, 0.25f, t),
                part(px, texH, 40, 32, aw, 12, 4, slim ? -2 : -3, -2, -2, 0.25f, t),
                part(px, texH, 48, 48, aw, 12, 4, -1, -2, -2, 0.25f, t),
                part(px, texH, 0, 32, 4, 12, 4, -2, 0, -2, 0.25f, t),
                part(px, texH, 0, 48, 4, 12, 4, -2, 0, -2, 0.25f, t));
    }

    /** Слой одной части: 6 граней, пиксели соседнего одинакового цвета сливаются в полоски. */
    private static List<Box> part(int[] px, int texH, int u, int v, int w, int h, int d, float x, float y, float z, float g, float thick) {
        List<Box> out = new ArrayList<>();
        float x1 = x - g, y1 = y - g, z1 = z - g, x2 = x + w + g, y2 = y + h + g, z2 = z + d + g;
        float sx = (w + 2 * g) / w, sy = (h + 2 * g) / h, sz = (d + 2 * g) / d;
        // грань: origin текселя (0,0), шаг по u, шаг по v, нормаль наружу, прямоугольник текстуры
        face(out, px, texH, u + d, v + d, w, h, new float[]{x1, y1, z1}, new float[]{sx, 0, 0}, new float[]{0, sy, 0}, new float[]{0, 0, -1}, g, thick);          // перед
        face(out, px, texH, u + 2 * d + w, v + d, w, h, new float[]{x2, y1, z2}, new float[]{-sx, 0, 0}, new float[]{0, sy, 0}, new float[]{0, 0, 1}, g, thick); // зад
        face(out, px, texH, u, v + d, d, h, new float[]{x1, y1, z2}, new float[]{0, 0, -sz}, new float[]{0, sy, 0}, new float[]{-1, 0, 0}, g, thick);          // правый бок
        face(out, px, texH, u + d + w, v + d, d, h, new float[]{x2, y1, z1}, new float[]{0, 0, sz}, new float[]{0, sy, 0}, new float[]{1, 0, 0}, g, thick);     // левый бок
        face(out, px, texH, u + d, v, w, d, new float[]{x1, y1, z2}, new float[]{sx, 0, 0}, new float[]{0, 0, -sz}, new float[]{0, -1, 0}, g, thick);          // верх
        face(out, px, texH, u + d + w, v, w, d, new float[]{x1, y2, z2}, new float[]{sx, 0, 0}, new float[]{0, 0, -sz}, new float[]{0, 1, 0}, g, thick);      // низ
        return out;
    }

    private static void face(List<Box> out, int[] px, int texH, int tu, int tv, int tw, int th, float[] p, float[] du, float[] dv, float[] n, float g, float thick) {
        for (int j = 0; j < th; j++) {
            int i = 0;
            while (i < tw) {
                int c = pixel(px, texH, tu + i, tv + j);
                if ((c >>> 24) < 24) { i++; continue; }
                int run = 1;
                while (i + run < tw && pixel(px, texH, tu + i + run, tv + j) == c) run++;
                float[] a = new float[3], b = new float[3];
                for (int k = 0; k < 3; k++) {
                    float s0 = p[k] + du[k] * i + dv[k] * j, s1 = p[k] + du[k] * (i + run) + dv[k] * (j + 1);
                    // толщина: от поверхности тела (−g) наружу на thick
                    float n0 = n[k] * (-g * 0.9f), n1 = n[k] * (thick - g);
                    a[k] = Math.min(s0, s1) + Math.min(n0, n1);
                    b[k] = Math.max(s0, s1) + Math.max(n0, n1);
                    if (n[k] == 0 && Math.abs(b[k] - a[k]) < 1e-4f) b[k] = a[k] + 0.01f;
                }
                int col = (c >>> 24) < 250 ? (0xFF000000 | (c & 0xFFFFFF)) : c;
                out.add(new Box(a[0], a[1], a[2], b[0], b[1], b[2], col));
                i += run;
            }
        }
    }

    private static int pixel(int[] px, int texH, int x, int y) {
        if (x < 0 || x >= 64 || y < 0 || y >= texH) return 0;
        return px[y * 64 + x];
    }
}
