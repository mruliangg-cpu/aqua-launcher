package dev.aqua.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.aqua.client.module.impl.GlowingItems;
import dev.aqua.client.module.impl.SmallHands;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Small Hands (размер/положение/наклон рук) + Glowing Items (предмет в руке светится — эмиссивно). require=0 для безопасности. */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), require = 0)
    private void aqua$handsPush(CallbackInfo ci, @Local(argsOnly = true) MatrixStack matrices, @Local(argsOnly = true) Hand hand) {
        if (!SmallHands.on()) return;
        matrices.push();
        SmallHands.transform(matrices, hand == Hand.MAIN_HAND);
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"), require = 0)
    private void aqua$handsPop(CallbackInfo ci, @Local(argsOnly = true) MatrixStack matrices) {
        if (SmallHands.on()) matrices.pop();
    }

    // Glowing Items: предмет в руке светится ярко (fullbright) — видно даже в темноте. Кросс-версийно и безопасно.
    @ModifyVariable(method = "renderFirstPersonItem", at = @At("HEAD"), argsOnly = true, require = 0)
    private int aqua$glowLight(int light) {
        return GlowingItems.on() ? 0xF000F0 : light;
    }
}
