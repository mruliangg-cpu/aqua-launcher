package dev.aqua.pvpbot;

import net.minecraft.ChatFormatting;

/**
 * How well a bot plays. Every number here is something a real player could do -
 * the bot never gets extra reach, extra health or hacks, it just plays better or worse.
 */
public final class Difficulty {
    public final String id;
    public final String label;
    public final ChatFormatting color;

    /** ticks between the target moving and the bot noticing (1 tick = 50 ms) */
    public final int reaction;
    /** max degrees the head turns per tick */
    public final float turnSpeed;
    /** random aim wobble in degrees */
    public final float aimError;
    /** how far away it hits from (vanilla max is 3.0) */
    public final float reach;
    /** attack charge needed before swinging (1.0 = fully charged) */
    public final float minCharge;
    /** extra random ticks it waits after the cooldown is ready (slow clicking) */
    public final int clickDelay;
    /** chance a swing misses completely */
    public final float missChance;
    /** chance to go for a jump critical hit */
    public final float critChance;
    /** chance to w-tap (sprint reset) after a sprint hit */
    public final float wtapChance;
    /** 0 = walks straight at you, 1 = circles and strafes like a good player */
    public final float strafe;
    /** backs off while its sword is recharging, then rushes back in */
    public final boolean spacing;
    /** raises its shield between hits */
    public final boolean shield;
    /** switches to an axe to break your shield */
    public final boolean axeVsShield;
    /** ticks between crystal actions (place / break) */
    public final int crystalDelay;
    /** ticks it needs to put a new totem in its off-hand */
    public final int totemDelay;
    /** heals (golden apple / potion) when health is at or below this */
    public final float healAt;
    /** ticks between thrown healing potions */
    public final int potDelay;
    /** throws ender pearls to chase you */
    public final boolean pearls;
    /** shoots a bow at range */
    public final boolean bow;
    /** uses wind charges + mace smashes */
    public final boolean maceSmash;

    private Difficulty(String id, String label, ChatFormatting color, int reaction, float turnSpeed, float aimError, float reach,
                       float minCharge, int clickDelay, float missChance, float critChance, float wtapChance, float strafe,
                       boolean spacing, boolean shield, boolean axeVsShield, int crystalDelay, int totemDelay, float healAt,
                       int potDelay, boolean pearls, boolean bow, boolean maceSmash) {
        this.id = id; this.label = label; this.color = color;
        this.reaction = reaction; this.turnSpeed = turnSpeed; this.aimError = aimError; this.reach = reach;
        this.minCharge = minCharge; this.clickDelay = clickDelay; this.missChance = missChance; this.critChance = critChance;
        this.wtapChance = wtapChance; this.strafe = strafe; this.spacing = spacing; this.shield = shield;
        this.axeVsShield = axeVsShield; this.crystalDelay = crystalDelay; this.totemDelay = totemDelay; this.healAt = healAt;
        this.potDelay = potDelay; this.pearls = pearls; this.bow = bow; this.maceSmash = maceSmash;
    }

    //                                                 react turn  aim  reach charge click miss  crit  wtap  strafe spacing shield axe  crystal totem heal pot pearls bow  mace
    public static final Difficulty EASY       = new Difficulty("easy", "Easy", ChatFormatting.GREEN,
                                                        9,   9f,  7f,  2.55f, 1.0f, 7,  0.30f, 0.0f, 0.0f, 0.0f, false, false, false, 16,  40, 5f,  24, false, false, false);
    public static final Difficulty NORMAL     = new Difficulty("normal", "Normal", ChatFormatting.YELLOW,
                                                        5,  18f,  3.5f, 2.8f, 0.97f, 3, 0.12f, 0.35f, 0.3f, 0.45f, false, false, false, 8,   16, 8f,  14, false, true,  true);
    public static final Difficulty HARD       = new Difficulty("hard", "Hard", ChatFormatting.GOLD,
                                                        3,  38f,  1.6f, 2.95f, 0.93f, 1, 0.04f, 0.75f, 0.8f, 0.85f, true,  true,  true,  4,   7,  10f, 8,  true,  true,  true);
    public static final Difficulty IMPOSSIBLE = new Difficulty("impossible", "Impossible", ChatFormatting.DARK_RED,
                                                        1,  90f,  0.35f, 3.0f, 0.92f, 0, 0.0f,  1.0f, 1.0f, 1.0f,  true,  true,  true,  2,   2,  12f, 4,  true,  true,  true);

    public static final String[] IDS = {"easy", "normal", "hard", "endless", "impossible"};

    public static Difficulty byId(String id) {
        return switch (id.toLowerCase()) {
            case "easy" -> EASY;
            case "normal" -> NORMAL;
            case "hard" -> HARD;
            case "impossible" -> IMPOSSIBLE;
            case "endless" -> endless(1);
            default -> null;
        };
    }

    /** Endless mode: wave 1 plays like Normal and every wave gets closer to Impossible (reached at wave 12). */
    public static Difficulty endless(int wave) {
        float t = Math.min(1f, Math.max(0f, (wave - 1) / 11f));
        Difficulty a = NORMAL, b = IMPOSSIBLE;
        return new Difficulty("endless", "Endless · Wave " + wave, ChatFormatting.LIGHT_PURPLE,
                Math.round(lerp(a.reaction, b.reaction, t)), lerp(a.turnSpeed, b.turnSpeed, t), lerp(a.aimError, b.aimError, t),
                lerp(a.reach, b.reach, t), lerp(a.minCharge, b.minCharge, t), Math.round(lerp(a.clickDelay, b.clickDelay, t)),
                lerp(a.missChance, b.missChance, t), lerp(a.critChance, b.critChance, t), lerp(a.wtapChance, b.wtapChance, t),
                lerp(a.strafe, b.strafe, t), t > 0.25f, t > 0.2f, t > 0.3f, Math.round(lerp(a.crystalDelay, b.crystalDelay, t)),
                Math.round(lerp(a.totemDelay, b.totemDelay, t)), lerp(a.healAt, b.healAt, t), Math.round(lerp(a.potDelay, b.potDelay, t)),
                t > 0.3f, true, true);
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
}
