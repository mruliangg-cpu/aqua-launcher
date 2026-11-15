package dev.aqua.client.module.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.aqua.client.AquaClient;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ButtonSetting;
import dev.aqua.client.setting.NumberSetting;
import dev.aqua.client.util.WorldText;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Точки маршрута: клавиша B ставит точку, она видна сквозь стены с расстоянием и лучом. */
public class Waypoints extends Module {
    public static class Point {
        public String name, dim, world;
        public double x, y, z;
        public int color;
    }

    private static final int[] COLORS = {0xFF3EE6FF, 0xFF5FF2C8, 0xFFFFD86B, 0xFFFF5577, 0xFF4DA6FF, 0xFF7CFC6B};
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("aquaclient-waypoints.json");
    private final List<Point> points = new ArrayList<>();
    private final BoolSetting beam = add(new BoolSetting("Луч в небо", true));
    private final BoolSetting deathPoint = add(new BoolSetting("Точка смерти", true));
    private final NumberSetting maxDistance = add(new NumberSetting("Макс. дистанция", 5000, 100, 20000, 100, "м"));
    private final NumberSetting labelScale = add(new NumberSetting("Размер надписей", 1, 0.5, 2, 0.1, "x"));

    public Waypoints() {
        super("Waypoints", "Точки маршрута: B — поставить, видно сквозь стены", Category.MISC);
        add(new ButtonSetting("Удалить ближайшую точку", "Удалить", this::removeNearest));
        add(new ButtonSetting("Удалить все точки этого мира", "Очистить", this::clearWorld));
        load();
    }

    private String worldId() {
        if (mc.isInSingleplayer() && mc.getServer() != null) return "sp:" + mc.getServer().getSaveProperties().getLevelName();
        return mc.getCurrentServerEntry() != null ? "mp:" + mc.getCurrentServerEntry().address : "unknown";
    }

    private String dim() { return mc.world == null ? "" : mc.world.getRegistryKey().getValue().toString(); }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        while (AquaClient.WAYPOINT != null && AquaClient.WAYPOINT.wasPressed()) {
            Point p = new Point();
            p.name = dev.aqua.client.util.Lang.t("Точка ", "Point ") + (points.stream().filter(x -> worldId().equals(x.world)).count() + 1);
            p.x = mc.player.getBlockX() + 0.5;
            p.y = mc.player.getBlockY();
            p.z = mc.player.getBlockZ() + 0.5;
            p.dim = dim();
            p.world = worldId();
            p.color = COLORS[points.size() % COLORS.length];
            points.add(p);
            save();
            mc.player.sendMessage(Text.literal(dev.aqua.client.util.Lang.t("§5[Aqua] §fПоставлена §d", "§5[Aqua] §fAdded §d") + p.name + " §7(" + (int) p.x + " " + (int) p.y + " " + (int) p.z + ")"), false);
        }
    }

    public void render(dev.aqua.client.compat.WorldCtx ctx) {
        if (!isEnabled() || mc.player == null || mc.world == null) return;
        String w = worldId(), d = dim();
        Vec3d eye = mc.player.getEyePos();
        for (Point p : points) {
            if (!w.equals(p.world) || !d.equals(p.dim)) continue;
            Vec3d pos = new Vec3d(p.x, p.y, p.z);
            double dist = pos.distanceTo(eye);
            if (dist > maxDistance.get()) continue;
            draw(ctx, pos, p.name, p.color, dist);
        }
        if (deathPoint.isOn()) {
            Vec3d death = DeathCoords.lastDeath(d);
            if (death != null) draw(ctx, death, dev.aqua.client.util.Lang.t("☠ Смерть", "☠ Death"), 0xFFFF5577, death.distanceTo(eye));
        }
        WorldText.flush(ctx);
    }

    private void draw(dev.aqua.client.compat.WorldCtx ctx, Vec3d pos, String name, int color, double dist) {
        // если точка далеко — рисуем надпись ближе к камере в том же направлении
        Vec3d cam = dev.aqua.client.compat.V.camPos(ctx.camera());
        Vec3d target = pos.add(0, 1.6, 0);
        Vec3d shown = dist > 96 ? cam.add(target.subtract(cam).normalize().multiply(96)) : target;
        if (beam.isOn() && dist < 512) WorldText.beam(ctx, pos, color, 256);
        WorldText.label(ctx, shown, name + " §7" + Math.round(dist) + "m", color, labelScale.getFloat(), true);
    }

    private void removeNearest() {
        if (mc.player == null) return;
        Point best = null;
        double bd = Double.MAX_VALUE;
        for (Point p : points) {
            if (!worldId().equals(p.world)) continue;
            double d = mc.player.squaredDistanceTo(p.x, p.y, p.z);
            if (d < bd) { bd = d; best = p; }
        }
        if (best != null) { points.remove(best); save(); }
    }

    private void clearWorld() {
        String w = worldId();
        points.removeIf(p -> w.equals(p.world));
        save();
    }

    private void load() {
        try {
            if (Files.exists(FILE)) {
                List<Point> list = new Gson().fromJson(Files.readString(FILE, StandardCharsets.UTF_8), new TypeToken<List<Point>>() {}.getType());
                if (list != null) points.addAll(list);
            }
        } catch (Exception e) { AquaClient.LOG.warn("Не удалось загрузить точки", e); }
    }

    private void save() {
        try { Files.writeString(FILE, new Gson().toJson(points), StandardCharsets.UTF_8); }
        catch (Exception e) { AquaClient.LOG.warn("Не удалось сохранить точки", e); }
    }
}
