package dev.aqua.client.cosmetic;

//#if MC >= 1.21.9
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.21.9+: Minecraft рисует сущности через очередь команд. Косметика пишет вершины как раньше
 * (уже с учётом поворотов частей модели), мы их запоминаем по слоям и отдаём очереди одной командой на слой.
 */
public final class RecordingConsumers implements VertexConsumerProvider {
    private final Map<RenderLayer, Rec> layers = new LinkedHashMap<>();

    @Override
    public VertexConsumer getBuffer(RenderLayer layer) { return layers.computeIfAbsent(layer, l -> new Rec()); }

    public void submit(OrderedRenderCommandQueue queue) {
        if (layers.isEmpty()) return;
        MatrixStack identity = new MatrixStack();
        for (var e : layers.entrySet()) {
            Rec r = e.getValue();
            if (r.data.isEmpty()) continue;
            queue.submitCustom(identity, e.getKey(), (entry, vc) -> r.replay(vc));
        }
    }

    /** Запись вершин: x y z | r g b a | u v | overlay | light | nx ny nz. */
    private static final class Rec implements VertexConsumer {
        final List<float[]> data = new ArrayList<>();
        float[] cur;

        @Override public VertexConsumer vertex(float x, float y, float z) {
            cur = new float[]{x, y, z, 255, 255, 255, 255, 0, 0, 0, 0xF000F0, 0, 1, 0};
            data.add(cur);
            return this;
        }
        @Override public VertexConsumer color(int r, int g, int b, int a) { if (cur != null) { cur[3] = r; cur[4] = g; cur[5] = b; cur[6] = a; } return this; }
        @Override public VertexConsumer texture(float u, float v) { if (cur != null) { cur[7] = u; cur[8] = v; } return this; }
        @Override public VertexConsumer overlay(int u, int v) { if (cur != null) cur[9] = Float.intBitsToFloat((v << 16) | (u & 0xFFFF)); return this; }
        @Override public VertexConsumer light(int u, int v) { if (cur != null) cur[10] = Float.intBitsToFloat((v << 16) | (u & 0xFFFF)); return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { if (cur != null) { cur[11] = x; cur[12] = y; cur[13] = z; } return this; }
        @Override public VertexConsumer color(int argb) { return color((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >>> 24) & 255); }
        public VertexConsumer lineWidth(float w) { return this; }

        void replay(VertexConsumer vc) {
            for (float[] d : data) {
                int ov = Float.floatToRawIntBits(d[9]), lt = Float.floatToRawIntBits(d[10]);
                vc.vertex(d[0], d[1], d[2]).color((int) d[3], (int) d[4], (int) d[5], (int) d[6]).texture(d[7], d[8])
                        .overlay(ov & 0xFFFF, (ov >>> 16) & 0xFFFF).light(lt & 0xFFFF, (lt >>> 16) & 0xFFFF).normal(d[11], d[12], d[13]);
            }
        }
    }
}
//#else
//$$ final class RecordingConsumers {}
//#endif
