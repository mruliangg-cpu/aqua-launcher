package dev.aqua.client.module.impl;

import dev.aqua.client.compat.V;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.util.Lang;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * AppleSkin (как мод с Modrinth): показывает голод и насыщение еды.
 * — в подсказке предмета: сколько голода и насыщения восстановит еда;
 * — на HUD: полоска насыщения под голодом и предпросмотр того, что восстановит еда в руке.
 */
public class AppleSkin extends Module {
    private static AppleSkin instance;
    private final BoolSetting tooltips = add(new BoolSetting("Подсказки еды", true));
    private final BoolSetting satBar = add(new BoolSetting("Полоса насыщения", true));
    private final BoolSetting restorePreview = add(new BoolSetting("Предпросмотр еды", true));

    public AppleSkin() {
        super("AppleSkin", "Голод и насыщение еды в подсказках и на HUD", Category.RENDER);
        instance = this;
    }

    public static boolean on() { return instance != null && instance.isEnabled(); }

    /** Добавляет строки «Голод» и «Насыщение» в подсказку еды. */
    public static void appendTooltip(ItemStack stack, List<Text> lines) {
        if (!on() || !instance.tooltips.isOn() || stack == null) return;
        int nutrition = V.foodNutrition(stack);
        if (nutrition < 0) return;
        float sat = nutrition * V.foodSatModifier(stack) * 2f;
        lines.add(Text.literal(Lang.t("Голод: ", "Hunger: ") + "§6" + nutrition + " §7(" + fmt1(nutrition / 2f) + "🍗)").formatted(Formatting.GRAY));
        lines.add(Text.literal(Lang.t("Насыщение: ", "Saturation: ") + "§e" + fmt1(sat)).formatted(Formatting.GRAY));
    }

    /** Полоса насыщения и предпросмотр восстановления на полоске голода. */
    public static void renderOverlay(DrawContext ctx) {
        if (!on()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity p = mc.player;
        if (p == null || mc.options.hudHidden || p.isCreative() || p.isSpectator()) return;
        if (p.getVehicle() instanceof LivingEntity) return;

        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        int rEdge = sw / 2 + 91;      // правый край полоски голода (ваниль)
        int top = sh - 39;            // верх иконок голода
        int full = 81;                // ширина полоски (20 очков)
        int food = p.getHungerManager().getFoodLevel();
        float saturation = p.getHungerManager().getSaturationLevel();

        // полоса насыщения — тонкая линия под голодом
        if (instance.satBar.isOn() && saturation > 0) {
            int len = Math.round(Math.min(saturation, 20f) / 20f * full);
            int y = top + 10;
            ctx.fill(rEdge - len, y, rEdge, y + 1, 0xD0FFE23E);
        }

        // предпросмотр восстановления от еды в руке — мерцающая полоска над голодом
        if (instance.restorePreview.isOn() && food < 20) {
            ItemStack held = p.getMainHandStack();
            int nutri = V.foodNutrition(held);
            if (nutri < 0) { held = p.getOffHandStack(); nutri = V.foodNutrition(held); }
            if (nutri > 0) {
                int restored = Math.min(nutri, 20 - food);
                int len = Math.round(restored / 20f * full);
                float a = 0.45f + 0.35f * (float) Math.sin((System.currentTimeMillis() % 900) / 900.0 * Math.PI * 2);
                int col = ((int) (a * 255) << 24) | 0x5FF2C8;
                int y = top - 3;
                ctx.fill(rEdge - len, y, rEdge, y + 2, col);
            }
        }
    }

    private static String fmt1(float v) {
        return String.format(java.util.Locale.US, "%.1f", v);
    }
}
