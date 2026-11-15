package dev.aqua.client.module.impl;

import dev.aqua.client.gui.Render2D;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.NumberSetting;
import dev.aqua.client.util.Lang;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;

/** Дополнительные модули, которых нет в Lunar. */
public final class MoreModules {
    private MoreModules() {}

    public static class AutoRespawn extends Module {
        private final NumberSetting delay = add(new NumberSetting("Задержка (с)", 0.5, 0, 5, 0.5, "s"));
        private long deathAt;
        public AutoRespawn() { super("Auto Respawn", "Автоматически возрождает после смерти", Category.MISC); }
        @Override public void onTick() {
            if (mc.currentScreen instanceof DeathScreen && mc.player != null) {
                if (deathAt == 0) deathAt = System.currentTimeMillis();
                if (System.currentTimeMillis() - deathAt >= delay.get() * 1000) {
                    mc.player.requestRespawn();
                    mc.setScreen(null);
                    deathAt = 0;
                }
            } else deathAt = 0;
        }
    }

    /** Красная пульсирующая рамка, когда мало здоровья. */
    public static class LowHealth extends Module {
        private static LowHealth instance;
        private final NumberSetting threshold = add(new NumberSetting("Порог здоровья", 6, 1, 15, 1, "❤"));
        private final BoolSetting sound = add(new BoolSetting("Звук", false));
        private long lastBeep;
        public LowHealth() { super("Low Health Warning", "Красная рамка экрана при низком здоровье", Category.COMBAT); instance = this; }

        public static void render(DrawContext ctx) {
            if (instance == null || !instance.isEnabled() || instance.mc.player == null || instance.mc.player.isDead()) return;
            float hp = instance.mc.player.getHealth() / 2f;
            if (hp > instance.threshold.get()) return;
            float pulse = 0.55f + 0.45f * (float) Math.sin(System.currentTimeMillis() / 160.0);
            float strength = (1 - hp / instance.threshold.get().floatValue()) * 0.6f + 0.4f;
            int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
            int steps = 24;
            for (int i = 0; i < steps; i++) {
                int a = (int) (110 * pulse * strength * (1 - i / (float) steps));
                int c = (a << 24) | 0xC0102A;
                ctx.fill(i, 0, i + 1, h, c);
                ctx.fill(w - i - 1, 0, w - i, h, c);
                ctx.fill(0, i, w, i + 1, c);
                ctx.fill(0, h - i - 1, w, h - i, c);
            }
            if (instance.sound.isOn() && System.currentTimeMillis() - instance.lastBeep > 1500) {
                instance.lastBeep = System.currentTimeMillis();
                instance.mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.6f, 0.6f);
            }
        }
    }

    /** Предупреждение, когда броня или инструмент вот-вот сломается. */
    public static class DurabilityAlert extends Module {
        private static DurabilityAlert instance;
        private final NumberSetting threshold = add(new NumberSetting("Порог прочности", 10, 3, 40, 1, "%"));
        public DurabilityAlert() { super("Durability Alert", "Предупреждает, что броня или инструмент ломается", Category.COMBAT); instance = this; }

        public static void render(DrawContext ctx) {
            if (instance == null || !instance.isEnabled() || instance.mc.player == null) return;
            String worst = null;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack st = instance.mc.player.getEquippedStack(slot);
                if (st.isEmpty() || !st.isDamageable()) continue;
                float left = (st.getMaxDamage() - st.getDamage()) * 100f / st.getMaxDamage();
                if (left <= instance.threshold.get()) { worst = st.getName().getString() + " " + Math.round(left) + "%"; break; }
            }
            if (worst == null || (System.currentTimeMillis() / 400) % 2 == 0) return;
            String s = "⚠ " + Lang.t("Ломается: ", "Breaking: ") + worst;
            int w = ctx.getScaledWindowWidth();
            Render2D.roundRect(ctx, w / 2f - Render2D.width(s) / 2f - 6, 34, Render2D.width(s) + 12, 14, 4, 0xC0300818);
            ctx.drawText(instance.mc.textRenderer, s, w / 2 - Render2D.width(s) / 2, 37, 0xFFFF5566, true);
        }
    }

    /** Свой ник над головой в виде от третьего лица. */
    public static class OwnNametag extends Module {
        public static volatile boolean ACTIVE;
        public OwnNametag() { super("Own Nametag", "Показывает ваш ник в F5", Category.RENDER); }
        @Override protected void onEnable() { ACTIVE = true; }
        @Override protected void onDisable() { ACTIVE = false; }
    }
}
