package dev.aqua.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.aqua.client.module.Module;
import dev.aqua.client.module.ModuleManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Сборки (пресеты): наборы включённых модулей и их настроек. До 5 штук, как в Lunar. */
public final class Profiles {
    public static final int MAX = 5;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("aquaclient-profiles.json");
    private static final LinkedHashMap<String, JsonObject> PROFILES = new LinkedHashMap<>();
    private static String active = "";
    private static boolean loaded;

    /** Встроенные сборки — есть у всех, не занимают лимит и не удаляются. */
    private static final LinkedHashMap<String, List<String>> BUILTIN = new LinkedHashMap<>();
    static {
        BUILTIN.put("⭐ Bedwars", Arrays.asList(
                "CPS Total", "Combo", "Reach", "Attack Charge", "Sprint", "Target HP", "Velocity",
                "Coordinates", "Facing", "Blocks", "Gaps", "Fireballs", "TNT", "Bed Nearby", "Players Nearby",
                "No Blocks", "Armor Alert", "Low Hunger", "No Sword",
                "Low HP", "Health", "Enemy Close", "Void Warning", "Projectile Warning"));
    }

    /** Встроенные сборки, которые пользователь удалил (их можно удалять, они просто прячутся). */
    private static final Set<String> hiddenBuiltins = new HashSet<>();

    public static boolean isBuiltin(String name) { return BUILTIN.containsKey(name); }

    private Profiles() {}

    public static void load() {
        loaded = true;
        if (!Files.exists(FILE)) return;
        try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root == null) return;
            if (root.has("active")) active = root.get("active").getAsString();
            if (root.has("profiles")) {
                JsonObject p = root.getAsJsonObject("profiles");
                for (String name : p.keySet()) {
                    if (PROFILES.size() >= MAX) break;
                    PROFILES.put(name, p.getAsJsonObject(name));
                }
            }
            if (root.has("hiddenBuiltins"))
                for (var el : root.getAsJsonArray("hiddenBuiltins")) hiddenBuiltins.add(el.getAsString());
        } catch (Exception ignored) {}
    }

    private static void persist() {
        JsonObject root = new JsonObject();
        root.addProperty("active", active);
        JsonObject p = new JsonObject();
        for (var e : PROFILES.entrySet()) p.add(e.getKey(), e.getValue());
        root.add("profiles", p);
        com.google.gson.JsonArray hb = new com.google.gson.JsonArray();
        for (String s : hiddenBuiltins) hb.add(s);
        root.add("hiddenBuiltins", hb);
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) { GSON.toJson(root, w); }
        } catch (Exception ignored) {}
    }

    public static List<String> names() {
        if (!loaded) load();
        List<String> l = new ArrayList<>();
        for (String b : BUILTIN.keySet()) if (!hiddenBuiltins.contains(b)) l.add(b);
        l.addAll(PROFILES.keySet());
        return l;
    }

    /** Лимит считаем только по пользовательским сборкам, встроенные не в счёт. */
    public static int count() { if (!loaded) load(); return PROFILES.size(); }
    public static boolean canAdd() { return count() < MAX; }
    public static String active() { return active; }

    /** Сохранить текущее состояние модулей в сборку (новую или перезаписать). */
    public static void saveCurrent(String name) {
        if (!loaded) load();
        name = clean(name);
        if (name.isEmpty() || BUILTIN.containsKey(name)) return;
        if (!PROFILES.containsKey(name) && PROFILES.size() >= MAX) return;
        PROFILES.put(name, ConfigManager.snapshotModules());
        active = name;
        persist();
    }

    /** Применить сборку: включает/выключает модули и ставит их настройки. */
    public static void apply(String name) {
        if (!loaded) load();
        if (BUILTIN.containsKey(name)) {
            Set<String> want = new HashSet<>(BUILTIN.get(name));
            for (Module m : ModuleManager.all()) if (want.contains(m.getName()) && !m.isEnabled()) m.toggle();
            active = name;
            persist();
            ConfigManager.save();
            return;
        }
        JsonObject snap = PROFILES.get(name);
        if (snap == null) return;
        ConfigManager.applyModules(snap);
        active = name;
        persist();
        ConfigManager.save();
    }

    public static void rename(String oldName, String newName) {
        if (!loaded) load();
        if (BUILTIN.containsKey(oldName)) return;
        newName = clean(newName);
        if (newName.isEmpty() || BUILTIN.containsKey(newName) || !PROFILES.containsKey(oldName) || PROFILES.containsKey(newName)) return;
        // сохранить порядок
        LinkedHashMap<String, JsonObject> copy = new LinkedHashMap<>();
        for (var e : PROFILES.entrySet()) copy.put(e.getKey().equals(oldName) ? newName : e.getKey(), e.getValue());
        PROFILES.clear();
        PROFILES.putAll(copy);
        if (active.equals(oldName)) active = newName;
        persist();
    }

    public static void delete(String name) {
        if (!loaded) load();
        if (BUILTIN.containsKey(name)) {        // встроенную не удаляем из кода, а прячем — чтобы пропала навсегда
            hiddenBuiltins.add(name);
            if (active.equals(name)) active = "";
            persist();
            return;
        }
        PROFILES.remove(name);
        if (active.equals(name)) active = "";
        persist();
    }

    private static String clean(String s) {
        if (s == null) return "";
        s = s.trim();
        return s.length() > 18 ? s.substring(0, 18) : s;
    }
}
