package dev.aqua.client.mixin;

import dev.aqua.client.module.impl.Fullbright;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleOption.class)
public abstract class SimpleOptionMixin<T> {
    /** Fullbright: гамма выше 100% без ошибки валидации опции. */
    @SuppressWarnings("unchecked")
    @Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
    private void aqua$gamma(CallbackInfoReturnable<T> cir) {
        if (!Fullbright.ACTIVE) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.options != null && (Object) this == mc.options.getGamma()) {
            cir.setReturnValue((T) (Object) Fullbright.gamma());
        }
    }
}
