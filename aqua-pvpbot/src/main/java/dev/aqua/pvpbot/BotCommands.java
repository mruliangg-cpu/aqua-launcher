package dev.aqua.pvpbot;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

/**
 * /spawnbot <easy|normal|hard|endless|impossible> [kit]
 * /kit <sword|diamond|op|nethpot|crystal|mace|restore>
 * /killbots
 * /pvpbot  (help menu with clickable buttons)
 */
public final class BotCommands {
    private BotCommands() {}

    private static final String[] KIT_IDS = Arrays.stream(Kit.values()).map(k -> k.id).toArray(String[]::new);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawnbot")
                .executes(ctx -> { menu(ctx.getSource().getPlayerOrException()); return 1; })
                .then(Commands.argument("difficulty", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Difficulty.IDS, b))
                        .executes(ctx -> spawn(ctx, null))
                        .then(Commands.argument("kit", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(KIT_IDS, b))
                                .executes(ctx -> spawn(ctx, StringArgumentType.getString(ctx, "kit"))))));

        dispatcher.register(Commands.literal("kit")
                .executes(ctx -> { kitMenu(ctx.getSource().getPlayerOrException()); return 1; })
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                java.util.stream.Stream.concat(Arrays.stream(KIT_IDS), java.util.stream.Stream.of("restore")), b))
                        .executes(BotCommands::kit)));

        dispatcher.register(Commands.literal("killbots").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            int n = BotManager.removeAll(p.getUUID());
            p.sendSystemMessage(Messages.prefix().append(Component.literal(n == 0 ? "You have no bots." : "Removed " + n + " bot" + (n == 1 ? "" : "s") + ".").withStyle(ChatFormatting.GRAY)));
            return n;
        }));

        dispatcher.register(Commands.literal("pvpbot").executes(ctx -> { menu(ctx.getSource().getPlayerOrException()); return 1; }));
    }

    private static int spawn(CommandContext<CommandSourceStack> ctx, String kitId) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        String diffId = StringArgumentType.getString(ctx, "difficulty");
        Difficulty diff = Difficulty.byId(diffId);
        if (diff == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown difficulty \"" + diffId + "\". Use easy, normal, hard, endless or impossible."));
            return 0;
        }
        Kit kit;
        if (kitId != null) {
            kit = Kit.byId(kitId);
            if (kit == null) {
                ctx.getSource().sendFailure(Component.literal("Unknown kit \"" + kitId + "\". Use sword, diamond, op, nethpot, crystal or mace."));
                return 0;
            }
        } else {
            kit = BotManager.selectedKit(p.getUUID());
            if (kit == null) kit = Kit.SWORD;
        }
        BotManager.EndlessRun run = null;
        if (diff.id.equals("endless")) {
            BotManager.removeAll(p.getUUID());
            run = BotManager.startEndless(p, kit);
            p.sendSystemMessage(Messages.prefix().append(Component.literal("Endless mode: ").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
                    .append(Component.literal("every bot you beat brings a stronger one. How far can you get? Best: wave "
                            + BotManager.bestWave(p.getUUID())).withStyle(ChatFormatting.GRAY)));
        }
        Component err = BotManager.spawn(p, diff, kit, run);
        if (err != null) { ctx.getSource().sendFailure(err); return 0; }
        return 1;
    }

    private static int kit(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "name");
        if (name.equalsIgnoreCase("restore")) {
            boolean ok = BotManager.restore(p);
            p.sendSystemMessage(Messages.prefix().append(Component.literal(ok ? "Your old items are back." : "There is nothing to restore.").withStyle(ChatFormatting.GRAY)));
            return ok ? 1 : 0;
        }
        Kit kit = Kit.byId(name);
        if (kit == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown kit \"" + name + "\". Use sword, diamond, op, nethpot, crystal or mace."));
            return 0;
        }
        BotManager.selectKit(p, kit);
        p.sendSystemMessage(Messages.prefix().append(Component.literal("Kit ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(kit.label).withStyle(kit.color, ChatFormatting.BOLD))
                .append(Component.literal(" equipped. Bots you spawn now use it too. ").withStyle(ChatFormatting.GRAY))
                .append(button("[/kit restore]", "/kit restore", "Get back the items you had before", ChatFormatting.DARK_AQUA, false)));
        return 1;
    }

    // ---------------- menus ----------------
    private static void menu(ServerPlayer p) {
        p.sendSystemMessage(Component.literal(""));
        p.sendSystemMessage(Component.literal("  ⚔ Aqua PvP Bot ").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                .append(Component.literal("· made by Ulian Voskoboynikov").withStyle(ChatFormatting.DARK_GRAY)));
        Kit sel = BotManager.selectedKit(p.getUUID());
        MutableComponent diffs = Component.literal("  Spawn a bot: ").withStyle(ChatFormatting.GRAY);
        for (String id : Difficulty.IDS) {
            Difficulty d = Difficulty.byId(id);
            diffs.append(button("[" + d.id.substring(0, 1).toUpperCase() + d.id.substring(1) + "]", "/spawnbot " + id,
                    describe(id), d.color, true)).append(Component.literal(" "));
        }
        p.sendSystemMessage(diffs);
        kitLine(p, sel);
        p.sendSystemMessage(Component.literal("  ").append(button("[Remove my bots]", "/killbots", "Remove all your bots", ChatFormatting.RED, true))
                .append(Component.literal("  ")).append(button("[Restore my items]", "/kit restore", "Get back the items you had before /kit", ChatFormatting.DARK_AQUA, true)));
        p.sendSystemMessage(Component.literal("  Tip: /spawnbot hard crystal  •  bots use the kit you picked with /kit").withStyle(ChatFormatting.DARK_GRAY));
        p.sendSystemMessage(Component.literal(""));
    }

    private static void kitMenu(ServerPlayer p) {
        kitLine(p, BotManager.selectedKit(p.getUUID()));
    }

    private static void kitLine(ServerPlayer p, Kit sel) {
        MutableComponent kits = Component.literal("  Pick a kit: ").withStyle(ChatFormatting.GRAY);
        for (Kit k : Kit.values()) {
            String label = (k == sel ? "✔" : "") + "[" + k.label + "]";
            kits.append(button(label, "/kit " + k.id, kitInfo(k), k.color, true)).append(Component.literal(" "));
        }
        p.sendSystemMessage(kits);
    }

    private static MutableComponent button(String text, String command, String hover, ChatFormatting color, boolean run) {
        ClickEvent click = run ? new ClickEvent.RunCommand(command) : new ClickEvent.SuggestCommand(command);
        return Component.literal(text).withStyle(s -> s.withColor(color).withBold(true)
                .withClickEvent(click)
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(hover + "\n").append(Component.literal(command).withStyle(ChatFormatting.DARK_GRAY)))));
    }

    private static String describe(String id) {
        return switch (id) {
            case "easy" -> "Slow, misses a lot, no crits.\nGood for learning.";
            case "normal" -> "Strafes a bit, sometimes crits,\nheals when low.";
            case "hard" -> "Crits, w-taps, spaces, shields,\nbreaks your shield, pearls.";
            case "endless" -> "Wave after wave - every bot\nyou beat brings a stronger one.";
            case "impossible" -> "Near-perfect reactions, aim\nand timing. Good luck.";
            default -> id;
        };
    }

    private static String kitInfo(Kit k) {
        return switch (k) {
            case SWORD -> "Diamond armor + sword.\nPure sword fight.";
            case DIAMOND -> "Diamond gear, axe, shield,\ngolden apples, bow.";
            case OP -> "God netherite, enchanted gapples,\ntotems, pearls, potions.";
            case NETHPOT -> "Netherite + splash healing pots,\nstrength & speed, totems.";
            case CRYSTAL -> "Obsidian, end crystals, totems,\ngapples, anchors.";
            case MACE -> "Density mace, wind charges,\ntotems - smash from above.";
        };
    }
}
