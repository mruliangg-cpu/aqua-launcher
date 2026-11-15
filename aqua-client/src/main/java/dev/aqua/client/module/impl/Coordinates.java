package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.HudModule;
import dev.aqua.client.setting.BoolSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public class Coordinates extends HudModule {
    private final BoolSetting facing = add(new BoolSetting("Направление", true));
    private final BoolSetting biome = add(new BoolSetting("Биом", true));
    private final BoolSetting nether = add(new BoolSetting("Координаты Незера", false));

    public Coordinates() {
        super("Coordinates", "Координаты XYZ, направление и биом", Category.HUD, 0.005f, 0.36f);
    }

    @Override
    protected void draw(DrawContext ctx, boolean editor) {
        List<String[]> lines = new ArrayList<>();
        if (mc.player != null) {
            BlockPos p = mc.player.getBlockPos();
            lines.add(new String[]{"X", String.valueOf(p.getX())});
            lines.add(new String[]{"Y", String.valueOf(p.getY())});
            lines.add(new String[]{"Z", String.valueOf(p.getZ())});
            if (facing.isOn()) {
                Direction d = mc.player.getHorizontalFacing();
                String axis = switch (d) {
                    case NORTH -> "-Z"; case SOUTH -> "+Z"; case EAST -> "+X"; case WEST -> "-X"; default -> "";
                };
                lines.add(new String[]{"F", pretty(d.asString()) + " (" + axis + ")"});
            }
            if (biome.isOn() && mc.world != null) {
                String b = mc.world.getBiome(p).getKey().map(k -> pretty(k.getValue().getPath())).orElse("?");
                lines.add(new String[]{"B", b});
            }
            if (nether.isOn()) {
                boolean inNether = mc.world != null && mc.world.getRegistryKey().getValue().getPath().equals("the_nether");
                double f = inNether ? 8 : 0.125;
                lines.add(new String[]{inNether ? "OW" : "N", (int) (p.getX() * f) + " / " + (int) (p.getZ() * f)});
            }
        } else if (editor) {
            lines.add(new String[]{"X", "128"});
            lines.add(new String[]{"Y", "64"});
            lines.add(new String[]{"Z", "-256"});
        }
        int lw = 0;
        for (String[] l : lines) lw = Math.max(lw, mc.textRenderer.getWidth(l[0] + ": " + l[1]));
        width = lw + 10;
        height = lines.size() * 10 + 6;
        drawBackground(ctx, width, height);
        int y = 4;
        for (String[] l : lines) {
            int x = text(ctx, l[0] + ": ", 5, y, accent());
            text(ctx, l[1], x - (shadow.isOn() ? 1 : 0), y);
            y += 10;
        }
    }

    static String pretty(String id) {
        StringBuilder sb = new StringBuilder();
        for (String part : id.split("_")) {
            if (part.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
