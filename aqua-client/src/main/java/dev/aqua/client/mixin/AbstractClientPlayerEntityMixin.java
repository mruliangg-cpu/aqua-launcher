package dev.aqua.client.mixin;

import dev.aqua.client.module.impl.Cosmetics;
import dev.aqua.client.net.AquaNet;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
    /** Плащи Aqua Client и скины из Aqua Launcher — для вас и других игроков лаунчера. */
    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void aqua$skinAndCape(CallbackInfoReturnable<SkinTextures> cir) {
        SkinTextures s = cir.getReturnValue();
        if (s == null) return;
        UUID id = ((AbstractClientPlayerEntity) (Object) this).getGameProfile().getId();
        if (!AquaNet.isUser(id)) return;
        Identifier skin = AquaNet.skin(id);
        Identifier cape = Cosmetics.capeTexture(id);
        if (skin == null && cape == null) return;
        SkinTextures.Model model = skin != null ? ("slim".equals(AquaNet.model(id)) ? SkinTextures.Model.SLIM : SkinTextures.Model.WIDE) : s.model();
        Identifier capeTex = cape != null ? cape : s.capeTexture();
        Identifier elytra = cape != null && Cosmetics.elytra() ? cape : s.elytraTexture();
        cir.setReturnValue(new SkinTextures(skin != null ? skin : s.texture(), skin != null ? null : s.textureUrl(), capeTex, elytra, model, s.secure()));
    }
}
