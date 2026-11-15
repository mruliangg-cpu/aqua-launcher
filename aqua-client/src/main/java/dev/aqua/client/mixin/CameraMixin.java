package dev.aqua.client.mixin;

import dev.aqua.client.module.impl.Freelook;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Freelook: камера смотрит туда, куда крутит мышь, а не туда, куда смотрит игрок.
 * Меняем аргументы первого вызова setRotation(yaw, pitch) в Camera.update — без @Redirect,
 * чтобы не конфликтовать с другими модами, которые тоже меняют камеру.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyArg(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 0), index = 0)
    private float aqua$freelookYaw(float yaw) {
        return Freelook.active ? Freelook.yaw : yaw;
    }

    @ModifyArg(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 0), index = 1)
    private float aqua$freelookPitch(float pitch) {
        return Freelook.active ? Freelook.pitch : pitch;
    }
}
