package dev.aqua.client.module.impl;

import dev.aqua.client.AquaClient;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ModeSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;

/**
 * Свободный обзор (как в Lunar): пока держишь клавишу, мышь крутит только камеру,
 * а персонаж продолжает смотреть и идти туда же. Клавиша проверяется каждый кадр,
 * камера всегда сзади (работает и если до этого был вид спереди), возврат — плавный.
 */
public class Freelook extends Module {
    /** Камера в режиме Freelook (в том числе во время плавного возврата). */
    public static boolean active;
    /** Мышь управляет камерой, а не игроком. */
    public static boolean capturing;
    public static float yaw, pitch;
    private static float realYaw, realPitch, realPrevYaw, realPrevPitch;
    private static Freelook instance;

    private final ModeSetting mode = add(new ModeSetting("Режим", "Удерживать", "Удерживать", "Переключать"));
    private final ModeSetting view = add(new ModeSetting("Вид", "Сзади", "Сзади", "Первое лицо"));
    private final NumberSetting sens = add(new NumberSetting("Чувствительность", 100, 25, 200, 5, "%"));
    private final BoolSetting invert = add(new BoolSetting("Инверсия по вертикали", false));
    private final BoolSetting smooth = add(new BoolSetting("Плавный возврат", true));

    private Perspective previous;
    private boolean toggled, wasDown, returning;
    private long lastFrame;

    public Freelook() {
        super("Freelook", "Осмотреться, не поворачивая персонажа (Left Alt)", Category.RENDER);
        instance = this;
    }

    /** Вызывается каждый кадр из Camera.update. */
    public static void frame() {
        Freelook f = instance;
        if (f == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!f.isEnabled() || mc.player == null) { if (active) f.finish(); return; }
        boolean down = AquaClient.FREELOOK != null && AquaClient.FREELOOK.isPressed() && mc.currentScreen == null;
        if (down && !f.wasDown) f.toggled = !f.toggled;
        f.wasDown = down;
        boolean want = f.mode.is("Удерживать") ? down : f.toggled;
        if (want && !capturing) f.start();
        else if (!want && capturing) f.release();
        if (capturing && f.view.is("Сзади") && mc.options.getPerspective() != Perspective.THIRD_PERSON_BACK)
            mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);

        long now = System.nanoTime();
        float dt = f.lastFrame == 0 ? 0.016f : Math.min(0.1f, (now - f.lastFrame) / 1e9f);
        f.lastFrame = now;
        if (f.returning) {
            float k = Math.min(1f, dt * 14f);
            float ty = mc.player.getYaw(), tp = mc.player.getPitch();
            yaw += MathHelper.wrapDegrees(ty - yaw) * k;
            pitch += (tp - pitch) * k;
            if (Math.abs(MathHelper.wrapDegrees(ty - yaw)) < 0.6f && Math.abs(tp - pitch) < 0.6f) f.finish();
        }
    }

    private void start() {
        if (!active) {
            yaw = mc.player.getYaw();
            pitch = mc.player.getPitch();
            previous = mc.options.getPerspective();
        }
        if (view.is("Сзади")) mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        capturing = true;
        returning = false;
        active = true;
    }

    private void release() {
        capturing = false;
        toggled = false;
        if (smooth.isOn()) {
            returning = true;
            if (previous != null && previous.isFirstPerson()) restorePerspective(); // в первом лице камера сразу у глаз
        } else finish();
    }

    private void finish() {
        capturing = false;
        returning = false;
        active = false;
        toggled = false;
        restorePerspective();
    }

    private void restorePerspective() {
        if (previous != null) mc.options.setPerspective(previous);
        previous = null;
    }

    public static void turn(double dx, double dy) {
        float k = 0.15f * (instance == null ? 1f : instance.sens.getFloat() / 100f);
        yaw += (float) dx * k;
        pitch = MathHelper.clamp(pitch + (float) (instance != null && instance.invert.isOn() ? -dy : dy) * k, -90f, 90f);
    }

    /** На время обновления камеры ставим игроку углы Freelook — камера встаёт по ним. */
    public static void applyToPlayer() {
        if (!active || instance == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        var p = mc.player;
        realYaw = p.getYaw(); realPitch = p.getPitch();
        realPrevYaw = dev.aqua.client.compat.V.prevYaw(p); realPrevPitch = dev.aqua.client.compat.V.prevPitch(p);
        p.setYaw(yaw); p.setPitch(pitch);
        dev.aqua.client.compat.V.setPrevRot(p, yaw, pitch);
    }

    /** Сразу после камеры возвращаем настоящие углы — тело и движение не меняются. */
    public static void restorePlayer() {
        if (!active || instance == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        var p = mc.player;
        p.setYaw(realYaw); p.setPitch(realPitch);
        dev.aqua.client.compat.V.setPrevRot(p, realPrevYaw, realPrevPitch);
    }

    @Override
    public void onTick() {
        if (mc.player == null && active) finish();
    }

    @Override
    protected void onDisable() { if (active) finish(); }
}
