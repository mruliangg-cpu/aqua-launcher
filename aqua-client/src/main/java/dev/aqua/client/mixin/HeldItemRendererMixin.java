package dev.aqua.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.aqua.client.module.impl.SmallHands;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Small Hands — масштабирует предмет от первого лица. Безопасно: require=0, на версиях без подходящей сигнатуры просто не применяется. */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), require = 0)
    private void aqua$smallHandsPush(CallbackInfo ci, @Local(argsOnly = true) MatrixStack matrices) {
        float s = SmallHands.scale();
        if (s == 1f) return;
        matrices.push();
        // Масштабируем вокруг точки, где предмет держится в руке (≈ низ-право),
        // чтобы он уменьшался/увеличивался НА МЕСТЕ, а не уезжал в угол (иначе руки трясёт при ходьбе).
        matrices.translate(0.56f * (1f - s), -0.52f * (1f - s), -0.72f * (1f - s));
        matrices.scale(s, s, s);
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"), require = 0)
    private void aqua$smallHandsPop(CallbackInfo ci, @Local(argsOnly = true) MatrixStack matrices) {
        if (SmallHands.scale() == 1f) return;
        matrices.pop();
    }
}
