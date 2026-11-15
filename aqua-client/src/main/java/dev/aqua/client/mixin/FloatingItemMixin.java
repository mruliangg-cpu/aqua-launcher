package dev.aqua.client.mixin;

import dev.aqua.client.module.impl.PvpModules;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No Totem Animation: не показывать большой тотем на весь экран. */
@Mixin(GameRenderer.class)
public abstract class FloatingItemMixin {
    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    private void aqua$noTotem(ItemStack stack, CallbackInfo ci) {
        if (PvpModules.NoTotemAnimation.hide(stack)) ci.cancel();
    }
}
