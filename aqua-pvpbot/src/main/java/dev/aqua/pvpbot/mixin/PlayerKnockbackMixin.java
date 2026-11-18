package dev.aqua.pvpbot.mixin;

import dev.aqua.pvpbot.BotPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Real players' hits must knock the bot back (the bot has no client to send velocity to). */
@Mixin(Player.class)
public abstract class PlayerKnockbackMixin {
    @Redirect(method = "causeExtraKnockback",
              at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/Entity;hurtMarked:Z", ordinal = 0))
    private boolean aquapvp$knockBots(Entity target) {
        return target.hurtMarked && !(target instanceof BotPlayer);
    }
}
