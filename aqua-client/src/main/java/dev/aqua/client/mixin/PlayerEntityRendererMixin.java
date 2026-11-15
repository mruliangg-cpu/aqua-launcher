package dev.aqua.client.mixin;

import dev.aqua.client.cosmetic.NameTags;
import dev.aqua.client.module.impl.Skin3D;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#if MC >= 1.21.9
//#elseif MC >= 1.21.2
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
//#else
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
//#endif

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
    //#if MC >= 1.21.9
    // 1.21.9+: новая система отрисовки игроков — значки у ника и 3D-слой в следующем обновлении
    //#else
    @Inject(method = "renderArm", at = @At("HEAD"))
    private void aqua$armStart(CallbackInfo ci) { Skin3D.FIRST_PERSON.set(true); }

    @Inject(method = "renderArm", at = @At("RETURN"))
    private void aqua$armEnd(CallbackInfo ci) { Skin3D.FIRST_PERSON.set(false); }
    //#endif

    //#if MC >= 1.21.9
    //#elseif MC >= 1.21.2
    /** 1.21.2+: значок у ника, свечение и 3D-слой задаются в состоянии отрисовки игрока. */
    @Inject(method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("TAIL"))
    private void aqua$state(AbstractClientPlayerEntity player, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        Skin3D.hideFlatLayers(player, state);
        if (state.displayName != null)
            state.displayName = NameTags.decorate(player.getGameProfile().getId(), player.getGameProfile().getName(), state.displayName);
    }
    //#else
    @Unique private static boolean aqua$inLabel;

    //#if MC >= 1.20.5
    @Shadow
    protected abstract void renderLabelIfPresent(AbstractClientPlayerEntity player, Text text, MatrixStack matrices, VertexConsumerProvider consumers, int light, float tickDelta);
    //#else
    @Shadow
    protected abstract void renderLabelIfPresent(AbstractClientPlayerEntity player, Text text, MatrixStack matrices, VertexConsumerProvider consumers, int light);
    //#endif

    /** 3D Skins: прячем плоский второй слой, вместо него рисуется объёмный. */
    @Inject(method = "setModelPose", at = @At("TAIL"))
    private void aqua$skin3d(AbstractClientPlayerEntity player, CallbackInfo ci) {
        Skin3D.hideFlatLayers(player, ((PlayerEntityRenderer) (Object) this).getModel());
    }

    /** Значок черепахи (и OWNER) перед ником над головой. */
    //#if MC >= 1.20.5
    @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V",
            at = @At("HEAD"), cancellable = true)
    private void aqua$nameIcon(AbstractClientPlayerEntity player, Text text, MatrixStack matrices, VertexConsumerProvider consumers, int light, float tickDelta, CallbackInfo ci) {
    //#else
    @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"), cancellable = true)
    private void aqua$nameIcon(AbstractClientPlayerEntity player, Text text, MatrixStack matrices, VertexConsumerProvider consumers, int light, CallbackInfo ci) {
    //#endif
        if (aqua$inLabel) return;
        Text decorated = NameTags.decorate(player.getGameProfile().getId(), player.getGameProfile().getName(), text);
        if (decorated == text) return;
        aqua$inLabel = true;
        try {
            //#if MC >= 1.20.5
            this.renderLabelIfPresent(player, decorated, matrices, consumers, light, tickDelta);
            //#else
            this.renderLabelIfPresent(player, decorated, matrices, consumers, light);
            //#endif
        } finally {
            aqua$inLabel = false;
        }
        ci.cancel();
    }
    //#endif
}
