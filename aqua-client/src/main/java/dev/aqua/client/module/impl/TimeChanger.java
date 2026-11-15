package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.NumberSetting;

/** Меняет время суток только у вас на экране. */
public class TimeChanger extends Module {
    private final NumberSetting time = add(new NumberSetting("Время (тики)", 6000, 0, 23999, 250));

    public TimeChanger() { super("Time Changer", "Своё время суток (только визуально)", Category.RENDER); }

    public void onWorldRender() {
        if (isEnabled() && mc.world != null) dev.aqua.client.compat.V.setTimeOfDay(time.getInt());
    }
}
