package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.NumberSetting;

/** Маленькие руки: уменьшает предмет/руку от первого лица до выбранного размера. */
public class SmallHands extends Module {
    private static SmallHands instance;
    private final NumberSetting size = add(new NumberSetting("Размер", 0.55, 0.20, 1.00, 0.05, "x"));

    public SmallHands() {
        super("Small Hands", "Уменьшает предмет в руке от первого лица", Category.RENDER);
        instance = this;
    }

    /** Масштаб руки от первого лица. 1.0 = выключено (ничего не меняем). */
    public static float scale() {
        if (instance == null || !instance.isEnabled()) return 1f;
        return (float) instance.size.get();
    }
}
