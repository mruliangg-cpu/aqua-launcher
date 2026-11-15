package dev.aqua.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.aqua.client.util.Keys;

/** Клавиша внутри модуля (GLFW-код, -1 = нет). Нажатие отслеживается методом {@link #pressed()}. */
public class KeySetting extends Setting<Integer> {
    private boolean wasDown;

    public KeySetting(String name, int def) { super(name, def); }

    /** true один раз в момент нажатия (вне меню). */
    public boolean pressed() {
        boolean down = value > 0 && net.minecraft.client.MinecraftClient.getInstance().currentScreen == null && Keys.isKeyDown(value);
        boolean edge = down && !wasDown;
        wasDown = down;
        return edge;
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) { if (e.isJsonPrimitive()) value = e.getAsInt(); }
}
