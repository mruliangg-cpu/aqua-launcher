package dev.aqua.client.module.impl;

import dev.aqua.client.AquaClient;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ModeSetting;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;

/** Свободный обзор: камера крутится вокруг игрока, а направление движения не меняется. */
public class Freelook extends Module {
    public static boolean active;
    public static float yaw, pitch;
    private static Freelook instance;
    private final ModeSetting mode = add(new ModeSetting("Режим", "Удерживать", "Удерживать", "Переключать"));
    private final BoolSetting invert = add(new BoolSetting("Инверсия по вертикали", false));
    private Perspective previous;
    private boolean toggled, wasDown;

    public Freelook() {
        super("Freelook", "Осмотреться, не поворачивая персонажа (Left Alt)", Category.RENDER);
        instance = this;
    }

    @Override
    public void onTick() {
        if (mc.player == null) { if (active) stop(); return; }
        boolean down = AquaClient.FREELOOK != null && AquaClient.FREELOOK.isPressed() && mc.currentScreen == null;
        if (down && !wasDown) toggled = !toggled;
        wasDown = down;
        boolean want = mode.is("Удерживать") ? down : toggled;
        if (want && !active) start();
        else if (!want && active) stop();
    }

    private void start() {
        yaw = mc.player.getYaw();
        pitch = mc.player.getPitch();
        previous = mc.options.getPerspective();
        if (previous.isFirstPerson()) mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        active = true;
    }

    private void stop() {
        active = false;
        toggled = false;
        if (previous != null) mc.options.setPerspective(previous);
    }

    public static void turn(double dx, double dy) {
        yaw += (float) dx * 0.15f;
        pitch = MathHelper.clamp(pitch + (float) (instance.invert.isOn() ? -dy : dy) * 0.15f, -90f, 90f);
    }

    @Override
    protected void onDisable() { if (active) stop(); }
}
