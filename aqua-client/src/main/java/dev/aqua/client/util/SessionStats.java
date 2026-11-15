package dev.aqua.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

/** Статистика сессии: смерти, пройденное расстояние, прыжки, TPS сервера. */
public final class SessionStats {
    public static int deaths, jumps;
    public static double distance;
    private static boolean wasDead, wasOnGround = true;
    private static Vec3d last;
    private static long tpsWorldTime = -1, tpsRealTime;
    private static double tps = 20;

    private SessionStats() {}

    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) { last = null; tpsWorldTime = -1; return; }
        boolean dead = mc.player.isDead();
        if (dead && !wasDead) deaths++;
        wasDead = dead;

        Vec3d pos = mc.player.getPos();
        if (last != null) {
            double d = pos.distanceTo(last);
            if (d < 10) distance += d; // телепорты не считаем
        }
        last = pos;

        boolean ground = mc.player.isOnGround();
        if (wasOnGround && !ground && mc.player.getVelocity().y > 0.2 && !mc.player.isTouchingWater()) jumps++;
        wasOnGround = ground;

        long now = System.currentTimeMillis(), wt = mc.world.getTime();
        if (tpsWorldTime < 0) { tpsWorldTime = wt; tpsRealTime = now; }
        else if (now - tpsRealTime >= 2000) {
            double measured = (wt - tpsWorldTime) * 1000.0 / (now - tpsRealTime);
            tps = Math.max(0, Math.min(20, tps * 0.5 + measured * 0.5));
            tpsWorldTime = wt; tpsRealTime = now;
        }
    }

    public static double tps() { return tps; }
}
