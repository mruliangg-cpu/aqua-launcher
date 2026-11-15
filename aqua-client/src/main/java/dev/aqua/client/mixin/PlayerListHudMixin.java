package dev.aqua.client.mixin;

import dev.aqua.client.cosmetic.NameTags;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    /** Значок черепахи (и OWNER) в TAB. */
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void aqua$tabIcon(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        Text decorated = NameTags.decorate(entry.getProfile().getId(), entry.getProfile().getName(), cir.getReturnValue());
        if (decorated != cir.getReturnValue()) cir.setReturnValue(decorated);
    }
}
