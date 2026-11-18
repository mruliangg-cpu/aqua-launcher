package dev.aqua.pvpbot;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.UUID;

/**
 * The bot is a real (server-side) player without a client, like Carpet's fake players.
 * That means it uses the exact same combat rules as you: attack cooldown, crits, shields,
 * totems, potions, crystals, mace smashes - nothing is faked.
 */
@SuppressWarnings("EntityConstructor")
public class BotPlayer extends ServerPlayer {
    public Runnable fixStartingPosition = () -> {};
    public final BotBrain brain;
    private boolean leaving = false;

    private BotPlayer(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation info, BotBrain.Setup setup) {
        super(server, level, profile, info);
        this.brain = new BotBrain(this, setup);
    }

    /** Spawns a bot 4 blocks in front of the owner, facing them. */
    public static BotPlayer spawn(ServerPlayer owner, String name, BotBrain.Setup setup) {
        MinecraftServer server = owner.level().getServer();
        ServerLevel level = owner.level();
        UUID uuid = UUIDUtil.createOfflinePlayerUUID(name);
        GameProfile profile = new GameProfile(uuid, name);

        Vec3 pos = findSpawnPos(owner);
        double dx = owner.getX() - pos.x, dz = owner.getZ() - pos.z;
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;

        BotPlayer bot = new BotPlayer(server, level, profile, ClientInformation.createDefault(), setup);
        bot.fixStartingPosition = () -> bot.snapTo(pos.x, pos.y, pos.z, yaw, 0f);
        server.getPlayerList().placeNewPlayer(new FakeConnection(), bot, new CommonListenerCookie(profile, 0, bot.clientInformation(), false));
        bot.stopRiding();
        bot.teleportTo(level, pos.x, pos.y, pos.z, Set.of(), yaw, 0f, true);
        bot.setYHeadRot(yaw);
        bot.setHealth(bot.getMaxHealth());
        bot.unsetRemoved();
        bot.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(0.6);
        bot.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        bot.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0x7f); // show hat / jacket layers
        server.getPlayerList().broadcastAll(new ClientboundRotateHeadPacket(bot, (byte) (yaw * 256 / 360)), level.dimension());
        server.getPlayerList().broadcastAll(ClientboundEntityPositionSyncPacket.of(bot), level.dimension());
        setup.kit().apply(bot);
        bot.brain.start();
        return bot;
    }

    /** a free spot on the ground about 4 blocks in front of the player */
    private static Vec3 findSpawnPos(ServerPlayer owner) {
        ServerLevel level = owner.level();
        Vec3 look = Vec3.directionFromRotation(0f, owner.getYRot());
        for (double dist = 4.0; dist >= 1.5; dist -= 0.5) {
            Vec3 p = owner.position().add(look.scale(dist));
            BlockPos base = BlockPos.containing(p);
            for (int dy = 3; dy >= -4; dy--) {
                BlockPos feet = base.above(dy);
                if (level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), net.minecraft.core.Direction.UP)
                        && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                        && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
                    return new Vec3(p.x, feet.getY(), p.z);
                }
            }
        }
        return owner.position();
    }

    /** Leaves the server (safe to call many times). */
    public void removeBot() {
        if (leaving) return;
        leaving = true;
        MinecraftServer server = this.level().getServer();
        BotManager.unregister(this);
        server.schedule(new TickTask(server.getTickCount(), () -> this.connection.onDisconnect(new DisconnectionDetails(Component.literal("Bot removed")))));
    }

    public boolean isLeaving() { return leaving; }

    @Override
    public void tick() {
        if (this.level().getServer().getTickCount() % 10 == 0) {
            this.connection.resetPosition();
            this.level().getChunkSource().move(this);
        }
        try {
            if (!leaving) brain.tick();
        } catch (Throwable t) {
            AquaPvPBot.LOG.error("Bot brain error", t);
        }
        try {
            super.tick();
            this.doTick();
        } catch (NullPointerException ignored) {
        }
    }

    /** Physics, ground and fall damage are simulated here on the server (there is no client). */
    @Override
    public boolean isClientAuthoritative() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt) brain.onHurt(source, amount);
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        // practice bots never drop loot
        this.getInventory().clearContent();
        super.die(source);
        this.setHealth(20f);
        this.foodData = new FoodData();
        brain.onDeath(source);
        removeBot();
    }

    @Override
    public void onEquipItem(EquipmentSlot slot, ItemStack previous, ItemStack stack) {
        if (!isUsingItem()) super.onEquipItem(slot, previous, stack);
    }

    @Override
    public String getIpAddress() {
        return "127.0.0.1";
    }

    @Override
    public boolean allowsListing() {
        return true;
    }
}
