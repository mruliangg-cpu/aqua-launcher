package dev.aqua.client.util;

/** Плавная экспоненциальная анимация значения. */
public final class Anim {
    private float value, target;

    public Anim(float start) { value = target = start; }

    public void to(float t) { target = t; }
    public void snap(float v) { value = target = v; }
    public float get() { return value; }
    public float target() { return target; }

    public float update(float dt, float speed) {
        value += (target - value) * (1f - (float) Math.exp(-speed * dt));
        if (Math.abs(target - value) < 0.0005f) value = target;
        return value;
    }

    public static float easeOutBack(float t) {
        float c1 = 1.70158f, c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }

    public static float easeOutCubic(float t) { return 1 - (float) Math.pow(1 - t, 3); }
}
