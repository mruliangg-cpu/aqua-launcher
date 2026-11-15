package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.ModeSetting;

public class WeatherChanger extends Module {
    private final ModeSetting weather = add(new ModeSetting("Погода", "Ясно", "Ясно", "Дождь", "Гроза"));

    public WeatherChanger() { super("Weather Changer", "Убирает дождь или включает его (визуально)", Category.RENDER); }

    public void onWorldRender() {
        if (!isEnabled() || mc.world == null) return;
        float rain = weather.is("Ясно") ? 0f : 1f, thunder = weather.is("Гроза") ? 1f : 0f;
        mc.world.setRainGradient(rain);
        mc.world.setThunderGradient(thunder);
    }
}
