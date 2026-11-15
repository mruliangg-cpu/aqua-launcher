package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.TextHudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

/** Запоминает, где вы умерли, и показывает расстояние до этого места. */
public class DeathCoords extends TextHudModule {
    private static BlockPos deathPos;
    private static String deathDim;
    private boolean wasDead;

    /** Позиция последней смерти в этом измерении (для Waypoints). */
    public static net.minecraft.util.math.Vec3d lastDeath(String dimension) {
        if (deathPos == null || !dimension.equals(deathDim)) return null;
        return net.minecraft.util.math.Vec3d.ofBottomCenter(deathPos);
    }

    public DeathCoords() {
        super("Death Coords", "Координаты последней смерти и расстояние", Category.MISC, 0.8f, 0.3f,
                DeathCoords::line, () -> "☠ 120 64 -340 (85m)");
    }

    private static String line() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (deathPos == null || mc.player == null || mc.world == null) return null;
        String dim = mc.world.getRegistryKey().getValue().toString();
        String dist = dim.equals(deathDim) ? " (" + Math.round(Math.sqrt(mc.player.getBlockPos().getSquaredDistance(deathPos))) + dev.aqua.client.util.Lang.t("м)", "m)") : dev.aqua.client.util.Lang.t(" (другое измерение)", " (other dimension)");
        return "☠ " + deathPos.getX() + " " + deathPos.getY() + " " + deathPos.getZ() + dist;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        boolean dead = mc.player.isDead();
        if (dead && !wasDead) {
            deathPos = mc.player.getBlockPos();
            deathDim = mc.world.getRegistryKey().getValue().toString();
        }
        wasDead = dead;
    }
}
