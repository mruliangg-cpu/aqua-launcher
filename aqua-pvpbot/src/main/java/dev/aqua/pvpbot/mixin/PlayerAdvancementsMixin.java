package dev.aqua.pvpbot.mixin;

import dev.aqua.pvpbot.BotPlayer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bots don't earn advancements (no "HardBot has made the advancement..." chat spam). */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
    @Shadow private ServerPlayer player;

    @Inject(method = "award", at = @At("HEAD"), cancellable = true)
    private void aquapvp$noBotAdvancements(AdvancementHolder advancement, String criterion, CallbackInfoReturnable<Boolean> cir) {
        if (this.player instanceof BotPlayer) cir.setReturnValue(false);
    }
}
