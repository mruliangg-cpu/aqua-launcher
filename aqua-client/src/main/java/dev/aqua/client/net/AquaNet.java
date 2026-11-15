package dev.aqua.client.net;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.aqua.client.AquaClient;
import dev.aqua.client.cosmetic.Slot;
import dev.aqua.client.module.ModuleManager;
import dev.aqua.client.module.impl.Cosmetics;
import dev.aqua.client.module.impl.AquaNetwork;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aqua Network: игроки Aqua Launcher видят косметику, скины и значок черепахи друг друга.
 * Сервер — папка aqua-network (бесплатно разворачивается на Render).
 */
public final class AquaNet {
    public static final class Remote {
        public String name, skin, model;
        public Map<String, String> cosmetics = new HashMap<>();
        public long time;
    }

    private static final Gson GSON = new Gson();
    private static final Map<UUID, Remote> USERS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> MISSES = new ConcurrentHashMap<>();
    private static final Map<UUID, Identifier> SKINS = new ConcurrentHashMap<>();
    private static final Set<UUID> SKIN_LOADING = ConcurrentHashMap.newKeySet();
    private static HttpClient http;
    private static long lastPresence, lastLookup;
    private static String sentSkinHash;
    private static boolean online;

    private AquaNet() {}

    private static HttpClient http() {
        if (http == null) http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
        return http;
    }

    public static String api() {
        AquaNetwork m = ModuleManager.get(AquaNetwork.class);
        String u = m != null ? m.url() : "";
        if (u == null || u.isBlank()) u = System.getProperty("aqua.api", "https://aqua-network.mruliangg.workers.dev");
        if (u == null || u.isBlank()) u = "https://aqua-network.mruliangg.workers.dev";
        u = u.trim();
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u;
    }

    public static boolean isOnline() { return online; }
    public static int knownUsers() { return USERS.size(); }

    private static UUID self() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player != null ? mc.player.getGameProfile().getId() : mc.getSession() != null ? mc.getSession().getUuidOrNull() : null;
    }

    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        AquaNetwork mod = ModuleManager.get(AquaNetwork.class);
        if (mod == null || !mod.isEnabled() || mc.player == null || api().isEmpty()) return;
        long now = System.currentTimeMillis();
        if (now - lastPresence > 60_000) { lastPresence = now; presence(); }
        if (now - lastLookup > 5_000) { lastLookup = now; lookup(); }
    }

    /** Сразу отправить обновлённую косметику (после смены в меню). */
    public static void pushNow() { lastPresence = 0; }

    private static void presence() {
        MinecraftClient mc = MinecraftClient.getInstance();
        JsonObject o = new JsonObject();
        o.addProperty("uuid", mc.player.getGameProfile().getId().toString());
        o.addProperty("name", mc.player.getGameProfile().getName());
        o.addProperty("launcher", "aqua");
        o.add("cosmetics", Cosmetics.loadoutJson());
        AquaNetwork mod = ModuleManager.get(AquaNetwork.class);
        String hash = LocalSkin.hash();
        if (mod != null && mod.shareSkin() && hash != null && !hash.equals(sentSkinHash)) {
            o.addProperty("skin", LocalSkin.base64());
            o.addProperty("model", LocalSkin.model());
        }
        post("/api/presence", o, res -> {
            online = true;
            if (o.has("skin")) sentSkinHash = hash;
        });
    }

    private static void lookup() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) return;
        UUID me = self();
        long now = System.currentTimeMillis();
        JsonArray ids = new JsonArray();
        List<UUID> asked = new ArrayList<>();
        for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList()) {
            UUID id = e.getProfile().getId();
            if (id == null || id.equals(me)) continue;
            Remote r = USERS.get(id);
            Long miss = MISSES.get(id);
            if (r != null && now - r.time < 120_000) continue;
            if (r == null && miss != null && now - miss < 120_000) continue;
            ids.add(id.toString());
            asked.add(id);
            if (ids.size() >= 100) break;
        }
        if (ids.isEmpty()) return;
        JsonObject body = new JsonObject();
        body.add("uuids", ids);
        post("/api/users", body, res -> {
            online = true;
            JsonObject users = res.has("users") ? res.getAsJsonObject("users") : new JsonObject();
            for (UUID id : asked) {
                JsonElement el = users.get(id.toString());
                if (el == null || !el.isJsonObject()) { MISSES.put(id, System.currentTimeMillis()); USERS.remove(id); continue; }
                JsonObject u = el.getAsJsonObject();
                Remote r = new Remote();
                r.name = u.has("name") ? u.get("name").getAsString() : "";
                r.skin = u.has("skin") && !u.get("skin").isJsonNull() ? u.get("skin").getAsString() : null;
                r.model = u.has("model") && !u.get("model").isJsonNull() ? u.get("model").getAsString() : "classic";
                if (u.has("cosmetics") && u.get("cosmetics").isJsonObject())
                    for (Map.Entry<String, JsonElement> c : u.getAsJsonObject("cosmetics").entrySet())
                        if (c.getValue().isJsonPrimitive()) r.cosmetics.put(c.getKey(), c.getValue().getAsString());
                r.time = System.currentTimeMillis();
                Remote old = USERS.put(id, r);
                if (old != null && old.skin != null && !old.skin.equals(r.skin)) SKINS.remove(id);
                MISSES.remove(id);
            }
        });
    }

    private interface Ok { void accept(JsonObject res); }

    private static void post(String path, JsonObject body, Ok ok) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(api() + path)).timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body))).build();
            http().sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(r -> {
                if (r.statusCode() / 100 != 2) { online = false; return; }
                try { ok.accept(GSON.fromJson(r.body(), JsonObject.class)); } catch (Exception ignored) {}
            }).exceptionally(e -> { online = false; return null; });
        } catch (Exception e) {
            online = false;
        }
    }

    // ------------------------------------------------------------------ запросы рендера
    public static boolean isUser(UUID id) {
        if (id == null) return false;
        if (id.equals(self())) return true;
        return USERS.containsKey(id);
    }

    public static String cosmetic(UUID id, Slot slot) {
        if (id == null) return "none";
        if (id.equals(self())) return Cosmetics.local(slot);
        AquaNetwork mod = ModuleManager.get(AquaNetwork.class);
        if (mod != null && !mod.showOthers()) return slot == Slot.ICON ? Optional.ofNullable(USERS.get(id)).map(r -> r.cosmetics.getOrDefault("icon", "turtle")).orElse("none") : "none";
        Remote r = USERS.get(id);
        return r == null ? "none" : r.cosmetics.getOrDefault(slot.key, slot == Slot.ICON ? "turtle" : "none");
    }

    /** Текстура скина (свой скин из лаунчера или скин другого игрока лаунчера). */
    public static Identifier skin(UUID id) {
        if (id == null) return null;
        if (id.equals(self())) return LocalSkin.texture();
        Remote r = USERS.get(id);
        if (r == null || r.skin == null) return null;
        Identifier have = SKINS.get(id);
        if (have != null) return have;
        if (SKIN_LOADING.add(id)) downloadSkin(id, r.skin);
        return null;
    }

    public static String model(UUID id) {
        if (id != null && id.equals(self())) return LocalSkin.model();
        Remote r = USERS.get(id);
        return r == null ? "classic" : r.model;
    }

    private static void downloadSkin(UUID id, String hash) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(api() + "/api/skin/" + id + "?h=" + hash)).timeout(Duration.ofSeconds(15)).GET().build();
        http().sendAsync(req, HttpResponse.BodyHandlers.ofByteArray()).thenAccept(r -> {
            if (r.statusCode() != 200) { SKIN_LOADING.remove(id); return; }
            byte[] png = r.body();
            MinecraftClient.getInstance().execute(() -> {
                try {
                    NativeImage img = NativeImage.read(png);
                    Identifier tex = dev.aqua.client.compat.V.id("aquaclient", "netskin/" + id.toString().replace("-", ""));
                    MinecraftClient.getInstance().getTextureManager().registerTexture(tex, dev.aqua.client.compat.V.texture(img));
                    SKINS.put(id, tex);
                } catch (Exception e) {
                    AquaClient.LOG.debug("Skin decode failed", e);
                }
                SKIN_LOADING.remove(id);
            });
        }).exceptionally(e -> { SKIN_LOADING.remove(id); return null; });
    }

    public static void clear() { USERS.clear(); MISSES.clear(); }
}
