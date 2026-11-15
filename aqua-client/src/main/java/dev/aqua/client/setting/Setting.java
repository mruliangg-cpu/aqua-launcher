package dev.aqua.client.setting;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

/** Базовая настройка модуля. */
public abstract class Setting<T> {
    private final String name;
    protected final T defaultValue;
    protected T value;
    private BooleanSupplier visible = () -> true;

    protected Setting(String name, T defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String getName() { return name; }
    public T get() { return value; }
    public void set(T value) { this.value = value; }
    public void reset() { this.value = defaultValue; }

    public boolean isVisible() { return visible.getAsBoolean(); }

    public void setVisibility(BooleanSupplier supplier) { this.visible = supplier; }

    public abstract JsonElement toJson();
    public abstract void fromJson(JsonElement element);
}
