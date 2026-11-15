package dev.aqua.client.mixin;

import dev.aqua.client.cosmetic.NameTags;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
    @Unique private static boolean aqua$inLabel;

    @Shadow
    protected abstract void renderLabelIfPresent(AbstractClientPlayerEntity player, Text text, MatrixStack matrices, VertexConsumerProvider consumers, int light, float tickDelta);

    /** Значок черепахи (и OWNER) перед ником над головой. */
    @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V",
            at = @At("HEAD"), cancellable = true)
    private void aqua$nameIcon(AbstractClientPlayerEntity player, Text text, MatrixStack matrices, VertexConsumerProvider consumers, int light, float tickDelta, CallbackInfo ci) {
        if (aqua$inLabel) return;
        Text decorated = NameTags.decorate(player.getGameProfile().getId(), player.getGameProfile().getName(), text);
        if (decorated == text) return;
        aqua$inLabel = true;
        try {
            this.renderLabelIfPresent(player, decorated, matrices, consumers, light, tickDelta);
        } finally {
            aqua$inLabel = false;
        }
        ci.cancel();
    }
}
