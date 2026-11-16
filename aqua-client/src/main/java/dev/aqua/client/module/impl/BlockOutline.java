package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.util.ColorUtil;
//#if MC >= 1.21.10
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
//#elseif MC < 1.21.9
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
//#endif
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

public class BlockOutline extends Module {
    private final ColorSetting color = add(new ColorSetting("Цвет линий", 0xFF3EE6FF));
    private final BoolSetting fill = add(new BoolSetting("Заливка", true));
    private final ColorSetting fillColor = add(new ColorSetting("Цвет заливки", 0x303EE6FF), fill::isOn);

    public BlockOutline() {
        super("Block Outline", "Своя обводка и заливка блока под прицелом", Category.RENDER);
    }

    //#if MC >= 1.21.10
    /** 1.21.10+: новое событие отрисовки мира (пакет rendering.v1.world). */
    public boolean render(WorldRenderContext ctx, WorldRenderContext.BlockOutlineContext bo) {
        if (!isEnabled()) return true;
        World world = MinecraftClient.getInstance().world;
        if (world == null) return true;
        return aqua$draw(ctx.matrices(), ctx.consumers(), world, bo.blockPos(), bo.blockState(), bo.cameraX(), bo.cameraY(), bo.cameraZ());
    }
    //#elseif MC < 1.21.9
    /** @return true — рисовать ванильную обводку, false — заменить своей. */
    public boolean render(WorldRenderContext ctx, WorldRenderContext.BlockOutlineContext bo) {
        if (!isEnabled() || ctx.world() == null) return true;
        return aqua$draw(ctx.matrixStack(), ctx.consumers(), ctx.world(), bo.blockPos(), bo.blockState(), bo.cameraX(), bo.cameraY(), bo.cameraZ());
    }
    //#endif

    //#if MC < 1.21.9 || MC >= 1.21.10
    /** Общая отрисовка обводки/заливки, одинаковая для старого и нового API. */
    private boolean aqua$draw(MatrixStack ms, VertexConsumerProvider consumers, World world, BlockPos pos, BlockState state,
                              double camX, double camY, double camZ) {
        if (ms == null || consumers == null) return true;
        VoxelShape shape = state.getOutlineShape(world, pos, ShapeContext.of(mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player));
        if (shape.isEmpty()) return true;
        double ox = pos.getX() - camX, oy = pos.getY() - camY, oz = pos.getZ() - camZ;

        if (fill.isOn()) {
            int fc = fillColor.color();
            VertexConsumer fv = consumers.getBuffer(RenderLayer.getDebugFilledBox());
            for (Box b : shape.getBoundingBoxes()) {
                Box e = b.expand(0.002);
                dev.aqua.client.compat.V.filledBox(ms, fv, e.minX + ox, e.minY + oy, e.minZ + oz, e.maxX + ox, e.maxY + oy, e.maxZ + oz,
                        ColorUtil.red(fc) / 255f, ColorUtil.green(fc) / 255f, ColorUtil.blue(fc) / 255f, ColorUtil.alpha(fc) / 255f);
            }
        }

        int c = color.color();
        float r = ColorUtil.red(c) / 255f, g = ColorUtil.green(c) / 255f, b = ColorUtil.blue(c) / 255f, a = ColorUtil.alpha(c) / 255f;
        VertexConsumer lines = consumers.getBuffer(dev.aqua.client.compat.V.lines());
        MatrixStack.Entry entry = ms.peek();
        shape.forEachEdge((x1, y1, z1, x2, y2, z2) -> {
            float dx = (float) (x2 - x1), dy = (float) (y2 - y1), dz = (float) (z2 - z1);
            float len = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1e-6f) return;
            dx /= len; dy /= len; dz /= len;
            dev.aqua.client.compat.V.end(dev.aqua.client.compat.V.normal(lines.vertex(entry.getPositionMatrix(), (float) (x1 + ox), (float) (y1 + oy), (float) (z1 + oz)).color(r, g, b, a), entry, dx, dy, dz));
            dev.aqua.client.compat.V.end(dev.aqua.client.compat.V.normal(lines.vertex(entry.getPositionMatrix(), (float) (x2 + ox), (float) (y2 + oy), (float) (z2 + oz)).color(r, g, b, a), entry, dx, dy, dz));
        });
        return false;
    }
    //#endif
}
