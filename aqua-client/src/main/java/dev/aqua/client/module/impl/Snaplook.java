package dev.aqua.client.module.impl;

import dev.aqua.client.AquaClient;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import net.minecraft.client.option.Perspective;

/** Пока зажата клавиша (Z) — камера спереди, отпустили — вернулась. */
public class Snaplook extends Module {
    private Perspective previous;

    public Snaplook() { super("Snaplook", "Посмотреть на себя спереди, пока зажата Z", Category.RENDER); }

    @Override
    public void onTick() {
        boolean down = AquaClient.SNAPLOOK != null && AquaClient.SNAPLOOK.isPressed() && mc.currentScreen == null;
        if (down && previous == null) {
            previous = mc.options.getPerspective();
            mc.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
        } else if (!down && previous != null) {
            mc.options.setPerspective(previous);
            previous = null;
        }
    }

    @Override
    protected void onDisable() {
        if (previous != null && mc.options != null) { mc.options.setPerspective(previous); previous = null; }
    }
}
