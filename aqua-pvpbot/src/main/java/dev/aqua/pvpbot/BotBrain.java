package dev.aqua.pvpbot;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * The bot's brain: runs every tick before the bot moves.
 * It only does things a real player can do - press movement keys, turn its head,
 * click, use items and switch hotbar slots.
 */
public final class BotBrain {
    public record Setup(UUID owner, Difficulty difficulty, Kit kit, BotManager.EndlessRun run) {}

    private static final int COUNTDOWN = 60;

    final BotPlayer bot;
    final UUID ownerId;
    final Difficulty d;
    final Kit kit;
    final BotManager.EndlessRun run;
    final RandomSource rng = RandomSource.create();

    int age = 0;
    int countdown = COUNTDOWN;
    boolean fightOver = false;
    int noOwnerTicks = 0;

    // target & what the bot "has seen" (reaction time)
    LivingEntity target;
    LivingEntity attacker; int attackerUntil;
    final ArrayDeque<Vec3> seen = new ArrayDeque<>();
    Vec3 seenPos, seenVel = Vec3.ZERO;

    // aim
    float aimNoiseYaw, aimNoisePitch; int nextNoise;

    // melee timing
    int nextClickAt = 0;
    int resprintAt = 0;
    boolean critPlan = false;
    int jumpedAt = -100;
    int strafeDir = 1; int strafeSwitchAt = 0;
    boolean shieldUp = false;

    // items
    int totemMissingSince = -1;
    int nextPotAt = 0;
    int nextCrystalAt = 0;
    int nextPearlAt = 0;
    int bowStart = -1;
    int nextBuffAt = 0;
    int retreatUntil = 0;

    // mace
    enum MaceState { GROUND, LAUNCH, AIR }
    MaceState mace = MaceState.GROUND; int maceStateAt = 0; int nextWindAt = 0;

    // movement output for this tick
    float forward, strafe; boolean jump, sprint;

    // stats
    // what the bot did (shown by the self-test)
    int nCrit, nCrystalPlace, nCrystalBreak, nObsidian, nPot, nApple, nPearl, nArrow, nWind, nSmash, nTotem, nShield, nAxe, nMiss;
    int startTick = 0, botHits = 0, ownerHits = 0, ownerCombo = 0, ownerBestCombo = 0, botCombo = 0, botBestCombo = 0;

    BotBrain(BotPlayer bot, Setup s) {
        this.bot = bot;
        this.ownerId = s.owner();
        this.d = s.difficulty();
        this.kit = s.kit();
        this.run = s.run();
    }

    // =====================================================================
    //  life cycle
    // =====================================================================
    void start() {
        ServerPlayer owner = owner();
        if (owner != null) {
            owner.sendSystemMessage(Messages.prefix()
                    .append(Component.literal(bot.getScoreboardName()).withStyle(d.color, ChatFormatting.BOLD))
                    .append(Component.literal(" joined  •  ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(d.label).withStyle(d.color))
                    .append(Component.literal("  •  kit ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(kit.label).withStyle(kit.color)));
            if (owner.isCreative() || owner.isSpectator())
                owner.sendSystemMessage(Messages.prefix().append(Component.literal(
                        "You are in " + (owner.isCreative() ? "creative" : "spectator") + " mode - switch to survival to fight: /gamemode survival").withStyle(ChatFormatting.YELLOW)));
            Kit mine = BotManager.selectedKit(ownerId);
            if (mine == null) owner.sendSystemMessage(Messages.prefix().append(Component.literal("Tip: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("/kit " + kit.id).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" gives you the same kit as the bot.").withStyle(ChatFormatting.GRAY)));
        }
    }

    public String statsLine() {
        return "hits=" + botHits + " miss=" + nMiss + " crit=" + nCrit + " axe=" + nAxe + " shield=" + nShield + " totem=" + nTotem
                + " pot=" + nPot + " apple=" + nApple + " arrow=" + nArrow + " pearl=" + nPearl + " obsidian=" + nObsidian
                + " crystalPlace=" + nCrystalPlace + " crystalBreak=" + nCrystalBreak + " wind=" + nWind + " smash=" + nSmash;
    }

    ServerPlayer owner() {
        MinecraftServer server = bot.level().getServer();
        return server == null ? null : server.getPlayerList().getPlayer(ownerId);
    }

    void onHurt(DamageSource source, float amount) {
        Entity e = source.getEntity();
        if (e instanceof Player p && p != bot) {
            if (p.getUUID().equals(ownerId)) {
                ownerHits++;
                ownerCombo++;
                ownerBestCombo = Math.max(ownerBestCombo, ownerCombo);
                botCombo = 0;
            } else if (!(p instanceof BotPlayer)) {
                attacker = p;
                attackerUntil = age + 200;
            }
        }
        // a hit cancels eating / bow drawing like for a real player who gets comboed
        if (critPlan && rng.nextFloat() < 0.4f) critPlan = false;
    }

    void onDeath(DamageSource source) {
        if (fightOver) return;
        fightOver = true;
        ServerPlayer owner = owner();
        if (run != null) {
            sendSummary(owner, true);
            BotManager.endlessWaveWon(bot.level().getServer(), run);
            return;
        }
        if (owner != null) {
            Messages.title(owner, Component.literal("VICTORY").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                    Component.literal("You beat " + d.label + " Bot").withStyle(d.color), 5, 50, 15);
            sendSummary(owner, true);
        }
    }

    private void ownerLost(ServerPlayer owner) {
        if (fightOver) return;
        fightOver = true;
        releaseAll();
        if (run != null) {
            sendSummary(owner, false);
            BotManager.endlessLost(owner, run);
        } else if (owner != null) {
            Messages.title(owner, Component.literal("DEFEAT").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    Component.literal(d.label + " Bot won with " + Messages.hearts(bot.getHealth()) + " left").withStyle(ChatFormatting.GRAY), 5, 50, 15);
            sendSummary(owner, false);
        }
        bot.swing(InteractionHand.MAIN_HAND);
        BotManager.later(60, bot::removeBot);
    }

    private void sendSummary(ServerPlayer owner, boolean won) {
        if (owner == null) return;
        int t = Math.max(0, age - startTick);
        owner.sendSystemMessage(Messages.prefix()
                .append(Component.literal(won ? "✔ You won" : "✘ You lost").withStyle(won ? ChatFormatting.GREEN : ChatFormatting.RED, ChatFormatting.BOLD))
                .append(Component.literal("  vs " + d.label + " · " + kit.label + "  ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("⏱ " + Messages.time(t)).withStyle(ChatFormatting.WHITE)));
        owner.sendSystemMessage(Component.literal("   Your hits: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(ownerHits)).withStyle(ChatFormatting.AQUA))
                .append(Component.literal("  •  Bot hits: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(botHits)).withStyle(ChatFormatting.RED))
                .append(Component.literal("  •  Your best combo: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(ownerBestCombo)).withStyle(ChatFormatting.AQUA))
                .append(Component.literal("  •  Bot best combo: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(botBestCombo)).withStyle(ChatFormatting.RED)));
    }

    // =====================================================================
    //  main tick
    // =====================================================================
    void tick() {
        age++;
        forward = 0; strafe = 0; jump = false; sprint = false;

        // bots get hungry like players but "eat" off-screen so they can always sprint
        if (bot.getFoodData().getFoodLevel() < 18) bot.getFoodData().setFoodLevel(20);

        ServerPlayer owner = owner();
        if (owner == null || owner.level() != bot.level()) {
            if (++noOwnerTicks > 400) { if (run != null) BotManager.endlessLost(null, run); bot.removeBot(); }
            idle();
            return;
        }
        noOwnerTicks = 0;

        if (fightOver) { idle(); lookAt(owner.getEyePosition(), 10f); apply(); return; }

        if (countdown > 0) { countdownTick(owner); apply(); return; }

        if (!owner.isAlive() || owner.isDeadOrDying()) { ownerLost(owner); apply(); return; }

        target = pickTarget(owner);
        if (target == null) { idle(); apply(); return; }

        observe(target);
        double dist = bot.distanceTo(target);

        totemTick();
        if (bot.isUsingItem() && !isUsing(Items.BOW) && !isUsing(Items.SHIELD)) {
            // eating / drinking: step back if it's dangerous and keep looking at the target
            lookAt(aimPoint(target), d.turnSpeed);
            if (d.spacing && dist < 4) { forward = -1; strafe = strafeDir; }
            apply();
            return;
        }

        if (healTick(dist) || buffTick(dist)) { apply(); return; }

        boolean acted = false;
        if (kit == Kit.CRYSTAL) acted = crystalTick(dist);
        if (!acted && kit == Kit.MACE) acted = maceTick(dist);
        if (!acted && d.pearls && dist > 16 && dist < 60) acted = pearlTick(dist);
        if (!acted && d.bow && dist > 9 && dist < 32) acted = bowTick(dist);
        if (!acted) meleeTick(dist);
        apply();
    }

    private LivingEntity pickTarget(ServerPlayer owner) {
        if (attacker != null && (age > attackerUntil || !attacker.isAlive() || attacker.level() != bot.level()
                || bot.distanceTo(attacker) > 24 || isPeaceful(attacker))) attacker = null;
        if (attacker != null && d.spacing) return attacker; // good bots punish whoever hits them
        if (isPeaceful(owner) || bot.distanceTo(owner) > 64) return null;
        return owner;
    }

    private static boolean isPeaceful(LivingEntity e) {
        return e instanceof Player p && (p.isCreative() || p.isSpectator());
    }

    private void countdownTick(ServerPlayer owner) {
        if (countdown == COUNTDOWN) {
            Messages.title(owner, Component.literal("3").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    Component.literal(d.label + " Bot · " + kit.label).withStyle(d.color), 0, 22, 0);
        } else if (countdown == 40) {
            Messages.title(owner, Component.literal("2").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), Component.literal(d.label + " Bot · " + kit.label).withStyle(d.color), 0, 22, 0);
        } else if (countdown == 20) {
            Messages.title(owner, Component.literal("1").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), Component.literal(d.label + " Bot · " + kit.label).withStyle(d.color), 0, 22, 0);
        }
        countdown--;
        lookAt(owner.getEyePosition(), 20f);
        if (countdown == 0) {
            Messages.title(owner, Component.literal("FIGHT!").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), Component.empty(), 0, 20, 10);
            startTick = age;
            critPlan = rng.nextFloat() < d.critChance;
        }
    }

    private void idle() {
        releaseAll();
        forward = 0; strafe = 0;
        apply();
    }

    private void releaseAll() {
        if (bot.isUsingItem()) bot.releaseUsingItem();
        shieldUp = false;
        bowStart = -1;
    }

    /** writes this tick's "keys" into the player */
    private void apply() {
        bot.zza = Mth.clamp(forward, -1f, 1f);
        bot.xxa = Mth.clamp(strafe, -1f, 1f);
        if (bot.isUsingItem()) { bot.zza *= 0.2f; bot.xxa *= 0.2f; }
        bot.setJumping(jump);
        boolean canSprint = sprint && forward > 0 && !bot.isUsingItem() && bot.getFoodData().getFoodLevel() > 6 && age >= resprintAt;
        bot.setSprinting(canSprint);
        bot.resetLastActionTime();
    }

    // =====================================================================
    //  seeing & aiming
    // =====================================================================
    private void observe(LivingEntity t) {
        seen.addLast(t.position());
        while (seen.size() > d.reaction + 2) seen.removeFirst();
        Vec3[] arr = seen.toArray(new Vec3[0]);
        int idx = Math.max(0, arr.length - 1 - d.reaction);
        seenPos = arr[idx];
        seenVel = idx > 0 ? arr[idx].subtract(arr[idx - 1]) : Vec3.ZERO;
        // the bot assumes you keep moving the same way while it reacts
        seenPos = seenPos.add(seenVel.scale(d.reaction));
    }

    private Vec3 aimPoint(LivingEntity t) {
        Vec3 base = (seenPos != null ? seenPos : t.position());
        double h = t.getBbHeight();
        // aim at the closest point of the hitbox between feet and head
        double eyeY = bot.getEyeY();
        double y = Mth.clamp(eyeY, base.y + 0.2, base.y + h * 0.9);
        return new Vec3(base.x, y, base.z);
    }

    private void lookAt(Vec3 p, float maxTurn) {
        if (age >= nextNoise) {
            nextNoise = age + 4 + rng.nextInt(6);
            aimNoiseYaw = (float) (rng.nextGaussian() * d.aimError);
            aimNoisePitch = (float) (rng.nextGaussian() * d.aimError * 0.5);
        }
        Vec3 eye = bot.getEyePosition();
        double dx = p.x - eye.x, dy = p.y - eye.y, dz = p.z - eye.z;
        double hd = Math.sqrt(dx * dx + dz * dz);
        float wantYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f + aimNoiseYaw;
        float wantPitch = (float) (-(Mth.atan2(dy, hd) * Mth.RAD_TO_DEG)) + aimNoisePitch;
        look(wantYaw, wantPitch, maxTurn);
    }

    private void look(float wantYaw, float wantPitch, float maxTurn) {
        float yaw = bot.getYRot();
        float pitch = bot.getXRot();
        float dy = Mth.wrapDegrees(wantYaw - yaw);
        float dp = wantPitch - pitch;
        yaw += Mth.clamp(dy, -maxTurn, maxTurn);
        pitch += Mth.clamp(dp, -maxTurn, maxTurn);
        setRot(yaw, pitch);
    }

    private void setRot(float yaw, float pitch) {
        pitch = Mth.clamp(pitch, -90f, 90f);
        bot.setYRot(yaw);
        bot.setXRot(pitch);
        bot.setYHeadRot(yaw);
    }

    private float angleTo(Vec3 p) {
        Vec3 eye = bot.getEyePosition();
        float want = (float) (Mth.atan2(p.z - eye.z, p.x - eye.x) * Mth.RAD_TO_DEG) - 90f;
        return Math.abs(Mth.wrapDegrees(want - bot.getYRot()));
    }

    private boolean inReach(Entity t, double reach) {
        AABB box = t.getBoundingBox();
        return box.distanceToSqr(bot.getEyePosition()) <= reach * reach;
    }

    // =====================================================================
    //  melee: sprinting, strafing, crits, w-tap, shield, axe
    // =====================================================================
    private void meleeTick(double dist) {
        LivingEntity t = target;
        if (isUsing(Items.BOW)) { bot.releaseUsingItem(); bowStart = -1; }
        boolean blocking = t.isBlocking();

        // weapon choice
        if (blocking && d.axeVsShield && has(BotBrain::isAxe)) select(BotBrain::isAxe);
        else selectWeapon();

        Vec3 aim = aimPoint(t);
        lookAt(aim, d.turnSpeed);

        float charge = bot.getAttackStrengthScale(0.5f);
        float delay = bot.getCurrentItemAttackStrengthDelay();
        int ticksToReady = (int) Math.ceil(Math.max(0, d.minCharge * delay - bot.getAttackStrengthScale(0f) * delay));

        // ---- movement ----
        double want = 2.4;
        if (d.spacing && charge < 0.75f && t instanceof Player) want = 3.4; // stay just outside their reach while recharging
        if (dist > want + 0.4) { forward = 1; sprint = true; }
        else if (dist < want - 0.7) forward = -1;
        else forward = d.spacing ? 0.4f : 1f;
        if (forward > 0) sprint = true;

        if (d.strafe > 0 && dist < 6) {
            if (age >= strafeSwitchAt || bot.horizontalCollision) {
                strafeDir = rng.nextBoolean() ? 1 : -1;
                strafeSwitchAt = age + 8 + rng.nextInt((int) (10 + 30 * (1.2f - d.strafe)));
            }
            strafe = strafeDir * d.strafe;
        }
        if (bot.horizontalCollision && bot.onGround()) jump = true; // step over things

        // ---- shield ----
        ItemStack off = bot.getOffhandItem();
        boolean hasShield = off.is(Items.SHIELD);
        if (d.shield && hasShield && t instanceof Player && dist < 4.5 && charge < 0.55f && !critPlan) {
            if (!bot.isUsingItem()) { bot.gameMode.useItem(bot, bot.level(), off, InteractionHand.OFF_HAND); nShield++; }
            shieldUp = true;
            sprint = false;
            return;
        }
        if (shieldUp && bot.isUsingItem() && isUsing(Items.SHIELD)) { bot.releaseUsingItem(); }
        shieldUp = false;

        // ---- crit jump ----
        boolean modernCrit = critPlan && !blocking;
        if (modernCrit && bot.onGround() && ticksToReady <= 5 && dist < d.reach + 1.6 && age - jumpedAt > 12) {
            jump = true;
            jumpedAt = age;
        }
        boolean falling = !bot.onGround() && bot.getDeltaMovement().y < -0.02 && bot.fallDistance > 0;
        if (modernCrit && !bot.onGround()) sprint = false; // crits only happen when not sprinting

        // ---- swing ----
        if (age < nextClickAt) return;
        if (charge < d.minCharge) return;
        if (!inReach(t, d.reach)) return;
        if (angleTo(t.position()) > 35f) return;
        if (!bot.hasLineOfSight(t)) return;
        if (modernCrit && !falling) {
            // waiting for the fall - unless the jump went wrong, then just hit
            if (age - jumpedAt < 14 || !bot.onGround()) return;
        }
        swingAt(t);
    }

    private void swingAt(LivingEntity t) {
        if (rng.nextFloat() < d.missChance) {
            bot.swing(InteractionHand.MAIN_HAND); // whiff
            nMiss++;
        } else {
            if (!bot.onGround() && bot.fallDistance > 0 && !bot.isSprinting()) nCrit++;
            if (isAxe(bot.getMainHandItem()) && t.isBlocking()) nAxe++;
            bot.attack(t);
            bot.swing(InteractionHand.MAIN_HAND);
            if (t.getUUID().equals(ownerId)) {
                botHits++;
                botCombo++;
                botBestCombo = Math.max(botBestCombo, botCombo);
                ownerCombo = 0;
            }
        }
        nextClickAt = age + (d.clickDelay > 0 ? rng.nextInt(d.clickDelay + 1) : 0);
        // w-tap: let go of sprint for a moment so the next hit deals full knockback again
        resprintAt = age + (rng.nextFloat() < d.wtapChance ? 1 + rng.nextInt(2) : 6 + rng.nextInt(8));
        critPlan = rng.nextFloat() < d.critChance;
    }

    private void selectWeapon() {
        if (kit == Kit.MACE && !bot.onGround() && bot.fallDistance > 1.5 && has(s -> s.is(Items.MACE))) { select(s -> s.is(Items.MACE)); return; }
        if (select(s -> s.is(Items.NETHERITE_SWORD))) return;
        if (select(s -> s.is(Items.DIAMOND_SWORD))) return;
        if (select(s -> s.is(Items.IRON_SWORD))) return;
        if (select(BotBrain::isAxe)) return;
        select(s -> s.is(Items.MACE));
    }

    private static boolean isAxe(ItemStack s) {
        return s.is(Items.NETHERITE_AXE) || s.is(Items.DIAMOND_AXE) || s.is(Items.IRON_AXE);
    }

    // =====================================================================
    //  survival: totems, healing, potions
    // =====================================================================
    private void totemTick() {
        if (bot.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) { totemMissingSince = -1; return; }
        if (bot.getOffhandItem().is(Items.SHIELD) && kit == Kit.DIAMOND) return;
        int slot = find(s -> s.is(Items.TOTEM_OF_UNDYING));
        if (slot < 0) return;
        if (totemMissingSince < 0) totemMissingSince = age;
        if (age - totemMissingSince < d.totemDelay) return;
        if (bot.isUsingItem() && bot.getUsedItemHand() == InteractionHand.OFF_HAND) bot.releaseUsingItem();
        Inventory inv = bot.getInventory();
        ItemStack totem = inv.getItem(slot);
        ItemStack old = bot.getOffhandItem();
        inv.setItem(slot, old.isEmpty() ? ItemStack.EMPTY : old.copy());
        bot.setItemSlot(EquipmentSlot.OFFHAND, totem.copy());
        totemMissingSince = -1;
        nTotem++;
    }

    private boolean healTick(double dist) {
        float hp = bot.getHealth();
        boolean danger = dist < 3.2;

        // splash healing (NethPot): look down and throw at your own feet
        if (hp <= d.healAt && age >= nextPotAt && has(BotBrain::isHealPot)) {
            if (d.spacing && danger && hp > 6) { retreat(); }
            select(BotBrain::isHealPot);
            throwDown(88f);
            nPot++;
            nextPotAt = age + (hp <= 6 && d.potDelay <= 8 ? 3 : d.potDelay);
            return true;
        }

        // golden apples
        float eatAt = kit == Kit.CRYSTAL ? Math.max(d.healAt, 14f) : d.healAt;
        boolean crystalAbsorb = kit == Kit.CRYSTAL && d.spacing && bot.getAbsorptionAmount() <= 0 && hp < 18;
        if ((hp <= eatAt || crystalAbsorb) && (!danger || !d.spacing || hp <= 6)) {
            Predicate<ItemStack> apple = hp <= 8 && has(s -> s.is(Items.ENCHANTED_GOLDEN_APPLE))
                    ? s -> s.is(Items.ENCHANTED_GOLDEN_APPLE)
                    : s -> s.is(Items.GOLDEN_APPLE);
            if (has(apple) && !bot.getCooldowns().isOnCooldown(new ItemStack(Items.GOLDEN_APPLE))) {
                select(apple);
                lookAt(aimPoint(target), d.turnSpeed);
                bot.gameMode.useItem(bot, bot.level(), bot.getMainHandItem(), InteractionHand.MAIN_HAND);
                nApple++;
                if (d.spacing && danger) retreat();
                return true;
            }
        }
        if (danger && hp <= d.healAt && d.spacing && has(s -> s.is(Items.GOLDEN_APPLE) || isHealPot(s))) {
            // too close to heal safely: back off first
            lookAt(aimPoint(target), d.turnSpeed);
            retreat();
            return true;
        }
        return false;
    }

    /** quick flick down, throw the held item at your own feet, look back up (like real pot players) */
    private void throwDown(float pitch) {
        float yaw = bot.getYRot(), oldPitch = bot.getXRot();
        setRot(yaw, pitch);
        bot.gameMode.useItem(bot, bot.level(), bot.getMainHandItem(), InteractionHand.MAIN_HAND);
        bot.swing(InteractionHand.MAIN_HAND);
        setRot(yaw, oldPitch);
    }

    private void retreat() {
        forward = -1;
        strafe = strafeDir;
        if (bot.horizontalCollision && bot.onGround()) jump = true;
    }

    private boolean buffTick(double dist) {
        if (age < nextBuffAt) return false;
        if (buff(MobEffects.STRENGTH, dist) || buff(MobEffects.SPEED, dist)) { nextBuffAt = age + 10; return true; }
        if (bot.isOnFire() && buff(MobEffects.FIRE_RESISTANCE, dist)) { nextBuffAt = age + 10; return true; }
        return false;
    }

    private boolean buff(Holder<MobEffect> effect, double dist) {
        if (bot.hasEffect(effect)) return false;
        Predicate<ItemStack> splash = s -> s.is(Items.SPLASH_POTION) && hasEffect(s, effect);
        Predicate<ItemStack> drink = s -> s.is(Items.POTION) && hasEffect(s, effect);
        if (has(splash)) {
            select(splash);
            throwDown(88f);
            return true;
        }
        if (has(drink) && dist > 5) {
            select(drink);
            bot.gameMode.useItem(bot, bot.level(), bot.getMainHandItem(), InteractionHand.MAIN_HAND);
            return true;
        }
        return false;
    }

    private static boolean isHealPot(ItemStack s) {
        return s.is(Items.SPLASH_POTION) && hasEffect(s, MobEffects.INSTANT_HEALTH);
    }

    private static boolean hasEffect(ItemStack s, Holder<MobEffect> effect) {
        PotionContents pc = s.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        if (pc == null) return false;
        for (var inst : pc.getAllEffects()) if (inst.getEffect().equals(effect)) return true;
        return false;
    }

    // =====================================================================
    //  ranged: bow & ender pearls
    // =====================================================================
    private boolean bowTick(double dist) {
        if (!has(s -> s.is(Items.BOW)) || !has(s -> s.is(Items.ARROW) || s.is(Items.SPECTRAL_ARROW) || s.is(Items.TIPPED_ARROW))) return false;
        if (bot.isUsingItem() && !isUsing(Items.BOW)) return false;
        select(s -> s.is(Items.BOW));
        if (!bot.isUsingItem()) {
            bot.gameMode.useItem(bot, bot.level(), bot.getMainHandItem(), InteractionHand.MAIN_HAND);
            bowStart = age;
        }
        // strafe while drawing
        if (age >= strafeSwitchAt) { strafeDir = rng.nextBoolean() ? 1 : -1; strafeSwitchAt = age + 10 + rng.nextInt(15); }
        strafe = strafeDir;
        int drawn = bot.getTicksUsingItem();
        float power = Math.min(1f, (drawn / 20f) * (drawn / 20f + 2f) / 3f);
        aimBallistic(target, 3.0 * Math.max(power, 0.95), 0.05, 0.99);
        int need = 21 + (d.clickDelay > 2 ? rng.nextInt(6) : 0);
        if (drawn >= need) { bot.releaseUsingItem(); bowStart = -1; nArrow++; }
        return true;
    }

    private boolean pearlTick(double dist) {
        if (age < nextPearlAt || !has(s -> s.is(Items.ENDER_PEARL))) return false;
        if (bot.getCooldowns().isOnCooldown(new ItemStack(Items.ENDER_PEARL))) return false;
        if (bot.isUsingItem()) bot.releaseUsingItem();
        select(s -> s.is(Items.ENDER_PEARL));
        // land a little in front of the target, not on its head
        Vec3 toBot = bot.position().subtract(target.position()).normalize().scale(2.5);
        if (!aimBallisticAt(target.position().add(toBot), 1.5, 0.03, 0.99)) return false;
        bot.gameMode.useItem(bot, bot.level(), bot.getMainHandItem(), InteractionHand.MAIN_HAND);
        bot.swing(InteractionHand.MAIN_HAND);
        nextPearlAt = age + 60 + rng.nextInt(60);
        nPearl++;
        return true;
    }

    private void aimBallistic(LivingEntity t, double v, double g, double drag) {
        Vec3 p = (seenPos != null ? seenPos : t.position()).add(0, t.getBbHeight() * 0.55, 0);
        double flight = bot.getEyePosition().distanceTo(p) / v;
        Vec3 lead = p.add(seenVel.scale(flight));
        aimBallisticAt(lead, v, g, drag);
    }

    /** turns the head so a projectile with speed v and gravity g lands on p. false if out of range */
    private boolean aimBallisticAt(Vec3 p, double v, double g, double drag) {
        Vec3 eye = bot.getEyePosition();
        double dx = p.x - eye.x, dz = p.z - eye.z, dy = p.y - eye.y;
        double x = Math.sqrt(dx * dx + dz * dz) * (1.0 + (1.0 - drag) * 2.0);
        double v2 = v * v;
        double disc = v2 * v2 - g * (g * x * x + 2 * dy * v2);
        if (disc < 0) return false;
        double angle = Math.atan((v2 - Math.sqrt(disc)) / (g * x));
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f + aimNoiseYaw * 0.5f;
        float pitch = (float) (-angle * Mth.RAD_TO_DEG) + aimNoisePitch * 0.5f;
        look(yaw, pitch, Math.max(d.turnSpeed, 25f));
        return true;
    }

    // =====================================================================
    //  crystal PvP: place obsidian, place crystal, break crystal
    // =====================================================================
    private boolean crystalTick(double dist) {
        if (dist > 6.5 || !(target instanceof Player)) return false;
        if (!has(s -> s.is(Items.END_CRYSTAL))) return false;
        ServerLevel level = bot.level();

        // keep a safe distance and circle
        lookAt(aimPoint(target), d.turnSpeed);
        if (dist < 2.8) forward = -1; else if (dist > 4.5) { forward = 1; sprint = true; }
        if (age >= strafeSwitchAt || bot.horizontalCollision) { strafeDir = rng.nextBoolean() ? 1 : -1; strafeSwitchAt = age + 10 + rng.nextInt(20); }
        strafe = strafeDir * Math.max(0.4f, d.strafe);
        if (bot.horizontalCollision && bot.onGround()) jump = true;

        if (age < nextCrystalAt) return true;
        boolean hasTotem = bot.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
        float selfHp = bot.getHealth() + bot.getAbsorptionAmount();

        // 1) break a crystal next to the target
        List<EndCrystal> crystals = level.getEntitiesOfClass(EndCrystal.class, bot.getBoundingBox().inflate(6));
        EndCrystal best = null; double bestScore = -99;
        for (EndCrystal c : crystals) {
            if (!inReach(c, 4.5)) continue;
            double td = c.position().distanceTo(target.position());
            double sd = c.position().distanceTo(bot.position());
            if (td > 5) continue;
            if (!hasTotem && selfHp < 12 && sd < 4) continue; // would kill myself
            double score = crystalScore(td, sd, hasTotem);
            if (score > bestScore) { bestScore = score; best = c; }
        }
        if (best != null) {
            lookAt(best.position().add(0, 0.5, 0), 180f);
            selectWeapon();
            bot.attack(best);
            bot.swing(InteractionHand.MAIN_HAND);
            nextCrystalAt = age + d.crystalDelay;
            nCrystalBreak++;
            return true;
        }

        // 2) place a crystal on obsidian / bedrock near the target
        BlockPos base = findCrystalBase(level, hasTotem);
        if (base != null) {
            select(s -> s.is(Items.END_CRYSTAL));
            Vec3 hit = Vec3.atCenterOf(base).add(0, 0.5, 0);
            lookAt(hit, 180f);
            bot.gameMode.useItemOn(bot, level, bot.getMainHandItem(), InteractionHand.MAIN_HAND, new BlockHitResult(hit, Direction.UP, base, false));
            bot.swing(InteractionHand.MAIN_HAND);
            nextCrystalAt = age + Math.max(1, d.crystalDelay / 2);
            nCrystalPlace++;
            return true;
        }

        // 3) no base: put down obsidian next to the target
        if (has(s -> s.is(Items.OBSIDIAN))) {
            BlockPos spot = findObsidianSpot(level);
            if (spot != null) {
                BlockPos below = spot.below();
                select(s -> s.is(Items.OBSIDIAN));
                Vec3 hit = Vec3.atCenterOf(below).add(0, 0.5, 0);
                lookAt(hit, 180f);
                bot.gameMode.useItemOn(bot, level, bot.getMainHandItem(), InteractionHand.MAIN_HAND, new BlockHitResult(hit, Direction.UP, below, false));
                bot.swing(InteractionHand.MAIN_HAND);
                nextCrystalAt = age + d.crystalDelay;
                nObsidian++;
                return true;
            }
        }
        // nothing to do with crystals right now -> sword
        return dist > 3.4;
    }

    private static double crystalScore(double targetDist, double selfDist, boolean hasTotem) {
        double hurtThem = Math.max(0, 1 - targetDist / 12.0);
        double hurtMe = Math.max(0, 1 - selfDist / 12.0);
        return hurtThem - hurtMe * (hasTotem ? 0.35 : 0.9);
    }

    private BlockPos findCrystalBase(ServerLevel level, boolean hasTotem) {
        BlockPos feet = target.blockPosition();
        BlockPos best = null; double bestScore = -99;
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = -1; dy <= 0; dy++) {
            BlockPos b = feet.offset(dx, dy, dz);
            BlockState st = level.getBlockState(b);
            if (!st.is(Blocks.OBSIDIAN) && !st.is(Blocks.BEDROCK)) continue;
            BlockPos up = b.above();
            if (!level.getBlockState(up).isAir()) continue;
            AABB column = new AABB(up.getX(), up.getY(), up.getZ(), up.getX() + 1, up.getY() + 2, up.getZ() + 1);
            if (!level.getEntities((Entity) null, column, e -> true).isEmpty()) continue;
            Vec3 hit = Vec3.atCenterOf(b).add(0, 0.5, 0);
            if (bot.getEyePosition().distanceToSqr(hit) > 4.5 * 4.5) continue;
            Vec3 crystal = Vec3.atBottomCenterOf(up);
            double td = crystal.distanceTo(target.position());
            double sd = crystal.distanceTo(bot.position());
            if (td > 3.6) continue;
            double score = crystalScore(td, sd, hasTotem);
            if (score > bestScore) { bestScore = score; best = b; }
        }
        return best;
    }

    private BlockPos findObsidianSpot(ServerLevel level) {
        BlockPos feet = target.blockPosition();
        BlockPos best = null; double bestDist = 99;
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (dx == 0 && dz == 0) continue;
            BlockPos p = feet.offset(dx, 0, dz);
            if (!level.getBlockState(p).canBeReplaced() || !level.getBlockState(p.above()).isAir()) continue;
            if (!level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) continue;
            AABB box = new AABB(p);
            if (!level.getEntities((Entity) null, box, e -> e instanceof LivingEntity).isEmpty()) continue;
            Vec3 hit = Vec3.atCenterOf(p.below()).add(0, 0.5, 0);
            if (bot.getEyePosition().distanceToSqr(hit) > 4.5 * 4.5) continue;
            double td = Vec3.atBottomCenterOf(p).distanceTo(target.position());
            double sd = Vec3.atBottomCenterOf(p).distanceTo(bot.position());
            if (sd < 1.6) continue;
            double score = td - sd * 0.15;
            if (score < bestDist) { bestDist = score; best = p; }
        }
        return best;
    }

    // =====================================================================
    //  mace: wind charge jump -> smash
    // =====================================================================
    private boolean maceTick(double dist) {
        if (!d.maceSmash || !has(s -> s.is(Items.MACE))) return false;
        boolean hasWind = has(s -> s.is(Items.WIND_CHARGE));
        switch (mace) {
            case GROUND -> {
                if (!hasWind || !bot.onGround() || age < nextWindAt || dist > 4.2 || dist < 1.2) return false;
                if (bot.getCooldowns().isOnCooldown(new ItemStack(Items.WIND_CHARGE))) return false;
                if (bot.isUsingItem()) bot.releaseUsingItem();
                select(s -> s.is(Items.WIND_CHARGE));
                throwDown(90f);
                nWind++;
                mace = MaceState.LAUNCH;
                maceStateAt = age;
                nextWindAt = age + 30 + (d.clickDelay * 4);
                return true;
            }
            case LAUNCH -> {
                select(s -> s.is(Items.MACE));
                lookAt(aimPoint(target), d.turnSpeed);
                if (!bot.onGround() && bot.getDeltaMovement().y > 0.1) { mace = MaceState.AIR; maceStateAt = age; }
                else if (age - maceStateAt > 8) { mace = MaceState.GROUND; return false; }
                return true;
            }
            case AIR -> {
                select(s -> s.is(Items.MACE));
                lookAt(aimPoint(target), Math.max(d.turnSpeed, 30f));
                // drift towards the target while in the air
                double hd = Math.sqrt(Math.pow(target.getX() - bot.getX(), 2) + Math.pow(target.getZ() - bot.getZ(), 2));
                forward = hd > 0.8 ? 1 : 0;
                sprint = false;
                boolean falling = bot.getDeltaMovement().y < 0;
                if (falling && bot.fallDistance > 1.5 && inReach(target, d.reach) && bot.getAttackStrengthScale(0.5f) >= 0.9f) {
                    bot.attack(target);
                    bot.swing(InteractionHand.MAIN_HAND);
                    nSmash++;
                    if (target.getUUID().equals(ownerId)) { botHits++; botCombo++; botBestCombo = Math.max(botBestCombo, botCombo); ownerCombo = 0; }
                    // wind burst throws us up again - stay in the air state
                    maceStateAt = age;
                }
                if (bot.onGround() && age - maceStateAt > 3) { mace = MaceState.GROUND; }
                if (age - maceStateAt > 80) mace = MaceState.GROUND;
                return true;
            }
        }
        return false;
    }

    // =====================================================================
    //  inventory helpers
    // =====================================================================
    private boolean isUsing(net.minecraft.world.item.Item item) {
        return bot.isUsingItem() && bot.getUseItem().is(item);
    }

    private int find(Predicate<ItemStack> p) {
        Inventory inv = bot.getInventory();
        for (int i = 0; i < 36; i++) if (!inv.getItem(i).isEmpty() && p.test(inv.getItem(i))) return i;
        return -1;
    }

    private boolean has(Predicate<ItemStack> p) { return find(p) >= 0; }

    /** puts a matching item in the hand (moving it to the hotbar if needed) */
    private boolean select(Predicate<ItemStack> p) {
        Inventory inv = bot.getInventory();
        ItemStack held = inv.getItem(inv.getSelectedSlot());
        if (!held.isEmpty() && p.test(held)) return true;
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && p.test(s)) { inv.setSelectedSlot(i); return true; }
        }
        for (int i = 9; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && p.test(s)) {
                int hb = -1;
                for (int j = 8; j >= 0; j--) if (inv.getItem(j).isEmpty()) { hb = j; break; }
                if (hb < 0) hb = 8;
                ItemStack swap = inv.getItem(hb);
                inv.setItem(hb, s);
                inv.setItem(i, swap);
                inv.setSelectedSlot(hb);
                return true;
            }
        }
        return false;
    }
}
