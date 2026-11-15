package dev.aqua.client.module.impl;

import dev.aqua.client.gui.Render2D;
import dev.aqua.client.gui.Theme;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.HudModule;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.Heightmap;

/** Мини-карта как в Lunar: рельеф сверху, игроки и мобы точками. */
public class Minimap extends HudModule {
    private static final Identifier TEX_ID = dev.aqua.client.compat.V.id("aquaclient", "minimap");
    private static final int RES = 96;
    private final NumberSetting size = add(new NumberSetting("Размер карты", 100, 60, 180, 5));
    private final NumberSetting zoom = add(new NumberSetting("Масштаб (блоков на пиксель)", 1, 1, 4, 1));
    private final BoolSetting players = add(new BoolSetting("Игроки", true));
    private final BoolSetting mobs = add(new BoolSetting("Враждебные мобы", true));
    private final BoolSetting coords = add(new BoolSetting("Координаты под картой", true));
    private NativeImageBackedTexture texture;
    private int timer;

    public Minimap() {
        super("Minimap", "Мини-карта местности с игроками и мобами", Category.HUD, 0.86f, 0.12f);
        background.set(false);
    }

    @Override
    public void onTick() {
        if (mc.world == null || mc.player == null) return;
        if (--timer > 0) return;
        timer = 4;
        if (texture == null) {
            texture = dev.aqua.client.compat.V.texture(RES, RES);
            mc.getTextureManager().registerTexture(TEX_ID, texture);
        }
        NativeImage img = texture.getImage();
        if (img == null) return;
        int z = zoom.getInt();
        int cx = mc.player.getBlockX(), cz = mc.player.getBlockZ();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int[] prevRow = new int[RES];
        for (int py = 0; py < RES; py++) {
            for (int px = 0; px < RES; px++) {
                int wx = cx + (px - RES / 2) * z, wz = cz + (py - RES / 2) * z;
                int top = mc.world.getTopY(Heightmap.Type.WORLD_SURFACE, wx, wz) - 1;
                pos.set(wx, top, wz);
                BlockState state = mc.world.getBlockState(pos);
                int rgb = state.getMapColor(mc.world, pos).color;
                if (rgb == 0) rgb = 0x0E0818;
                // тень рельефа: сравниваем с высотой «севернее»
                float shade = 1f;
                if (py > 0) shade = top > prevRow[px] ? 1.12f : top < prevRow[px] ? 0.82f : 1f;
                prevRow[px] = top;
                int r = Math.min(255, (int) (((rgb >> 16) & 0xFF) * shade));
                int g = Math.min(255, (int) (((rgb >> 8) & 0xFF) * shade));
                int b = Math.min(255, (int) ((rgb & 0xFF) * shade));
                dev.aqua.client.compat.V.setArgb(img, px, py, 0xFF000000 | (r << 16) | (g << 8) | b);
            }
        }
        texture.upload();
    }

    @Override
    protected void draw(DrawContext ctx, boolean editor) {
        int s = size.getInt();
        width = s + 4;
        height = s + 4 + (coords.isOn() ? 12 : 0);
        Render2D.roundRect(ctx, 0, 0, s + 4, s + 4, 4, 0xFF1B4A6B);
        if (texture != null && mc.player != null) {
            dev.aqua.client.compat.V.texture(ctx, TEX_ID, 2, 2, 0f, 0f, s, s, s, s);
        } else {
            Render2D.rect(ctx, 2, 2, s, s, 0xFF120A1E);
            Render2D.centered(ctx, dev.aqua.client.util.Lang.t("Карта", "Map"), 2 + s / 2f, 2 + s / 2f - 4, Theme.SUB);
        }
        float center = 2 + s / 2f;
        float scale = s / (float) (RES * zoom.getInt());
        if (mc.world != null && mc.player != null) {
            for (Entity e : mc.world.getEntities()) {
                if (e == mc.player) continue;
                boolean isPlayer = e instanceof PlayerEntity;
                if (isPlayer ? !players.isOn() : !(mobs.isOn() && e instanceof HostileEntity)) continue;
                float dx = (float) (e.getX() - mc.player.getX()) * scale, dz = (float) (e.getZ() - mc.player.getZ()) * scale;
                if (Math.abs(dx) > s / 2f - 2 || Math.abs(dz) > s / 2f - 2) continue;
                Render2D.rect(ctx, center + dx - 1.5f, center + dz - 1.5f, 3, 3, isPlayer ? 0xFFFFFFFF : 0xFFFF4D6D);
            }
            // стрелка игрока
            dev.aqua.client.compat.V.push(ctx);
            dev.aqua.client.compat.V.translate(ctx, center, center);
            dev.aqua.client.compat.V.rotateZ(ctx, mc.player.getYaw() + 180);
            ctx.fill(-1, -4, 1, 3, 0xFF000000);
            ctx.fill(-3, 0, 3, 2, 0xFF000000);
            ctx.fill(0, -4, 1, 2, Theme.EYE);
            ctx.fill(-2, 0, 2, 1, Theme.EYE);
            dev.aqua.client.compat.V.pop(ctx);
        }
        ctx.drawText(mc.textRenderer, "N", (int) center - 2, 3, 0xFFFF5577, true);
        if (coords.isOn()) {
            String c = mc.player != null ? mc.player.getBlockX() + ", " + mc.player.getBlockY() + ", " + mc.player.getBlockZ() : "0, 64, 0";
            Render2D.textScaled(ctx, c, center - Render2D.width(c) * 0.4f, s + 7, 0.8f, textColor.color(), shadow.isOn());
        }
    }
}
