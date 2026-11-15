package dev.aqua.client;

import dev.aqua.client.config.ConfigManager;
import dev.aqua.client.gui.ClickGuiScreen;
import dev.aqua.client.module.ModuleManager;
import dev.aqua.client.module.impl.AutoGG;
import dev.aqua.client.module.impl.BlockOutline;
import dev.aqua.client.module.impl.HitParticles;
import dev.aqua.client.module.impl.TimeChanger;
import dev.aqua.client.module.impl.WeatherChanger;
import dev.aqua.client.module.impl.TntTimer;
import dev.aqua.client.module.impl.Waypoints;
import dev.aqua.client.util.CombatTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
//#if MC >= 1.21.10
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
//#elseif MC < 1.21.9
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
//#endif
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AquaClient implements ClientModInitializer {
    public static final String MOD_ID = "aquaclient";
    public static final Logger LOG = LoggerFactory.getLogger("Aqua Client");

    public static KeyBinding MENU, ZOOM, FREELOOK, WAYPOINT, SNAPLOOK;

    @Override
    public void onInitializeClient() {
        MENU = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.aquaclient.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.aquaclient"));
        ZOOM = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.aquaclient.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.aquaclient"));
        FREELOOK = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.aquaclient.freelook", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "category.aquaclient"));
        WAYPOINT = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.aquaclient.waypoint", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "category.aquaclient"));
        SNAPLOOK = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.aquaclient.snaplook", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Z, "category.aquaclient"));

        ModuleManager.init();
        ConfigManager.load();
        // гардероб и скин, выбранные в Aqua Launcher
        dev.aqua.client.module.impl.Cosmetics cos = ModuleManager.get(dev.aqua.client.module.impl.Cosmetics.class);
        if (cos != null) cos.applyLauncherLoadout();

        //#if MC < 1.21.9
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof PlayerEntityRenderer per) helper.register(new dev.aqua.client.cosmetic.CosmeticFeatureRenderer(per));
        });
        //#endif

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (MENU.wasPressed()) client.setScreen(new ClickGuiScreen());
            ModuleManager.tick(client);
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> HudRenderer.render(ctx));
        // (в 1.20.x второй аргумент — float tickDelta; лямбда подходит для обоих вариантов)

        //#if MC < 1.21.9 || MC >= 1.21.10
        // (в Fabric API для 1.21.9 событий отрисовки мира нет)
        //#if MC >= 1.21.10
        WorldRenderEvents.START_MAIN.register(ctx -> {
        //#else
        WorldRenderEvents.START.register(ctx -> {
        //#endif
            TimeChanger t = ModuleManager.get(TimeChanger.class);
            if (t != null) t.onWorldRender();
            WeatherChanger w = ModuleManager.get(WeatherChanger.class);
            if (w != null) w.onWorldRender();
        });

        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            TntTimer tnt = ModuleManager.get(TntTimer.class);
            if (tnt != null) tnt.render(dev.aqua.client.compat.WorldCtx.of(ctx));
            Waypoints wp = ModuleManager.get(Waypoints.class);
            if (wp != null) wp.render(dev.aqua.client.compat.WorldCtx.of(ctx));
        });
        //#endif

        //#if MC < 1.21.9
        WorldRenderEvents.BLOCK_OUTLINE.register((ctx, outline) -> {
            BlockOutline b = ModuleManager.get(BlockOutline.class);
            return b == null || b.render(ctx, outline);
        });
        //#endif

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient) {
                CombatTracker.onAttack(entity);
                HitParticles hp = ModuleManager.get(HitParticles.class);
                if (hp != null) hp.onAttack(entity);
            }
            return ActionResult.PASS;
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            AutoGG gg = ModuleManager.get(AutoGG.class);
            if (gg != null) gg.onGameMessage(message);
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            ConfigManager.save();
            // Возвращаем ванильные настройки (FPS, тряска камеры и т.д.), чтобы не записать их в options.txt
            ModuleManager.all().forEach(m -> { if (m.isEnabled()) m.setEnabled(false); });
        });

        LOG.info("Aqua Client загружен: {} модулей. Меню — правый Shift.", ModuleManager.all().size());
    }
}
