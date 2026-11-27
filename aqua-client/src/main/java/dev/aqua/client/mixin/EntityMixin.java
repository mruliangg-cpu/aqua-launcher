package dev.aqua.client.mixin;

import dev.aqua.client.module.impl.Freelook;
import dev.aqua.client.module.impl.GlowingItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void aqua$freelook(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if (Freelook.capturing && (Object) this == MinecraftClient.getInstance().player) {
            Freelook.turn(cursorDeltaX, cursorDeltaY);
            ci.cancel();
        }
    }

    // Glowing Items «На полу» — выпавшие предметы светятся контуром (ванильное свечение).
    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true, require = 0)
    private void aqua$glowFloor(CallbackInfoReturnable<Boolean> cir) {
        if (GlowingItems.floorGlow() && (Object) this instanceof ItemEntity) cir.setReturnValue(true);
    }

    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true, require = 0)
    private void aqua$glowFloorColor(CallbackInfoReturnable<Integer> cir) {
        if (GlowingItems.floorGlow() && (Object) this instanceof ItemEntity) cir.setReturnValue(GlowingItems.color());
    }
}
