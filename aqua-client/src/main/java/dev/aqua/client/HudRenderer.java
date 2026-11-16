package dev.aqua.client;

import dev.aqua.client.gui.HudEditorScreen;
import dev.aqua.client.module.HudModule;
import dev.aqua.client.module.ModuleManager;
import dev.aqua.client.module.impl.Crosshair;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class HudRenderer {
    private HudRenderer() {}

    public static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden) return;
        Crosshair.renderHud(ctx);
        dev.aqua.client.module.impl.MoreModules.LowHealth.render(ctx);
        dev.aqua.client.module.impl.MoreModules.DurabilityAlert.render(ctx);
        dev.aqua.client.module.impl.ExtraModules.renderAlerts(ctx);
        dev.aqua.client.module.impl.PvpModules.render(ctx);
        dev.aqua.client.module.impl.AppleSkin.renderOverlay(ctx);
        if (mc.currentScreen instanceof HudEditorScreen) return;
        for (HudModule h : ModuleManager.huds()) {
            if (!h.isEnabled()) continue;
            try { h.render(ctx, false); } catch (Exception e) { AquaClient.LOG.debug("HUD {} failed", h.getName(), e); }
        }
    }
}
