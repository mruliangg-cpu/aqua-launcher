package dev.aqua.client.module;

import dev.aqua.client.setting.Setting;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Базовый модуль Aqua Client. */
public abstract class Module {
    protected final MinecraftClient mc = MinecraftClient.getInstance();
    private final String name, description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;
    private int key = -1;

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected <S extends Setting<?>> S add(S setting) {
        settings.add(setting);
        return setting;
    }

    protected <S extends Setting<?>> S add(S setting, BooleanSupplier visible) {
        setting.setVisibility(visible);
        settings.add(setting);
        return setting;
    }

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean value) {
        if (value == enabled) return;
        enabled = value;
        if (value) onEnable(); else onDisable();
    }

    /** Включение при загрузке конфига (без побочных эффектов, если мир не загружен). */
    public void loadEnabled(boolean value) { setEnabled(value); }

    protected void onEnable() {}
    protected void onDisable() {}
    public void onTick() {}

    public boolean isEnabled() { return enabled; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public List<Setting<?>> getSettings() { return Collections.unmodifiableList(settings); }
    public int getKey() { return key; }
    public void setKey(int key) { this.key = key; }

    /** true — модуль работает только пока клавиша зажата; false — клавиша включает/выключает. */
    private boolean holdKey;
    public boolean isHoldKey() { return holdKey; }
    public void setHoldKey(boolean v) { holdKey = v; }
}
