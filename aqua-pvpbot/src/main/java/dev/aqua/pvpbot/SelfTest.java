package dev.aqua.pvpbot;

import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Automatic test used by the build server: bots fight each other with every kit and log what happened.
 * Only runs when a file named "aquapvp-selftest.flag" is in the server folder.
 */
final class SelfTest {
    static final boolean ENABLED = Files.exists(Path.of("aquapvp-selftest.flag"));
    private static final int ROUND_TICKS = 20 * 45;

    private static int clock = 0;
    private static int round = -1;
    private static boolean waiting = false;
    private static int roundStart = 0;
    private static BotPlayer a, b;
    private static final Kit[] KITS = Kit.values();

    static void tick(MinecraftServer server) {
        clock++;
        if (clock < 100) return;
        if (waiting) return;
        if (round == -1) { next(server); return; }
        boolean done = a == null || b == null || a.isLeaving() || b.isLeaving() || a.isRemoved() || b.isRemoved()
                || clock - roundStart > ROUND_TICKS;
        if (clock % 100 == 0 && a != null && b != null)
            log(String.format("  t=%ds A hp=%.1f+%.1f %s | B hp=%.1f+%.1f %s | dist=%.1f", (clock - roundStart) / 20,
                    a.getHealth(), a.getAbsorptionAmount(), pos(a), b.getHealth(), b.getAbsorptionAmount(), pos(b), a.distanceTo(b)));
        if (done) {
            Kit kit = KITS[round];
            String winner = a.isLeaving() || a.isRemoved() ? "B(impossible)" : b.isLeaving() || b.isRemoved() ? "A(hard)" : "nobody (time)";
            log("ROUND " + kit.id + " finished after " + (clock - roundStart) / 20 + "s, winner: " + winner);
            log("  A(hard)       " + a.brain.statsLine());
            log("  B(impossible) " + b.brain.statsLine());
            if (!a.isLeaving()) a.removeBot();
            if (!b.isLeaving()) b.removeBot();
            a = b = null;
            waiting = true;
            BotManager.later(40, () -> { waiting = false; next(server); });
        }
    }

    private static void next(MinecraftServer server) {
        round++;
        if (round >= KITS.length) {
            log("DONE");
            server.halt(false);
            return;
        }
        ServerLevel level = server.overworld();
        int x = round * 64;
        level.getChunk(x >> 4, 0); // load it first
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, 0);
        Kit kit = KITS[round];
        String na = "TestA" + round, nb = "TestB" + round;
        log("ROUND " + kit.id + " start at x=" + x + " y=" + y);
        a = BotPlayer.spawnAt(level, new Vec3(x + 0.5, y, 0.5), -90f, na,
                new BotBrain.Setup(UUIDUtil.createOfflinePlayerUUID(nb), Difficulty.HARD, kit, null));
        b = BotPlayer.spawnAt(level, new Vec3(x + 6.5, y, 0.5), 90f, nb,
                new BotBrain.Setup(UUIDUtil.createOfflinePlayerUUID(na), Difficulty.IMPOSSIBLE, kit, null));
        BotManager.register(a);
        BotManager.register(b);
        roundStart = clock;
    }

    private static String pos(BotPlayer p) {
        return String.format("(%.1f %.1f %.1f)%s", p.getX(), p.getY(), p.getZ(), p.onGround() ? "" : " air");
    }

    static void log(String s) { AquaPvPBot.LOG.info("AQUA-PVP-TEST " + s); }
}
