package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.KeySetting;
import dev.aqua.client.setting.StringSetting;

/** Горячие клавиши для сообщений и команд (как Auto Text Hotkey в Lunar). */
public class AutoText extends Module {
    private final StringSetting[] texts = new StringSetting[4];
    private final KeySetting[] keys = new KeySetting[4];
    private static final String[] DEFAULTS = {"/l", "gg", "/play bedwars_eight_one", ""};

    public AutoText() {
        super("Auto Text", "Сообщение или команда по нажатию клавиши", Category.MISC);
        for (int i = 0; i < 4; i++) {
            texts[i] = add(new StringSetting("Текст " + (i + 1), DEFAULTS[i], 100));
            keys[i] = add(new KeySetting("Клавиша " + (i + 1), -1));
        }
    }

    @Override
    public void onTick() {
        for (int i = 0; i < 4; i++) {
            if (!keys[i].pressed()) continue;
            String t = texts[i].get().trim();
            if (t.isEmpty() || mc.getNetworkHandler() == null) continue;
            if (t.startsWith("/")) mc.getNetworkHandler().sendChatCommand(t.substring(1));
            else mc.getNetworkHandler().sendChatMessage(t);
        }
    }
}
