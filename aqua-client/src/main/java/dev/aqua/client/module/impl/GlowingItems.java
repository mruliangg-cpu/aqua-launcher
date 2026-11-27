package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.ColorSetting;

/** Glowing Items — выпавшие на землю предметы светятся контуром выбранного цвета (или радугой chroma). */
public class GlowingItems extends Module {
    private static GlowingItems instance;
    private final ColorSetting color = add(new ColorSetting("Цвет", 0xFF3EE6FF)); // aqua

    public GlowingItems() {
        super("Glowing Items", "Выпавшие предметы светятся выбранным цветом", Category.RENDER);
        instance = this;
    }

    public static boolean on() { return instance != null && instance.isEnabled(); }

    /** Цвет свечения (ARGB, альфа всегда полная). */
    public static int color() {
        if (instance == null) return 0xFF3EE6FF;
        return 0xFF000000 | (instance.color.color() & 0xFFFFFF);
    }
}
