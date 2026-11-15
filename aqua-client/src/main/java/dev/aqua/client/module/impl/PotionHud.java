package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.HudModule;
import dev.aqua.client.setting.BoolSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.ArrayList;
import java.util.List;

public class PotionHud extends HudModule {
    private final BoolSetting icons = add(new BoolSetting("Иконки", true));
    private final BoolSetting blink = add(new BoolSetting("Мигать в конце", true));

    public PotionHud() {
        super("Potion Effects", "Активные эффекты зелий и их время", Category.HUD, 0.005f, 0.75f);
    }

    @Override
    protected void draw(DrawContext ctx, boolean editor) {
        List<StatusEffectInstance> effects = mc.player != null ? new ArrayList<>(mc.player.getStatusEffects()) : List.of();
        if (effects.isEmpty()) {
            if (!editor) { width = 0; height = 0; return; }
            String a = dev.aqua.client.util.Lang.t("Скорость II", "Speed II"), b = "1:30";
            width = 20 + Math.max(mc.textRenderer.getWidth(a), 30) + 8;
            height = 24;
            drawBackground(ctx, width, height);
            text(ctx, a, 22, 3);
            text(ctx, b, 22, 13, 0xFFAAAAAA);
            return;
        }
        int maxW = 0;
        for (StatusEffectInstance e : effects) maxW = Math.max(maxW, mc.textRenderer.getWidth(name(e)));
        int ix = icons.isOn() ? 22 : 4;
        width = ix + maxW + 6;
        height = effects.size() * 22 + 2;
        drawBackground(ctx, width, height);
        int y = 2;
        for (StatusEffectInstance e : effects) {
            boolean ending = !e.isInfinite() && e.getDuration() < 200;
            boolean hidden = blink.isOn() && ending && (System.currentTimeMillis() / 250) % 2 == 0;
            if (icons.isOn() && !hidden) {
                Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(e.getEffectType());
                ctx.drawSprite(2, y + 1, 0, 18, 18, sprite);
            }
            text(ctx, name(e), ix, y + 1);
            text(ctx, duration(e), ix, y + 11, ending ? 0xFFFF5555 : 0xFFAAAAAA);
            y += 22;
        }
    }

    private static String name(StatusEffectInstance e) {
        String n = e.getEffectType().value().getName().getString();
        int amp = e.getAmplifier();
        return amp > 0 ? n + " " + roman(amp + 1) : n;
    }

    private static String duration(StatusEffectInstance e) {
        if (e.isInfinite()) return "∞";
        int s = e.getDuration() / 20;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    private static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n < r.length ? r[n] : String.valueOf(n);
    }
}
