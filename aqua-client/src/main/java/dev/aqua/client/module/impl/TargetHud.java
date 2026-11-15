package dev.aqua.client.module.impl;

import dev.aqua.client.gui.Render2D;
import dev.aqua.client.gui.Theme;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.HudModule;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.util.Anim;
import dev.aqua.client.util.ColorUtil;
import dev.aqua.client.util.CombatTracker;
import dev.aqua.client.util.Lang;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;

public class TargetHud extends HudModule {
    private final BoolSetting onLook = add(new BoolSetting("При наведении", true));
    private final Anim health = new Anim(1);
    private long lastNs = System.nanoTime();

    public TargetHud() {
        super("Target HUD", "Здоровье и ник цели, которую вы бьёте", Category.COMBAT, 0.55f, 0.55f);
    }

    @Override
    protected void draw(DrawContext ctx, boolean editor) {
        LivingEntity target = CombatTracker.lastTarget;
        if (target == null && onLook.isOn() && mc.targetedEntity instanceof LivingEntity l) target = l;
        if (target == null && !editor) { width = 0; height = 0; return; }
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNs) / 1e9f);
        lastNs = now;

        String name = target != null ? target.getName().getString() : "Enderman";
        float hp = target != null ? target.getHealth() : 30, max = target != null ? target.getMaxHealth() : 40;
        float pct = Math.max(0, Math.min(1, hp / Math.max(1, max)));
        health.to(pct);
        health.update(dt, 8);

        width = Math.max(120, 36 + mc.textRenderer.getWidth(name) + 8);
        height = 34;
        drawBackground(ctx, width, height);
        if (target instanceof AbstractClientPlayerEntity p) {
            dev.aqua.client.compat.V.drawHead(ctx, p, 4, 4, 26);
        } else {
            Render2D.roundRect(ctx, 4, 4, 26, 26, 3, 0xFF061A2C);
            Render2D.rect(ctx, 9, 14, 6, 2, 0xFF9FF6FF);
            Render2D.rect(ctx, 19, 14, 6, 2, 0xFF9FF6FF);
        }
        text(ctx, name, 34, 5);
        String hpText = String.format("%.1f ❤", hp);
        text(ctx, hpText, width - mc.textRenderer.getWidth(hpText) - 5, 5, 0xFFFF5577);
        int bx = 34, bw = width - 40;
        Render2D.roundRect(ctx, bx, 19, bw, 6, 3, 0xFF0E3350);
        int col = ColorUtil.lerp(0xFFE0466E, Theme.EYE, pct);
        Render2D.roundRect(ctx, bx, 19, Math.max(6, bw * health.get()), 6, 3, col);
        if (mc.player != null && target != null) {
            String d = String.format(Lang.t("%.1fм", "%.1fm"), Math.sqrt(mc.player.squaredDistanceTo(target)));
            Render2D.textScaled(ctx, d, bx, 27, 0.6f, 0xFFAAAAAA, false);
        }
    }
}
