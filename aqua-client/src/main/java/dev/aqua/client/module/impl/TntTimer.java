package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.util.ColorUtil;
import dev.aqua.client.util.WorldText;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.util.math.Vec3d;

/** Обратный отсчёт над подожжённым динамитом. */
public class TntTimer extends Module {
    public TntTimer() { super("TNT Timer", "Секунды до взрыва над динамитом", Category.RENDER); }

    public void render(WorldRenderContext ctx) {
        if (!isEnabled() || mc.world == null) return;
        boolean any = false;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof TntEntity tnt)) continue;
            float sec = Math.max(0, tnt.getFuse() / 20f);
            int col = ColorUtil.lerp(0xFFFF3B3B, 0xFF5CFF7A, Math.min(1, sec / 4f));
            Vec3d p = e.getLerpedPos(ctx.tickCounter().getTickDelta(true)).add(0, e.getHeight() + 0.5, 0);
            WorldText.label(ctx, p, String.format("%.2f", sec), col, 1.2f, false);
            any = true;
        }
        if (any) WorldText.flush(ctx);
    }
}
