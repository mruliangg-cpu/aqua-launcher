package dev.aqua.client.net;

import dev.aqua.client.AquaClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

/** Свой скин, загруженный в Aqua Launcher (config/aquaclient-skin.png). */
public final class LocalSkin {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("aquaclient-skin.png");
    private static final Identifier ID = dev.aqua.client.compat.V.id("aquaclient", "skin/self");
    private static byte[] bytes;
    private static String hash, model = "classic";
    private static boolean loaded, registered;

    private LocalSkin() {}

    public static void setModel(String m) { model = "slim".equals(m) ? "slim" : "classic"; }
    public static String model() { return model; }

    private static void load() {
        if (loaded) return;
        loaded = true;
        try {
            if (Files.exists(FILE) && Files.size(FILE) < 200_000) {
                bytes = Files.readAllBytes(FILE);
                hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
            }
        } catch (Exception e) {
            AquaClient.LOG.warn("Не удалось прочитать скин", e);
        }
    }

    public static String hash() { load(); return hash; }
    public static String base64() { load(); return bytes == null ? null : Base64.getEncoder().encodeToString(bytes); }

    public static Identifier texture() {
        load();
        if (bytes == null) return null;
        if (!registered) {
            registered = true;
            try {
                NativeImage img = NativeImage.read(bytes);
                MinecraftClient.getInstance().getTextureManager().registerTexture(ID, dev.aqua.client.compat.V.texture(img));
            } catch (Exception e) {
                AquaClient.LOG.warn("Скин повреждён", e);
                bytes = null;
                return null;
            }
        }
        return ID;
    }
}
