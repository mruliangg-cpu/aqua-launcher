package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.util.ColorUtil;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;

public class BlockOutline extends Module {
    private final ColorSetting color = add(new ColorSetting("Цвет линий", 0xFF3EE6FF));
    private final BoolSetting fill = add(new BoolSetting("Заливка", true));
    private final ColorSetting fillColor = add(new ColorSetting("Цвет заливки", 0x303EE6FF), fill::isOn);

    public BlockOutline() {
        super("Block Outline", "Своя обводка и заливка блока под прицелом", Category.RENDER);
    }

    /** @return true — рисовать ванильную обводку, false — заменить своей. */
    public boolean render(WorldRenderContext ctx, WorldRenderContext.BlockOutlineContext bo) {
        if (!isEnabled() || ctx.world() == null) return true;
        MatrixStack ms = ctx.matrixStack();
        VertexConsumerProvider consumers = ctx.consumers();
        if (ms == null || consumers == null) return true;
        BlockPos pos = bo.blockPos();
        VoxelShape shape = bo.blockState().getOutlineShape(ctx.world(), pos, ShapeContext.of(mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player));
        if (shape.isEmpty()) return true;
        double ox = pos.getX() - bo.cameraX(), oy = pos.getY() - bo.cameraY(), oz = pos.getZ() - bo.cameraZ();

        if (fill.isOn()) {
            int fc = fillColor.color();
            VertexConsumer fv = consumers.getBuffer(RenderLayer.getDebugFilledBox());
            for (Box b : shape.getBoundingBoxes()) {
                Box e = b.expand(0.002);
                WorldRenderer.renderFilledBox(ms, fv, e.minX + ox, e.minY + oy, e.minZ + oz, e.maxX + ox, e.maxY + oy, e.maxZ + oz,
                        ColorUtil.red(fc) / 255f, ColorUtil.green(fc) / 255f, ColorUtil.blue(fc) / 255f, ColorUtil.alpha(fc) / 255f);
            }
        }

        int c = color.color();
        float r = ColorUtil.red(c) / 255f, g = ColorUtil.green(c) / 255f, b = ColorUtil.blue(c) / 255f, a = ColorUtil.alpha(c) / 255f;
        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry entry = ms.peek();
        shape.forEachEdge((x1, y1, z1, x2, y2, z2) -> {
            float dx = (float) (x2 - x1), dy = (float) (y2 - y1), dz = (float) (z2 - z1);
            float len = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1e-6f) return;
            dx /= len; dy /= len; dz /= len;
            lines.vertex(entry, (float) (x1 + ox), (float) (y1 + oy), (float) (z1 + oz)).color(r, g, b, a).normal(entry, dx, dy, dz);
            lines.vertex(entry, (float) (x2 + ox), (float) (y2 + oy), (float) (z2 + oz)).color(r, g, b, a).normal(entry, dx, dy, dz);
        });
        return false;
    }
}
