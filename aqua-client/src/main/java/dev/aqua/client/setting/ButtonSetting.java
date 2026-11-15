package dev.aqua.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

/** Кнопка-действие в настройках модуля (не сохраняется). */
public class ButtonSetting extends Setting<String> {
    private final Runnable action;

    public ButtonSetting(String name, String label, Runnable action) {
        super(name, label);
        this.action = action;
    }

    public void run() { action.run(); }

    @Override public JsonElement toJson() { return JsonNull.INSTANCE; }
    @Override public void fromJson(JsonElement e) {}
}
