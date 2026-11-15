package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.NumberSetting;

/** Снижает FPS, когда окно игры свёрнуто/не в фокусе — меньше нагрев и шум. */
public class UnfocusedFps extends Module {
    private final NumberSetting limit = add(new NumberSetting("FPS без фокуса", 15, 5, 60, 5));
    private Integer saved;

    public UnfocusedFps() { super("Unfocused FPS", "Меньше FPS, когда вы в другом окне", Category.PERFORMANCE); }

    @Override
    public void onTick() {
        boolean focused = mc.isWindowFocused();
        var opt = mc.options.getMaxFps();
        if (!focused && saved == null) {
            saved = opt.getValue();
            opt.setValue(limit.getInt());
        } else if (focused && saved != null) {
            opt.setValue(saved);
            saved = null;
        }
    }

    @Override
    protected void onDisable() {
        if (saved != null) {
            mc.options.getMaxFps().setValue(saved);
            saved = null;
        }
    }
}
