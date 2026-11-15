package dev.aqua.client.module.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.aqua.client.AquaClient;
import dev.aqua.client.cosmetic.CosmeticRegistry;
import dev.aqua.client.cosmetic.Slot;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.module.ModuleManager;
import dev.aqua.client.net.AquaNet;
import dev.aqua.client.net.LocalSkin;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ModeSetting;
import dev.aqua.client.setting.NumberSetting;
import dev.aqua.client.setting.StringSetting;
import dev.aqua.client.util.Lang;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Гардероб Aqua Client: плащи, шляпы, очки, крылья, рюкзаки, питомцы, ауры и значок у ника.
 * Всё бесплатно; другие игроки Aqua Launcher видят косметику через Aqua Network.
 */
public class Cosmetics extends Module {
    private static Cosmetics instance;
    private final Map<Slot, ModeSetting> slots = new EnumMap<>(Slot.class);
    private final BoolSetting elytra = add(new BoolSetting("Узор на элитрах", true));
    private final NumberSetting auraAmount = add(new NumberSetting("Сила ауры", 2, 1, 6, 1));
    private final StringSetting appliedStamp = add(new StringSetting("stamp", "", 64), () -> false);

    public Cosmetics() {
        super("Cosmetics", "Плащ Aqua Client и аура частиц", Category.COSMETIC);
        instance = this;
        String[] defaults = {"aqua", "none", "none", "none", "none", "turtle", "bubbles", "turtle"};
        Slot[] order = Slot.values();
        for (int i = 0; i < order.length; i++) {
            Slot s = order[i];
            ModeSetting m = new ModeSetting(s.ru, defaults[i], CosmeticRegistry.ids(s));
            m.withDisplay(v -> CosmeticRegistry.name(s, v, Lang.isRu()));
            slots.put(s, add(m));
        }
    }

    /** Применить гардероб, выбранный в Aqua Launcher (config/aquaclient-loadout.json). */
    public void applyLauncherLoadout() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("aquaclient-loadout.json");
        try {
            if (!Files.exists(file)) return;
            JsonObject o = new Gson().fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonObject.class);
            if (o == null) return;
            if (o.has("skinModel")) LocalSkin.setModel(o.get("skinModel").getAsString());
            AquaNetwork net = ModuleManager.get(AquaNetwork.class);
            if (net != null && o.has("shareSkin")) net.setShareSkin(o.get("shareSkin").getAsBoolean());
            String stamp = o.has("stamp") ? o.get("stamp").getAsString() : "";
            if (stamp.equals(appliedStamp.get())) return;
            for (Slot s : Slot.values()) {
                if (o.has(s.key) && CosmeticRegistry.get(s, o.get(s.key).getAsString()) != null) slots.get(s).set(o.get(s.key).getAsString());
            }
            appliedStamp.set(stamp);
            setEnabled(true);
        } catch (Exception e) {
            AquaClient.LOG.warn("Не удалось прочитать гардероб из лаунчера", e);
        }
    }

    public static String local(Slot s) {
        if (instance == null || !instance.isEnabled()) return s == Slot.ICON ? "turtle" : "none";
        return instance.slots.get(s).get();
    }

    public static JsonObject loadoutJson() {
        JsonObject o = new JsonObject();
        for (Slot s : Slot.values()) o.addProperty(s.key, local(s));
        return o;
    }

    private static Identifier tex(String name) { return dev.aqua.client.compat.V.id("aquaclient", "textures/cape/" + name + ".png"); }

    /** Текстура плаща для игрока (свой или другого игрока лаунчера). */
    public static Identifier capeTexture(UUID id) {
        String cape = AquaNet.cosmetic(id, Slot.CAPE);
        if (cape == null || cape.equals("none")) return null;
        if (cape.equals("portal")) return tex("portal_" + (System.currentTimeMillis() / 120) % 8);
        return tex(cape);
    }

    public static boolean elytra() { return instance == null || instance.elytra.isOn(); }

    private static ParticleEffect particle(String id) {
        return switch (id) {
            case "endrod" -> ParticleTypes.END_ROD;
            case "hearts" -> ParticleTypes.HEART;
            case "notes" -> ParticleTypes.NOTE;
            case "bubbles" -> ParticleTypes.BUBBLE_POP;
            case "snow" -> ParticleTypes.SNOWFLAKE;
            case "flame" -> ParticleTypes.SMALL_FLAME;
            case "totem" -> ParticleTypes.TOTEM_OF_UNDYING;
            case "cherry" -> ParticleTypes.CHERRY_LEAVES;
            case "soul" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "glow" -> ParticleTypes.GLOW;
            case "dolphin" -> ParticleTypes.DOLPHIN;
            case "drip" -> ParticleTypes.DRIPPING_WATER;
            case "happy" -> ParticleTypes.HAPPY_VILLAGER;
            case "crit" -> ParticleTypes.CRIT;
            case "enchant" -> ParticleTypes.ENCHANT;
            case "cloud" -> ParticleTypes.CLOUD;
            case "spore" -> ParticleTypes.SPORE_BLOSSOM_AIR;
            case "ash" -> ParticleTypes.WHITE_ASH;
            case "rain" -> ParticleTypes.FALLING_WATER;
            default -> ParticleTypes.PORTAL;
        };
    }

    @Override
    public void onTick() {
        if (mc.world == null || mc.player == null) return;
        AquaNetwork net = ModuleManager.get(AquaNetwork.class);
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p != mc.player && (net == null || !net.showOthers())) continue;
            if (p.isInvisible() || p.squaredDistanceTo(mc.player) > 64 * 64) continue;
            UUID id = p.getGameProfile().getId();
            if (!AquaNet.isUser(id)) continue;
            String aura = AquaNet.cosmetic(id, Slot.AURA);
            if (aura.equals("none")) continue;
            ParticleEffect pe = particle(aura);
            boolean portal = pe == ParticleTypes.PORTAL || pe == ParticleTypes.ENCHANT;
            if (!portal && (p.age + id.hashCode()) % 6 != 0) continue;
            int n = p == mc.player ? auraAmount.getInt() : 1;
            for (int i = 0; i < n; i++) {
                double a = Math.random() * Math.PI * 2, r = 0.5 + Math.random() * 0.3;
                mc.world.addParticle(pe, p.getX() + Math.cos(a) * r, p.getY() + Math.random() * p.getHeight(), p.getZ() + Math.sin(a) * r,
                        0, portal ? -0.4 : 0.02, 0);
            }
        }
    }

    @Override
    protected void onEnable() { AquaNet.pushNow(); }

    @Override
    protected void onDisable() { AquaNet.pushNow(); }
}
