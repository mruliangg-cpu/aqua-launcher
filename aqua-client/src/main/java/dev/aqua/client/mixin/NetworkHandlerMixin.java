package dev.aqua.client.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Сервер присылает время раз в секунду — по паузам видно зависания (Lag Detector). */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class NetworkHandlerMixin {
    @Inject(method = "onWorldTimeUpdate", at = @At("HEAD"))
    private void aqua$time(CallbackInfo ci) { dev.aqua.client.module.impl.ExtraModules.lastServerPacket = System.currentTimeMillis(); }
}
