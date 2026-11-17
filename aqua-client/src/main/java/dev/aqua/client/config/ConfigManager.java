package dev.aqua.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.aqua.client.AquaClient;
import dev.aqua.client.module.HudModule;
import dev.aqua.client.module.Module;
import dev.aqua.client.module.ModuleManager;
import dev.aqua.client.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Сохраняет всё в config/aquaclient.json. */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("aquaclient.json");

    private ConfigManager() {}

    public static void load() {
        if (!Files.exists(FILE)) return;
        try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root == null) return;
            if (root.has("lang")) dev.aqua.client.util.Lang.setLang(root.get("lang").getAsInt());
            if (!root.has("modules")) return;
            applyModules(root.getAsJsonObject("modules"));
        } catch (Exception e) {
            AquaClient.LOG.error("Не удалось загрузить конфиг Aqua Client", e);
        }
    }

    /** Применить состояние модулей из объекта (включённость, настройки, клавиши, позиции HUD). */
    public static void applyModules(JsonObject mods) {
        if (mods == null) return;
        for (Module m : ModuleManager.all()) {
                if (!mods.has(m.getName())) continue;
                JsonObject o = mods.getAsJsonObject(m.getName());
                if (o.has("settings")) {
                    JsonObject s = o.getAsJsonObject("settings");
                    for (Setting<?> set : m.getSettings()) {
                        JsonElement e = s.get(set.getName());
                        if (e != null) {
                            try { set.fromJson(e); } catch (Exception ignored) {}
                        }
                    }
                }
                if (o.has("key")) m.setKey(o.get("key").getAsInt());
                if (o.has("holdKey")) m.setHoldKey(o.get("holdKey").getAsBoolean());
                if (m instanceof HudModule h) {
                    if (o.has("x")) h.x = o.get("x").getAsFloat();
                    if (o.has("y")) h.y = o.get("y").getAsFloat();
                }
                if (o.has("enabled")) m.loadEnabled(o.get("enabled").getAsBoolean());
            }
    }

    /** Снимок состояния всех модулей — для конфига и для сборок. */
    public static JsonObject snapshotModules() {
        JsonObject mods = new JsonObject();
        for (Module m : ModuleManager.all()) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.isEnabled());
            o.addProperty("key", m.getKey());
            o.addProperty("holdKey", m.isHoldKey());
            if (m instanceof HudModule h) {
                o.addProperty("x", h.x);
                o.addProperty("y", h.y);
            }
            JsonObject s = new JsonObject();
            for (Setting<?> set : m.getSettings()) {
                JsonElement j = set.toJson();
                if (j != null && !j.isJsonNull()) s.add(set.getName(), j);
            }
            o.add("settings", s);
            mods.add(m.getName(), o);
        }
        return mods;
    }

    public static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.addProperty("lang", dev.aqua.client.util.Lang.getLang());
        root.add("modules", snapshotModules());
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) { GSON.toJson(root, w); }
        } catch (Exception e) {
            AquaClient.LOG.error("Не удалось сохранить конфиг Aqua Client", e);
        }
    }
}
