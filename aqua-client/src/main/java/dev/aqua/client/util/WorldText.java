package dev.aqua.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Надписи и лучи в 3D-мире (TNT-таймер, точки маршрута). */
public final class WorldText {
    private WorldText() {}

    public static void label(dev.aqua.client.compat.WorldCtx ctx, Vec3d pos, String text, int color, float scale, boolean throughWalls) {
        MatrixStack ms = ctx.matrices();
        VertexConsumerProvider consumers = ctx.consumers();
        if (ms == null || consumers == null) return;
        Camera cam = ctx.camera();
        Vec3d c = cam.getPos();
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        // чем дальше — тем крупнее, чтобы надпись читалась
        float dist = (float) pos.distanceTo(c);
        float s = 0.025f * scale * Math.max(1f, dist / 8f);
        ms.push();
        ms.translate(pos.x - c.x, pos.y - c.y, pos.z - c.z);
        ms.multiply(cam.getRotation());
        ms.scale(s, -s, s);
        Matrix4f m = ms.peek().getPositionMatrix();
        float x = -tr.getWidth(text) / 2f;
        tr.draw(text, x, 0, color, false, m, consumers,
                throughWalls ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL,
                0x7008243A, LightmapTextureManager.MAX_LIGHT_COORDINATE);
        ms.pop();
    }

    /** Вертикальный луч от точки вверх (как у маяка). */
    public static void beam(dev.aqua.client.compat.WorldCtx ctx, Vec3d pos, int argb, double height) {
        MatrixStack ms = ctx.matrices();
        VertexConsumerProvider consumers = ctx.consumers();
        if (ms == null || consumers == null) return;
        Vec3d c = ctx.camera().getPos();
        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry e = ms.peek();
        float r = ColorUtil.red(argb) / 255f, g = ColorUtil.green(argb) / 255f, b = ColorUtil.blue(argb) / 255f;
        float x = (float) (pos.x - c.x), y = (float) (pos.y - c.y), z = (float) (pos.z - c.z);
        dev.aqua.client.compat.V.end(dev.aqua.client.compat.V.normal(lines.vertex(e.getPositionMatrix(), x, y, z).color(r, g, b, 1f), e, 0, 1, 0));
        dev.aqua.client.compat.V.end(dev.aqua.client.compat.V.normal(lines.vertex(e.getPositionMatrix(), x, (float) (y + height), z).color(r, g, b, 0.1f), e, 0, 1, 0));
    }

    public static void flush(dev.aqua.client.compat.WorldCtx ctx) {
        if (ctx.consumers() instanceof VertexConsumerProvider.Immediate imm) imm.draw();
    }
}
