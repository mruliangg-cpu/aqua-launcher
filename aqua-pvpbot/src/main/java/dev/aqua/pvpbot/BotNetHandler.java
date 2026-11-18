package dev.aqua.pvpbot;

import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;

import java.util.Set;

public class BotNetHandler extends ServerGamePacketListenerImpl {
    public BotNetHandler(MinecraftServer server, Connection connection, BotPlayer player, CommonListenerCookie cookie) {
        super(server, connection, player, cookie);
    }

    @Override
    public void disconnect(Component message) {
        // never kicked for idling etc. - bots are removed by the mod itself
        ((BotPlayer) this.player).removeBot();
    }

    @Override
    public void teleport(PositionMoveRotation pos, Set<Relative> relatives) {
        super.teleport(pos, relatives);
        if (this.player.level().getPlayerByUUID(this.player.getUUID()) != null) {
            this.resetPosition();
            this.player.level().getChunkSource().move(this.player);
        }
    }
}
