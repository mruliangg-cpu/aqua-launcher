package dev.aqua.pvpbot;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

public final class Messages {
    private Messages() {}

    public static MutableComponent prefix() {
        return Component.literal("[").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal("Aqua PvP").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static void title(ServerPlayer p, Component title, Component subtitle, int in, int stay, int out) {
        if (p == null || p.connection == null) return;
        p.connection.send(new ClientboundSetTitlesAnimationPacket(in, stay, out));
        p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle == null ? Component.empty() : subtitle));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    public static void actionBar(ServerPlayer p, Component text) {
        if (p != null) p.sendSystemMessage(text, true);
    }

    public static String time(int ticks) {
        int s = ticks / 20;
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }

    public static String hearts(float health) {
        return String.format("%.1f", Math.max(0f, health) / 2f) + "❤";
    }
}
