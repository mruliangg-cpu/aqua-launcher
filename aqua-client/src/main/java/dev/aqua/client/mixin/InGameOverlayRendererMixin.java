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
    private static void aqua$lowFireStart(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        float off = LowFire.offset();
        if (off > 0) {
            matrices.push();
            matrices.translate(0, -off, 0);
            aqua$pushed = true;
        }
    }

    @Inject(method = "renderFireOverlay", at = @At("RETURN"))
    private static void aqua$lowFireEnd(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        if (aqua$pushed) {
            matrices.pop();
            aqua$pushed = false;
        }
    }
}
