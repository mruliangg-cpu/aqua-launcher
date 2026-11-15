package dev.aqua.client.mixin;

import dev.aqua.client.module.impl.MoreModules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    //#if MC >= 1.21.2
    @Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z", at = @At("RETURN"), cancellable = true)
    private void aqua$ownLabel(LivingEntity entity, double squaredDistance, CallbackInfoReturnable<Boolean> cir) {
    //#else
    @Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;)Z", at = @At("RETURN"), cancellable = true)
    private void aqua$ownLabel(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
    //#endif
        MinecraftClient mc = MinecraftClient.getInstance();
        if (MoreModules.OwnNametag.ACTIVE && entity == mc.player && !mc.options.getPerspective().isFirstPerson() && !mc.options.hudHidden) cir.setReturnValue(true);
    }
}
