package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ColorSetting;

/** Glowing Items — предмет в руке светится выбранным цветом; галочка «На полу» — ещё и выпавшие предметы. */
public class GlowingItems extends Module {
    private static GlowingItems instance;
    private final ColorSetting color = add(new ColorSetting("Цвет", 0xFF3EE6FF)); // aqua
    private final BoolSetting floor = add(new BoolSetting("На полу", false));

    public GlowingItems() {
        super("Glowing Items", "Предмет в руке светится; опционально — и на полу", Category.RENDER);
        instance = this;
    }

    public static boolean on() { return instance != null && instance.isEnabled(); }
    public static boolean floorGlow() { return on() && instance.floor.isOn(); }

    /** Цвет свечения (ARGB, полная альфа). */
    public static int color() {
        if (instance == null) return 0xFF3EE6FF;
        return 0xFF000000 | (instance.color.color() & 0xFFFFFF);
    }
    public static float r() { return ((color() >> 16) & 0xFF) / 255f; }
    public static float g() { return ((color() >> 8) & 0xFF) / 255f; }
    public static float b() { return (color() & 0xFF) / 255f; }
}
