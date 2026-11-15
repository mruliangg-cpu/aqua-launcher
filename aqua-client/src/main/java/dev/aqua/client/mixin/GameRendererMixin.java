package dev.aqua.client.mixin;

import dev.aqua.client.module.impl.FovChanger;
import dev.aqua.client.module.impl.Zoom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    /** FOV Changer + Zoom. Только для мира (changingFov), рука остаётся нормальной. */
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    //#if MC >= 1.21.2
    private void aqua$fov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
    //#else
    private void aqua$fov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
    //#endif
        if (!changingFov) return;
        double fov = cir.getReturnValue();
        fov *= FovChanger.factor(MinecraftClient.getInstance().options.getFov().getValue());
        double div = Zoom.fovDivisor();
        if (div != 1.0) fov /= div;
        //#if MC >= 1.21.2
        cir.setReturnValue((float) fov);
        //#else
        cir.setReturnValue(fov);
        //#endif
    }
}
