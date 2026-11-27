package dev.aqua.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.aqua.client.module.impl.GlowingItems;
import dev.aqua.client.module.impl.SmallHands;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Small Hands (размер/положение/наклон рук) + Glowing Items (подсветка предмета в руке). require=0 для безопасности. */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), require = 0)
    private void aqua$handsPush(CallbackInfo ci, @Local(argsOnly = true) MatrixStack matrices, @Local(argsOnly = true) Hand hand) {
        if (SmallHands.on()) {
            matrices.push();
            SmallHands.transform(matrices, hand == Hand.MAIN_HAND);
        }
        if (GlowingItems.on()) RenderSystem.setShaderColor(GlowingItems.r(), GlowingItems.g(), GlowingItems.b(), 1f);
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"), require = 0)
    private void aqua$handsPop(CallbackInfo ci, @Local(argsOnly = true) MatrixStack matrices) {
        if (GlowingItems.on()) RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        if (SmallHands.on()) matrices.pop();
    }
}
