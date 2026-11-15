package dev.aqua.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class StringSetting extends Setting<String> {
    private final int maxLength;

    public StringSetting(String name, String def, int maxLength) {
        super(name, def);
        this.maxLength = maxLength;
    }

    public int getMaxLength() { return maxLength; }

    @Override public void set(String v) { value = v == null ? "" : (v.length() > maxLength ? v.substring(0, maxLength) : v); }
    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) { if (e.isJsonPrimitive()) set(e.getAsString()); }
}
