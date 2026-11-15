package dev.aqua.client.mixin;

import dev.aqua.client.module.impl.NewModules;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class VignetteMixin {
    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true)
    private void aqua$noVignette(DrawContext context, Entity entity, CallbackInfo ci) {
        if (NewModules.NoVignette.ACTIVE) ci.cancel();
    }
}
