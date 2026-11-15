package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.NumberSetting;

/** Полная яркость: подменяет значение гаммы (см. SimpleOptionMixin). */
public class Fullbright extends Module {
    public static volatile boolean ACTIVE;
    private static Fullbright instance;
    private final NumberSetting level = add(new NumberSetting("Яркость", 1000, 100, 1500, 50, "%"));

    public Fullbright() {
        super("Fullbright", "Видно всё даже в темноте и пещерах", Category.RENDER);
        instance = this;
    }

    public static double gamma() { return instance.level.get() / 100.0; }

    @Override protected void onEnable() { ACTIVE = true; }
    @Override protected void onDisable() { ACTIVE = false; }
}
