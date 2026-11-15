package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.NumberSetting;

/** Своё поле зрения (до 150°) и отключение «рыбьего глаза» от скорости. */
public class FovChanger extends Module {
    private static FovChanger instance;
    private final NumberSetting fov = add(new NumberSetting("Поле зрения", 90, 30, 150, 1, "°"));
    private final BoolSetting noDynamic = add(new BoolSetting("Без эффекта скорости", true));

    public FovChanger() {
        super("FOV Changer", "Поле зрения до 150° и без эффекта бега", Category.RENDER);
        instance = this;
    }

    /** Множитель к ванильному FOV. */
    public static double factor(double vanillaFov) {
        if (instance == null || !instance.isEnabled() || vanillaFov <= 0) return 1.0;
        return instance.fov.get() / vanillaFov;
    }

    @Override
    public void onTick() {
        if (noDynamic.isOn() && mc.options.getFovEffectScale().getValue() != 0.0) mc.options.getFovEffectScale().setValue(0.0);
    }
}
