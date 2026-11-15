package dev.aqua.client.cosmetic;

import dev.aqua.client.net.AquaNet;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.util.UUID;

/** Рисует 3D-косметику на игроках Aqua Launcher. */
public class CosmeticFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    private static final Identifier BEVEL = dev.aqua.client.compat.V.id("aquaclient", "textures/cosmetics/bevel.png");

    public CosmeticFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, AbstractClientPlayerEntity player,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (player.isInvisible()) return;
        if (dev.aqua.client.module.impl.Skin3D.active())
            dev.aqua.client.module.impl.Skin3D.render(player, getContextModel(), new Voxel(matrices, RenderLayer.getEntityTranslucent(BEVEL), consumers, light));
        UUID id = player.getGameProfile().getId();
        if (!AquaNet.isUser(id)) return;
        String hat = AquaNet.cosmetic(id, Slot.HAT), face = AquaNet.cosmetic(id, Slot.FACE),
                back = AquaNet.cosmetic(id, Slot.BACK), wings = AquaNet.cosmetic(id, Slot.WINGS), pet = AquaNet.cosmetic(id, Slot.PET);
        if (hat.equals("none") && face.equals("none") && back.equals("none") && wings.equals("none") && pet.equals("none")) return;

        Voxel v = new Voxel(matrices, RenderLayer.getEntityTranslucent(BEVEL), consumers, light);
        float t = (player.age + tickDelta) / 20f;
        boolean flying = player.isFallFlying() || player.getAbilities().flying;

        if (!hat.equals("none") || !face.equals("none")) {
            matrices.push();
            getContextModel().head.rotate(matrices);
            CosmeticRenderer.draw(CosmeticRegistry.get(Slot.HAT, hat), v, t, flying);
            CosmeticRenderer.draw(CosmeticRegistry.get(Slot.FACE, face), v, t, flying);
            matrices.pop();
        }
        if (!back.equals("none") || !wings.equals("none")) {
            matrices.push();
            getContextModel().body.rotate(matrices);
            CosmeticRenderer.draw(CosmeticRegistry.get(Slot.BACK, back), v, t, flying);
            CosmeticRenderer.draw(CosmeticRegistry.get(Slot.WINGS, wings), v, t, flying);
            matrices.pop();
        }
        if (!pet.equals("none")) CosmeticRenderer.draw(CosmeticRegistry.get(Slot.PET, pet), v, t, flying);
    }
}
