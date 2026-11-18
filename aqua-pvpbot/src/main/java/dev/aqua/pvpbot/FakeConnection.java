package dev.aqua.pvpbot;

import dev.aqua.pvpbot.mixin.ConnectionAccessor;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

/** A network connection that goes nowhere - the bot has no real client. */
public class FakeConnection extends Connection {
    public FakeConnection() {
        super(PacketFlow.SERVERBOUND);
        // makes isConnected() true (needed e.g. for ender pearls to teleport the bot)
        ((ConnectionAccessor) this).aquapvp$setChannel(new EmbeddedChannel());
    }

    @Override public void setReadOnly() {}
    @Override public void send(Packet<?> packet, ChannelFutureListener listener, boolean flush) {}
    @Override public void handleDisconnection() {}
    @Override public void setListenerForServerboundHandshake(PacketListener listener) {}
    @Override public <T extends PacketListener> void setupInboundProtocol(ProtocolInfo<T> info, T listener) {}
}
