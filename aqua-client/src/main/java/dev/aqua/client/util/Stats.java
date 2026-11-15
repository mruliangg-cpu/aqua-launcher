package dev.aqua.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

/** Скорость игрока и время сессии. */
public final class Stats {
    private static Vec3d lastPos;
    private static double speed;
    private static long sessionStart;

    private Stats() {}

    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) { lastPos = null; speed = 0; sessionStart = 0; return; }
        if (sessionStart == 0) sessionStart = System.currentTimeMillis();
        Vec3d pos = dev.aqua.client.compat.V.pos(mc.player);
        if (lastPos != null) {
            double dx = pos.x - lastPos.x, dz = pos.z - lastPos.z;
            double now = Math.sqrt(dx * dx + dz * dz) * 20.0;
            speed = speed * 0.6 + now * 0.4;
        }
        lastPos = pos;
    }

    public static double speed() { return speed; }

    public static String session() {
        if (sessionStart == 0) return null;
        long s = (System.currentTimeMillis() - sessionStart) / 1000;
        return s >= 3600 ? String.format("%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60) : String.format("%02d:%02d", s / 60, s % 60);
    }
}
