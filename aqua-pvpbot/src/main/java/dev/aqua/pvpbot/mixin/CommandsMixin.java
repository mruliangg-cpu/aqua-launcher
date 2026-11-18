package dev.aqua.pvpbot.mixin;

import com.mojang.brigadier.CommandDispatcher;
import dev.aqua.pvpbot.BotCommands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Registers /spawnbot, /kit, /killbots and /pvpbot (no Fabric API needed). */
@Mixin(Commands.class)
public abstract class CommandsMixin {
    @Shadow @Final private CommandDispatcher<CommandSourceStack> dispatcher;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void aquapvp$register(CallbackInfo ci) {
        BotCommands.register(this.dispatcher);
    }
}
