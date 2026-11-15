package dev.aqua.client.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
//#if MC >= 1.21.9
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
//#else
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
//#endif

/** Контекст отрисовки в мире, одинаковый для всех версий Fabric API. */
public record WorldCtx(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
    public static WorldCtx of(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        //#if MC >= 1.21.9
        return new WorldCtx(ctx.matrices(), ctx.consumers(), mc.gameRenderer.getCamera(), mc.getRenderTickCounter().getTickProgress(true));
        //#elseif MC >= 1.21.5
        return new WorldCtx(ctx.matrixStack(), ctx.consumers(), ctx.camera(), ctx.tickCounter().getTickProgress(true));
        //#elseif MC >= 1.21
        return new WorldCtx(ctx.matrixStack(), ctx.consumers(), ctx.camera(), ctx.tickCounter().getTickDelta(true));
        //#else
        return new WorldCtx(ctx.matrixStack(), ctx.consumers(), ctx.camera(), ctx.tickDelta());
        //#endif
    }
}
