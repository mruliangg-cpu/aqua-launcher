package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.NumberSetting;

/** Небольшие модули-переключатели. */
public final class SimpleToggles {
    private SimpleToggles() {}

    public static class NoPumpkin extends Module {
        public static volatile boolean ACTIVE;
        public NoPumpkin() { super("No Pumpkin", "Убирает затемнение от тыквы на голове", Category.RENDER); }
        @Override protected void onEnable() { ACTIVE = true; }
        @Override protected void onDisable() { ACTIVE = false; }
    }

    public static class HideScoreboard extends Module {
        public static volatile boolean ACTIVE;
        public HideScoreboard() { super("Hide Scoreboard", "Прячет табло справа", Category.RENDER); }
        @Override protected void onEnable() { ACTIVE = true; }
        @Override protected void onDisable() { ACTIVE = false; }
    }

    /** Отключает тряску камеры при уроне через ванильную опцию. */
    public static class NoHurtCam extends Module {
        private final NumberSetting strength = add(new NumberSetting("Сила тряски", 0, 0, 100, 5, "%"));
        private Double saved;

        public NoHurtCam() { super("No Hurt Cam", "Без тряски камеры при получении урона", Category.RENDER); }

        @Override
        public void onTick() {
            double want = strength.get() / 100.0;
            var opt = mc.options.getDamageTiltStrength();
            if (saved == null) saved = opt.getValue() == want ? 1.0 : opt.getValue();
            if (opt.getValue() != want) opt.setValue(want);
        }

        @Override
        protected void onDisable() {
            if (saved == null || mc.options == null) return;
            mc.options.getDamageTiltStrength().setValue(saved);
            saved = null;
        }
    }

    public static class HideBossbar extends Module {
        public static volatile boolean ACTIVE;
        public HideBossbar() { super("Hide Bossbar", "Прячет полоску здоровья босса", Category.RENDER); }
        @Override protected void onEnable() { ACTIVE = true; }
        @Override protected void onDisable() { ACTIVE = false; }
    }

    public static class Hitboxes extends Module {
        public Hitboxes() { super("Hitboxes", "Показывает хитбоксы сущностей (как F3+B)", Category.RENDER); }
        //#if MC >= 1.21.9
        // 1.21.9+: хитбоксы — запись отладочного экрана
        private static final net.minecraft.util.Identifier ID = net.minecraft.util.Identifier.ofVanilla("entity_hitboxes");
        @Override public void onTick() { if (mc.debugHudEntryList != null && !mc.debugHudEntryList.isEntryVisible(ID)) mc.debugHudEntryList.toggleVisibility(ID); }
        @Override protected void onDisable() { if (mc.debugHudEntryList != null && mc.debugHudEntryList.isEntryVisible(ID)) mc.debugHudEntryList.toggleVisibility(ID); }
        //#else
        @Override public void onTick() { if (!mc.getEntityRenderDispatcher().shouldRenderHitboxes()) mc.getEntityRenderDispatcher().setRenderHitboxes(true); }
        @Override protected void onDisable() { if (mc.getEntityRenderDispatcher() != null) mc.getEntityRenderDispatcher().setRenderHitboxes(false); }
        //#endif
    }

    public static class ChunkBorders extends Module {
        private boolean shown;
        public ChunkBorders() { super("Chunk Borders", "Границы чанков (как F3+G)", Category.RENDER); }
        //#if MC >= 1.21.9
        private static final net.minecraft.util.Identifier ID = net.minecraft.util.Identifier.ofVanilla("chunk_borders");
        @Override public void onTick() { if (mc.debugHudEntryList != null && !mc.debugHudEntryList.isEntryVisible(ID)) { mc.debugHudEntryList.toggleVisibility(ID); shown = true; } }
        @Override protected void onDisable() { if (shown && mc.debugHudEntryList != null && mc.debugHudEntryList.isEntryVisible(ID)) mc.debugHudEntryList.toggleVisibility(ID); shown = false; }
        //#else
        @Override public void onTick() { if (!shown && mc.debugRenderer != null) { shown = mc.debugRenderer.toggleShowChunkBorder(); if (!shown) shown = mc.debugRenderer.toggleShowChunkBorder(); } }
        @Override protected void onDisable() { if (shown && mc.debugRenderer != null) { if (mc.debugRenderer.toggleShowChunkBorder()) mc.debugRenderer.toggleShowChunkBorder(); } shown = false; }
        //#endif
    }

    /** Время перед каждым сообщением чата (см. ChatHudMixin). */
    public static class ChatTimestamps extends Module {
        public static volatile boolean ACTIVE;
        private static ChatTimestamps instance;
        private final dev.aqua.client.setting.BoolSetting seconds = add(new dev.aqua.client.setting.BoolSetting("Секунды", false));
        public ChatTimestamps() { super("Chat Timestamps", "Время перед сообщениями в чате", Category.MISC); instance = this; }
        @Override protected void onEnable() { ACTIVE = true; }
        @Override protected void onDisable() { ACTIVE = false; }
        public static net.minecraft.text.Text apply(net.minecraft.text.Text message) {
            if (!ACTIVE || message == null) return message;
            String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern(instance.seconds.isOn() ? "HH:mm:ss" : "HH:mm"));
            return net.minecraft.text.Text.literal("[" + time + "] ").formatted(net.minecraft.util.Formatting.DARK_PURPLE).append(message);
        }
    }
}
