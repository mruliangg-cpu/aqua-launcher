package dev.aqua.client.mixin;

import dev.aqua.client.module.impl.BreakingParticles;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Частицы при разрушении блока — только если блок действительно сломан. */
@Mixin(ClientPlayerInteractionManager.class)
public class BreakBlockMixin {
    @Inject(method = "breakBlock", at = @At("RETURN"))
    private void aqua$break(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            BreakingParticles.onBreak(pos);
            if (dev.aqua.client.module.impl.PvpModules.miningSound != null && dev.aqua.client.module.impl.PvpModules.miningSound.isEnabled())
                dev.aqua.client.module.impl.PvpModules.miningSound.play();
        }
    }
}
