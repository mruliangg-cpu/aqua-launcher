package dev.aqua.client.mixin;

import dev.aqua.client.module.impl.Cosmetics;
import dev.aqua.client.net.AquaNet;
import net.minecraft.client.network.AbstractClientPlayerEntity;
//#if MC >= 1.20.2 && MC < 1.21.9
import net.minecraft.client.util.SkinTextures;
//#endif
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
    private UUID aqua$id() { return dev.aqua.client.compat.V.profileId(((AbstractClientPlayerEntity) (Object) this).getGameProfile()); }

    //#if MC >= 1.21.9
    // 1.21.9+: скины и плащи Aqua — в следующем обновлении (новая система ассетов скинов)
    //#elseif MC >= 1.20.2
    /** Плащи Aqua Client и скины из Aqua Launcher — для вас и других игроков лаунчера. */
    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void aqua$skinAndCape(CallbackInfoReturnable<SkinTextures> cir) {
        SkinTextures s = cir.getReturnValue();
        if (s == null) return;
        UUID id = aqua$id();
        if (!AquaNet.isUser(id)) return;
        Identifier skin = AquaNet.skin(id);
        Identifier cape = Cosmetics.capeTexture(id);
        if (skin == null && cape == null) return;
        SkinTextures.Model model = skin != null ? ("slim".equals(AquaNet.model(id)) ? SkinTextures.Model.SLIM : SkinTextures.Model.WIDE) : s.model();
        Identifier capeTex = cape != null ? cape : s.capeTexture();
        Identifier elytra = cape != null && Cosmetics.elytra() ? cape : s.elytraTexture();
        cir.setReturnValue(new SkinTextures(skin != null ? skin : s.texture(), skin != null ? null : s.textureUrl(), capeTex, elytra, model, s.secure()));
    }
    //#else
    @Inject(method = "getSkinTexture", at = @At("RETURN"), cancellable = true)
    private void aqua$skin(CallbackInfoReturnable<Identifier> cir) {
        UUID id = aqua$id();
        if (!AquaNet.isUser(id)) return;
        Identifier skin = AquaNet.skin(id);
        if (skin != null) cir.setReturnValue(skin);
    }

    @Inject(method = "getCapeTexture", at = @At("RETURN"), cancellable = true)
    private void aqua$cape(CallbackInfoReturnable<Identifier> cir) {
        UUID id = aqua$id();
        if (!AquaNet.isUser(id)) return;
        Identifier cape = Cosmetics.capeTexture(id);
        if (cape != null) cir.setReturnValue(cape);
    }

    @Inject(method = "canRenderCapeTexture", at = @At("RETURN"), cancellable = true)
    private void aqua$hasCape(CallbackInfoReturnable<Boolean> cir) {
        if (AquaNet.isUser(aqua$id()) && Cosmetics.capeTexture(aqua$id()) != null) cir.setReturnValue(true);
    }

    @Inject(method = "getModel", at = @At("RETURN"), cancellable = true)
    private void aqua$model(CallbackInfoReturnable<String> cir) {
        UUID id = aqua$id();
        if (AquaNet.isUser(id) && AquaNet.skin(id) != null) cir.setReturnValue("slim".equals(AquaNet.model(id)) ? "slim" : "default");
    }
    //#endif
}
