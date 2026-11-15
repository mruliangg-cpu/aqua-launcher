package dev.aqua.client.mixin;

import dev.aqua.client.module.impl.Zoom;
import dev.aqua.client.util.ClickTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    //#if MC >= 1.21.9
    private void aqua$cps(long window, net.minecraft.client.input.MouseInput input, int action, CallbackInfo ci) {
        int button = input.button();
    //#else
    private void aqua$cps(long window, int button, int action, int mods, CallbackInfo ci) {
    //#endif
        if (action == GLFW.GLFW_PRESS && MinecraftClient.getInstance().currentScreen == null) ClickTracker.click(button);
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void aqua$zoomScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen == null && Zoom.onScroll(vertical)) ci.cancel();
    }
}
