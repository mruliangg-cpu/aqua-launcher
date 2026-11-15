package dev.aqua.client.cosmetic;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/**
 * Рисует 3D-косметику в пространстве модели (единицы — пиксели скина, 1/16 блока).
 * Кубики получают пиксельную фактуру с фаской (bevel.png, левая половина) и вертикальный градиент;
 * «плоские» (пиксель-арт) — чистый цвет (правая белая половина текстуры). Светящиеся части — полная яркость.
 * Крылья — HD-текстуры на пластинах с тёмным слоем позади для объёма.
 */
public final class Voxel {
    private static final int FULL_BRIGHT = LightmapTextureManager.MAX_LIGHT_COORDINATE;
    private final MatrixStack matrices;
    private final RenderLayer boxLayer;
    private VertexConsumer vc;
    private final VertexConsumerProvider consumers;
    private final int light;

    /** Буфер берётся заново перед каждым кубиком: переключение на слой текстуры крыла может закрыть прежний буфер. */
    public Voxel(MatrixStack matrices, RenderLayer boxLayer, VertexConsumerProvider consumers, int light) {
        this.matrices = matrices;
        this.boxLayer = boxLayer;
        this.consumers = consumers;
        this.light = light;
    }

    public MatrixStack matrices() { return matrices; }

    public void box(float x1, float y1, float z1, float x2, float y2, float z2, int argb, boolean glow, boolean flat) {
        x1 /= 16f; y1 /= 16f; z1 /= 16f; x2 /= 16f; y2 /= 16f; z2 /= 16f;
        vc = consumers.getBuffer(boxLayer);
        MatrixStack.Entry e = matrices.peek();
        Matrix4f m = e.getPositionMatrix();
        int a = (argb >>> 24) & 0xFF, r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        if (a == 0) a = 255;
        int lt = glow ? FULL_BRIGHT : light;
        float u0 = flat ? 0.76f : 0f, u1 = flat ? 0.99f : 0.5f;
        float top = flat ? 1f : 1.06f, bottom = flat ? 1f : 0.8f;
        // y в модели направлена вниз: y1 — верх кубика
        quad(e, m, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, 0, -1, 0, r, g, b, a, top, top, lt, u0, u1);
        quad(e, m, x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1, 0, 1, 0, r, g, b, a, bottom, bottom, lt, u0, u1);
        quad(e, m, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, 0, 0, -1, r, g, b, a, top, bottom, lt, u0, u1);
        quad(e, m, x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2, 0, 0, 1, r, g, b, a, top, bottom, lt, u0, u1);
        quad(e, m, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1, -1, 0, 0, r, g, b, a, top, bottom, lt, u0, u1);
        quad(e, m, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, 1, 0, 0, r, g, b, a, top, bottom, lt, u0, u1);
    }

    /** Вершины a,d — верхние, b,c — нижние (для боковых граней). */
    private void quad(MatrixStack.Entry e, Matrix4f m,
                      float ax, float ay, float az, float bx, float by, float bz,
                      float cx, float cy, float cz, float dx, float dy, float dz,
                      float nx, float ny, float nz, int r, int g, int b, int a, float fTop, float fBottom, int lt, float u0, float u1) {
        float shade = ny != 0 ? 1f : nx != 0 ? 0.82f : 0.92f;
        float st = shade * fTop, sb = shade * fBottom;
        v(e, m, ax, ay, az, u0, 0, nx, ny, nz, r, g, b, a, st, lt);
        v(e, m, bx, by, bz, u0, 1, nx, ny, nz, r, g, b, a, sb, lt);
        v(e, m, cx, cy, cz, u1, 1, nx, ny, nz, r, g, b, a, sb, lt);
        v(e, m, dx, dy, dz, u1, 0, nx, ny, nz, r, g, b, a, st, lt);
    }

    private void v(MatrixStack.Entry e, Matrix4f m, float x, float y, float z, float u, float vv,
                   float nx, float ny, float nz, int r, int g, int b, int a, float f, int lt) {
        dev.aqua.client.compat.V.end(dev.aqua.client.compat.V.normal(vc.vertex(m, x, y, z).color(Math.min(255, (int) (r * f)), Math.min(255, (int) (g * f)), Math.min(255, (int) (b * f)), a)
                .texture(u, vv).overlay(OverlayTexture.DEFAULT_UV).light(lt), e, nx, ny, nz));
    }

    /** HD-пластина крыла: основной слой + затемнённый слой позади (видна толщина). Без отсечения — видно с обеих сторон. */
    public void sprite(CosmeticRegistry.Sprite s) {
        VertexConsumer sv = consumers.getBuffer(dev.aqua.client.compat.V.entityTranslucent(s.texture()));
        MatrixStack.Entry e = matrices.peek();
        Matrix4f m = e.getPositionMatrix();
        float x0 = 0, x1 = s.w() / 16f, y1 = s.y1() / 16f, y2 = s.y2() / 16f;
        int lt = s.glow() ? FULL_BRIGHT : light;
        plate(sv, e, m, x0, x1, y1, y2, 0.05f / 16f, 140, lt);
        plate(sv, e, m, x0, x1, y1, y2, 0.3f / 16f, 255, lt);
    }

    private void plate(VertexConsumer sv, MatrixStack.Entry e, Matrix4f m, float x0, float x1, float y1, float y2, float z, int bright, int lt) {
        // лицевая сторона (+z, к зрителю сзади)
        pv(sv, e, m, x0, y1, z, 0, 0, 1, bright, lt);
        pv(sv, e, m, x0, y2, z, 0, 1, 1, bright, lt);
        pv(sv, e, m, x1, y2, z, 1, 1, 1, bright, lt);
        pv(sv, e, m, x1, y1, z, 1, 0, 1, bright, lt);
        // обратная сторона (−z) — тот же рисунок, видно спереди
        pv(sv, e, m, x1, y1, z, 1, 0, -1, bright, lt);
        pv(sv, e, m, x1, y2, z, 1, 1, -1, bright, lt);
        pv(sv, e, m, x0, y2, z, 0, 1, -1, bright, lt);
        pv(sv, e, m, x0, y1, z, 0, 0, -1, bright, lt);
    }

    private void pv(VertexConsumer sv, MatrixStack.Entry e, Matrix4f m, float x, float y, float z, float u, float vv, float nz, int c, int lt) {
        dev.aqua.client.compat.V.end(dev.aqua.client.compat.V.normal(sv.vertex(m, x, y, z).color(c, c, c, 255).texture(u, vv).overlay(OverlayTexture.DEFAULT_UV).light(lt), e, 0, 0, nz));
    }
}
