package dev.aqua.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

/** Данные для Reach Display, Combo Counter и Target HUD. */
public final class CombatTracker {
    public static double lastReach = -1;
    public static long lastReachTime;
    public static int combo;
    public static long lastHitTime;
    public static LivingEntity lastTarget;
    public static long lastTargetTime;
    private static int prevHurtTime;

    private CombatTracker() {}

    public static void onAttack(Entity target) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        HitResult hit = mc.crosshairTarget;
        if (hit instanceof EntityHitResult ehr && ehr.getEntity() == target) {
            lastReach = mc.player.getEyePos().distanceTo(ehr.getPos());
        } else {
            lastReach = Math.sqrt(mc.player.squaredDistanceTo(target));
        }
        lastReachTime = System.currentTimeMillis();
        long now = System.currentTimeMillis();
        combo = now - lastHitTime > 2500 ? 1 : combo + 1;
        lastHitTime = now;
        if (target instanceof LivingEntity living) {
            lastTarget = living;
            lastTargetTime = now;
        }
    }

    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) { combo = 0; lastTarget = null; return; }
        int hurt = mc.player.hurtTime;
        if (hurt > prevHurtTime) combo = 0; // получили урон — комбо сброшено
        prevHurtTime = hurt;
        if (System.currentTimeMillis() - lastHitTime > 2500) combo = 0;
        if (lastTarget != null && (lastTarget.isRemoved() || System.currentTimeMillis() - lastTargetTime > 4000)) lastTarget = null;
    }
}
