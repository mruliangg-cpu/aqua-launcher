package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.NumberSetting;

public class LowFire extends Module {
    private static LowFire instance;
    private final NumberSetting offset = add(new NumberSetting("Опустить на", 0.3, 0.05, 0.6, 0.05));

    public LowFire() {
        super("Low Fire", "Огонь на экране не закрывает обзор", Category.RENDER);
        instance = this;
    }

    public static float offset() { return instance != null && instance.isEnabled() ? instance.offset.getFloat() : 0f; }
}
