package dev.aqua.client.mixin;

import dev.aqua.client.module.impl.LowFire;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererMixin {
    @Unique private static boolean aqua$pushed;

    @Inject(method = "renderFireOverlay", at = @At("HEAD"))
    //#if MC >= 1.21.9
    private static void aqua$lowFireStart(MatrixStack matrices, net.minecraft.client.render.VertexConsumerProvider consumers, net.minecraft.client.texture.Sprite sprite, CallbackInfo ci) {
    //#elseif MC >= 1.21.4
    private static void aqua$lowFireStart(MatrixStack matrices, net.minecraft.client.render.VertexConsumerProvider consumers, CallbackInfo ci) {
    //#else
    private static void aqua$lowFireStart(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
    //#endif
        float off = LowFire.offset();
        if (off > 0) {
            matrices.push();
            matrices.translate(0, -off, 0);
            aqua$pushed = true;
        }
    }

    @Inject(method = "renderFireOverlay", at = @At("RETURN"))
    //#if MC >= 1.21.9
    private static void aqua$lowFireEnd(MatrixStack matrices, net.minecraft.client.render.VertexConsumerProvider consumers, net.minecraft.client.texture.Sprite sprite, CallbackInfo ci) {
    //#elseif MC >= 1.21.4
    private static void aqua$lowFireEnd(MatrixStack matrices, net.minecraft.client.render.VertexConsumerProvider consumers, CallbackInfo ci) {
    //#else
    private static void aqua$lowFireEnd(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
    //#endif
        if (aqua$pushed) {
            matrices.pop();
            aqua$pushed = false;
        }
    }
}
