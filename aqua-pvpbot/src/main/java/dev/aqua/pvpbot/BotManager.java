package dev.aqua.pvpbot;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Keeps track of bots, chosen kits, saved inventories and endless runs. */
public final class BotManager {
    public static final int MAX_BOTS_PER_PLAYER = 3;
    public static final int MAX_BOTS_TOTAL = 12;

    private static final List<BotPlayer> BOTS = new ArrayList<>();
    private static final Map<UUID, Kit> SELECTED_KIT = new HashMap<>();
    private static final Map<UUID, List<ItemStack>> SAVED_INV = new HashMap<>();
    private static final Map<UUID, EndlessRun> ENDLESS = new HashMap<>();
    private static final Map<UUID, Integer> ENDLESS_BEST = new HashMap<>();
    private static final List<Delayed> TASKS = new ArrayList<>();
    private static int now = 0;

    private record Delayed(int at, Runnable run) {}

    public static final class EndlessRun {
        public final UUID owner;
        public final Kit kit;
        public int wave = 1;
        public boolean over = false;
        EndlessRun(UUID owner, Kit kit) { this.owner = owner; this.kit = kit; }
    }

    private BotManager() {}

    // ---------------- ticking ----------------
    public static void tick(MinecraftServer server) {
        now++;
        if (TASKS.isEmpty()) return;
        List<Delayed> due = new ArrayList<>();
        for (Iterator<Delayed> it = TASKS.iterator(); it.hasNext(); ) {
            Delayed d = it.next();
            if (d.at <= now) { due.add(d); it.remove(); }
        }
        for (Delayed d : due) {
            try { d.run.run(); } catch (Throwable t) { AquaPvPBot.LOG.error("Aqua PvP Bot task failed", t); }
        }
    }

    public static void later(int ticks, Runnable r) { TASKS.add(new Delayed(now + ticks, r)); }

    // ---------------- bots ----------------
    public static void register(BotPlayer bot) { BOTS.add(bot); }
    public static void unregister(BotPlayer bot) { BOTS.remove(bot); }

    public static List<BotPlayer> bots() { return new ArrayList<>(BOTS); }

    public static int countFor(UUID owner) {
        int n = 0;
        for (BotPlayer b : BOTS) if (owner.equals(b.brain.ownerId)) n++;
        return n;
    }

    public static int removeAll(UUID ownerOrNull) {
        int n = 0;
        for (BotPlayer b : bots()) {
            if (ownerOrNull == null || ownerOrNull.equals(b.brain.ownerId)) { b.removeBot(); n++; }
        }
        if (ownerOrNull == null) ENDLESS.clear(); else ENDLESS.remove(ownerOrNull);
        return n;
    }

    /** A free bot name like "HardBot", "HardBot2"... */
    public static String freeName(MinecraftServer server, String base) {
        base = base.length() > 13 ? base.substring(0, 13) : base;
        for (int i = 1; i < 100; i++) {
            String name = i == 1 ? base : base + i;
            if (server.getPlayerList().getPlayerByName(name) == null) return name;
        }
        return base + (int) (Math.random() * 900 + 100);
    }

    /** Spawns a bot for the owner. Returns an error message, or null when it worked. */
    public static Component spawn(ServerPlayer owner, Difficulty difficulty, Kit kit, EndlessRun run) {
        if (countFor(owner.getUUID()) >= MAX_BOTS_PER_PLAYER)
            return Component.literal("You already have " + MAX_BOTS_PER_PLAYER + " bots. Use /killbots first.");
        if (BOTS.size() >= MAX_BOTS_TOTAL)
            return Component.literal("Too many bots on this server (max " + MAX_BOTS_TOTAL + ").");
        String name = freeName(owner.level().getServer(), capital(difficulty.id) + "Bot");
        BotPlayer bot = BotPlayer.spawn(owner, name, new BotBrain.Setup(owner.getUUID(), difficulty, kit, run));
        register(bot);
        return null;
    }

    // ---------------- endless ----------------
    public static EndlessRun startEndless(ServerPlayer owner, Kit kit) {
        EndlessRun run = new EndlessRun(owner.getUUID(), kit);
        ENDLESS.put(owner.getUUID(), run);
        return run;
    }

    public static int bestWave(UUID owner) { return ENDLESS_BEST.getOrDefault(owner, 0); }

    /** The bot of an endless run died: next wave in 3 seconds. */
    public static void endlessWaveWon(MinecraftServer server, EndlessRun run) {
        if (run.over) return;
        int cleared = run.wave;
        run.wave++;
        ENDLESS_BEST.merge(run.owner, cleared, Math::max);
        ServerPlayer owner = server.getPlayerList().getPlayer(run.owner);
        if (owner == null) { run.over = true; ENDLESS.remove(run.owner); return; }
        Messages.title(owner, Component.literal("Wave " + cleared + " cleared!").withStyle(ChatFormatting.GREEN),
                Component.literal("Wave " + run.wave + " starts in 3...").withStyle(ChatFormatting.LIGHT_PURPLE), 5, 40, 10);
        // fresh round: refill your kit if you picked the same one, otherwise just heal
        if (SELECTED_KIT.get(run.owner) == run.kit) run.kit.apply(owner);
        else { owner.setHealth(owner.getMaxHealth()); owner.getFoodData().setFoodLevel(20); }
        later(60, () -> {
            ServerPlayer o = server.getPlayerList().getPlayer(run.owner);
            if (run.over || o == null || !o.isAlive() || ENDLESS.get(run.owner) != run) return;
            Component err = spawn(o, Difficulty.endless(run.wave), run.kit, run);
            if (err != null) o.sendSystemMessage(err.copy().withStyle(ChatFormatting.RED));
        });
    }

    public static void endlessLost(ServerPlayer owner, EndlessRun run) {
        if (run.over) return;
        run.over = true;
        ENDLESS.remove(run.owner);
        int reached = run.wave;
        int best = Math.max(bestWave(run.owner), reached - 1);
        ENDLESS_BEST.put(run.owner, best);
        if (owner != null) {
            Messages.title(owner, Component.literal("Endless over").withStyle(ChatFormatting.RED),
                    Component.literal("You reached wave " + reached + "  •  best: " + best + " cleared").withStyle(ChatFormatting.LIGHT_PURPLE), 5, 60, 20);
            owner.sendSystemMessage(Messages.prefix().append(Component.literal("Endless run over - you reached wave " + reached
                    + " (" + (reached - 1) + " bots beaten). Best: " + best + ". ").withStyle(ChatFormatting.LIGHT_PURPLE))
                    .append(Component.literal("/spawnbot endless").withStyle(ChatFormatting.AQUA)).append(Component.literal(" to try again.").withStyle(ChatFormatting.GRAY)));
        }
    }

    // ---------------- kits ----------------
    public static void selectKit(ServerPlayer p, Kit kit) {
        SELECTED_KIT.put(p.getUUID(), kit);
        // keep the items the player had before their first kit, so /kit restore can give them back
        if (!SAVED_INV.containsKey(p.getUUID())) {
            Inventory inv = p.getInventory();
            List<ItemStack> copy = new ArrayList<>();
            boolean any = false;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i).copy();
                if (!s.isEmpty()) any = true;
                copy.add(s);
            }
            if (any) SAVED_INV.put(p.getUUID(), copy);
        }
        kit.apply(p);
    }

    public static Kit selectedKit(UUID player) { return SELECTED_KIT.get(player); }

    public static boolean restore(ServerPlayer p) {
        List<ItemStack> saved = SAVED_INV.remove(p.getUUID());
        SELECTED_KIT.remove(p.getUUID());
        if (saved == null) return false;
        Inventory inv = p.getInventory();
        inv.clearContent();
        for (int i = 0; i < saved.size() && i < inv.getContainerSize(); i++) inv.setItem(i, saved.get(i));
        p.inventoryMenu.broadcastChanges();
        return true;
    }

    static String capital(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }
}
