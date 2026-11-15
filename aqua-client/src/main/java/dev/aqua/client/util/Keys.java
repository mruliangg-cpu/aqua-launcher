package dev.aqua.client.util;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class Keys {
    private Keys() {}

    /** Физически ли зажата клавиша/кнопка мыши, привязанная к KeyBinding. */
    public static boolean isDown(KeyBinding binding) {
        InputUtil.Key key = KeyBindingHelper.getBoundKeyOf(binding);
        return isDown(key);
    }

    public static boolean isDown(InputUtil.Key key) {
        if (key == null || key.equals(InputUtil.UNKNOWN_KEY)) return false;
        long handle = MinecraftClient.getInstance().getWindow().getHandle();
        if (key.getCategory() == InputUtil.Type.MOUSE) return GLFW.glfwGetMouseButton(handle, key.getCode()) == GLFW.GLFW_PRESS;
        return key.getCode() > 0 && InputUtil.isKeyPressed(handle, key.getCode());
    }

    public static boolean isKeyDown(int glfwKey) {
        if (glfwKey <= 0) return false;
        return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), glfwKey);
    }

    public static String name(int glfwKey) {
        if (glfwKey <= 0) return Lang.tr("Нет");
        return InputUtil.fromKeyCode(glfwKey, 0).getLocalizedText().getString().toUpperCase();
    }
}
