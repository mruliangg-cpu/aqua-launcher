package dev.aqua.client.mixin;

import dev.aqua.client.module.impl.Crosshair;
import dev.aqua.client.module.impl.SimpleToggles;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void aqua$crosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (Crosshair.hideVanilla()) ci.cancel();
    }

    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
    private void aqua$pumpkin(DrawContext context, Identifier texture, float opacity, CallbackInfo ci) {
        if (SimpleToggles.NoPumpkin.ACTIVE && texture.getPath().contains("pumpkinblur")) ci.cancel();
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At("HEAD"), cancellable = true)
    private void aqua$scoreboard(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        if (SimpleToggles.HideScoreboard.ACTIVE) ci.cancel();
    }
}
