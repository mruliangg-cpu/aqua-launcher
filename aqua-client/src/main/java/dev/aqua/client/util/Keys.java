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
        return key.getCode() > 0 && GLFW.glfwGetKey(handle, key.getCode()) == GLFW.GLFW_PRESS;
    }

    /** Коды от 1000 — кнопки мыши (1002 = колёсико, 1003/1004 = боковые). */
    public static final int MOUSE = 1000;

    public static boolean isKeyDown(int glfwKey) {
        if (glfwKey <= 0) return false;
        if (glfwKey >= MOUSE) return GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), glfwKey - MOUSE) == GLFW.GLFW_PRESS;
        return GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), glfwKey) == GLFW.GLFW_PRESS;
    }

    public static String name(int glfwKey) {
        if (glfwKey <= 0) return Lang.tr("Нет");
        if (glfwKey >= MOUSE) return Lang.t("МЫШЬ ", "MOUSE ") + (glfwKey - MOUSE + 1);
        return InputUtil.Type.KEYSYM.createFromCode(glfwKey).getLocalizedText().getString().toUpperCase();
    }
}
