package dev.aqua.client.mixin;

import dev.aqua.client.module.impl.Freelook;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Freelook: камера смотрит туда, куда крутит мышь, а не туда, куда смотрит игрок.
 * Надёжный способ без правки внутренностей камеры: на время обновления камеры подменяем
 * углы взгляда игрока на углы Freelook (камера встаёт по ним), сразу после — возвращаем,
 * поэтому тело/движение остаются в прежнем направлении.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method = "update", at = @At("HEAD"))
    private void aqua$freelookHead(CallbackInfo ci) {
        Freelook.frame();
        Freelook.applyToPlayer();
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void aqua$freelookTail(CallbackInfo ci) {
        Freelook.restorePlayer();
    }
}
