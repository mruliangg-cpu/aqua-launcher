package dev.aqua.client.mixin;

import dev.aqua.client.cosmetic.Slot;
import dev.aqua.client.net.AquaNet;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Крылья вместо элитр. Если у игрока Aqua выбраны крылья-косметика, ваниль-элитры
 * не рисуются — на их месте показываются крылья (их уже рисует CosmeticFeatureRenderer).
 * Крыльев нет — элитры обычные.
 */
@Mixin(ElytraFeatureRenderer.class)
public abstract class ElytraFeatureRendererMixin {

    private static boolean aqua$wingsInstead(AbstractClientPlayerEntity player) {
        if (player == null) return false;
        UUID id = dev.aqua.client.compat.V.profileId(player.getGameProfile());
        return AquaNet.isUser(id) && !AquaNet.cosmetic(id, Slot.WINGS).equals("none");
    }

    //#if MC >= 1.21.2
    private static AbstractClientPlayerEntity aqua$player(int entityId) {
        var world = net.minecraft.client.MinecraftClient.getInstance().world;
        if (world != null && world.getEntityById(entityId) instanceof AbstractClientPlayerEntity p) return p;
        return null;
    }
    //#endif

    //#if MC >= 1.21.9
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void aqua$hideElytra(MatrixStack matrices, net.minecraft.client.render.command.OrderedRenderCommandQueue queue, int light,
                                 net.minecraft.client.render.entity.state.BipedEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        if (aqua$wingsInstead(aqua$player(state.id))) ci.cancel();
    }
    //#elseif MC >= 1.21.2
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void aqua$hideElytra(MatrixStack matrices, net.minecraft.client.render.VertexConsumerProvider consumers, int light,
                                 net.minecraft.client.render.entity.state.BipedEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        if (aqua$wingsInstead(aqua$player(state.id))) ci.cancel();
    }
    //#else
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void aqua$hideElytra(MatrixStack matrices, net.minecraft.client.render.VertexConsumerProvider consumers, int light,
                                 net.minecraft.entity.LivingEntity entity, float limbAngle, float limbDistance, float tickDelta,
                                 float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof AbstractClientPlayerEntity p && aqua$wingsInstead(p)) ci.cancel();
    }
    //#endif
}
