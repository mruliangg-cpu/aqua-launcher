package dev.aqua.client.mixin;

import dev.aqua.client.module.impl.Freelook;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    /** Freelook: камера смотрит туда, куда крутит мышь, а не туда, куда смотрит игрок. */
    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 0))
    private void aqua$freelookRotation(Camera camera, float yaw, float pitch) {
        if (Freelook.active) this.setRotation(Freelook.yaw, Freelook.pitch);
        else this.setRotation(yaw, pitch);
    }
}
