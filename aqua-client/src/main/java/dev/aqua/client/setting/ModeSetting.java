package dev.aqua.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;
import java.util.function.UnaryOperator;

public class ModeSetting extends Setting<String> {
    private final List<String> modes;
    private UnaryOperator<String> displayFn = dev.aqua.client.util.Lang::tr;

    public ModeSetting(String name, String def, String... modes) {
        super(name, def);
        this.modes = List.of(modes);
    }

    /** Как показывать значение в меню (по умолчанию — перевод). */
    public ModeSetting withDisplay(UnaryOperator<String> fn) { this.displayFn = fn; return this; }
    public String display() { return displayFn.apply(value); }

    public boolean is(String mode) { return value.equalsIgnoreCase(mode); }
    public List<String> getModes() { return modes; }

    public void cycle(int dir) {
        int i = modes.indexOf(value);
        value = modes.get(Math.floorMod(i + dir, modes.size()));
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) {
        if (e.isJsonPrimitive() && modes.contains(e.getAsString())) value = e.getAsString();
    }
}
