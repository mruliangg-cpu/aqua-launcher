package dev.aqua.client.module.impl;

import dev.aqua.client.AquaClient;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.NumberSetting;
import dev.aqua.client.util.Anim;

/** Зум как в OptiFine/Lunar: клавиша C, колёсико меняет кратность. */
public class Zoom extends Module {
    public static Zoom INSTANCE;
    private final NumberSetting factor = add(new NumberSetting("Кратность", 4, 1.5, 15, 0.5, "x"));
    private final BoolSetting smooth = add(new BoolSetting("Плавный зум", true));
    private final BoolSetting scroll = add(new BoolSetting("Колёсико мыши", true));
    private final BoolSetting cinematic = add(new BoolSetting("Кинематографичная камера", false));
    private final BoolSetting lowerSens = add(new BoolSetting("Снижать чувствительность", true));
    private final Anim anim = new Anim(1);
    private long lastNs = System.nanoTime();
    private boolean zooming;
    private double scrollMul = 1;
    private Double savedSens;
    private boolean savedCinematic;

    public Zoom() {
        super("Zoom", "Приближение на клавишу C (меняется в управлении)", Category.RENDER);
        INSTANCE = this;
    }

    public static double fovDivisor() {
        return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE.divisor() : 1.0;
    }

    public static boolean onScroll(double amount) {
        if (INSTANCE == null || !INSTANCE.isEnabled() || !INSTANCE.zooming || !INSTANCE.scroll.isOn() || amount == 0) return false;
        INSTANCE.scrollMul = Math.max(0.5, Math.min(6, INSTANCE.scrollMul * (amount > 0 ? 1.2 : 1 / 1.2)));
        return true;
    }

    private double divisor() {
        boolean want = AquaClient.ZOOM != null && AquaClient.ZOOM.isPressed() && mc.currentScreen == null;
        if (want && !zooming) start();
        else if (!want && zooming) stop();
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNs) / 1e9f);
        lastNs = now;
        anim.to(zooming ? (float) (factor.get() * scrollMul) : 1f);
        if (smooth.isOn()) anim.update(dt, 14);
        else anim.snap(anim.target());
        return Math.max(1.0, anim.get());
    }

    private void start() {
        zooming = true;
        scrollMul = 1;
        if (cinematic.isOn()) {
            savedCinematic = mc.options.smoothCameraEnabled;
            mc.options.smoothCameraEnabled = true;
        }
        if (lowerSens.isOn()) {
            savedSens = mc.options.getMouseSensitivity().getValue();
            mc.options.getMouseSensitivity().setValue(savedSens * 0.45);
        }
    }

    private void stop() {
        zooming = false;
        if (cinematic.isOn()) mc.options.smoothCameraEnabled = savedCinematic;
        if (savedSens != null) {
            mc.options.getMouseSensitivity().setValue(savedSens);
            savedSens = null;
        }
    }

    @Override
    protected void onDisable() {
        if (zooming) stop();
        anim.snap(1);
    }
}
