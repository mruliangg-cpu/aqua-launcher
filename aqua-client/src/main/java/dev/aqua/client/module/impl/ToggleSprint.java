package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.HudModule;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.util.Keys;
import dev.aqua.client.util.Lang;
import net.minecraft.client.gui.DrawContext;

/** Toggle Sprint / Toggle Sneak как в Lunar: нажал Ctrl один раз — бежишь всегда. */
public class ToggleSprint extends HudModule {
    private final BoolSetting sprint = add(new BoolSetting("Переключаемый бег", true));
    private final BoolSetting sneak = add(new BoolSetting("Переключаемое приседание", false));
    private final BoolSetting hudText = add(new BoolSetting("Текст на экране", true));
    private boolean sprintOn = true, sneakOn, sprintWas, sneakWas;

    public ToggleSprint() {
        super("Toggle Sprint", "Бег и приседание по одному нажатию", Category.MOVEMENT, 0.005f, 0.97f);
        background.set(false);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        boolean sprintDown = Keys.isDown(mc.options.sprintKey);
        if (sprintDown && !sprintWas && mc.currentScreen == null) sprintOn = !sprintOn;
        sprintWas = sprintDown;
        boolean sneakDown = Keys.isDown(mc.options.sneakKey);
        if (sneakDown && !sneakWas && mc.currentScreen == null) sneakOn = !sneakOn;
        sneakWas = sneakDown;

        if (sprint.isOn() && sprintOn && mc.currentScreen == null) mc.options.sprintKey.setPressed(true);
        if (sneak.isOn()) mc.options.sneakKey.setPressed((sneakOn && mc.currentScreen == null) || sneakDown);
    }

    @Override
    protected void onDisable() {
        if (mc.options == null) return;
        mc.options.sprintKey.setPressed(Keys.isDown(mc.options.sprintKey));
        mc.options.sneakKey.setPressed(Keys.isDown(mc.options.sneakKey));
    }

    private String status() {
        if (mc.player == null) return null;
        if (sneak.isOn() && sneakOn) return Lang.t("[Присед (переключён)]", "[Sneaking (Toggled)]");
        if (mc.player.isSneaking()) return Lang.t("[Присед (удерживается)]", "[Sneaking (Key Held)]");
        if (sprint.isOn() && sprintOn) return mc.player.isSprinting() ? Lang.t("[Бег (переключён)]", "[Sprinting (Toggled)]") : Lang.t("[Бег (готов)]", "[Sprint (Ready)]");
        if (mc.player.isSprinting()) return Lang.t("[Бег (удерживается)]", "[Sprinting (Key Held)]");
        return null;
    }

    @Override
    protected void draw(DrawContext ctx, boolean editor) {
        String s = hudText.isOn() ? status() : null;
        if (s == null && editor) s = Lang.t("[Бег (переключён)]", "[Sprinting (Toggled)]");
        if (s == null) { width = 0; height = 0; return; }
        width = mc.textRenderer.getWidth(s) + (background.isOn() ? 8 : 0);
        height = background.isOn() ? 14 : 9;
        drawBackground(ctx, width, height);
        text(ctx, s, background.isOn() ? 4 : 0, background.isOn() ? 3 : 0);
    }
}
