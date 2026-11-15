package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.ModeSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.text.Text;

import java.util.List;

/** Пишет «gg» в чат в конце игры на мини-игровых серверах. */
public class AutoGG extends Module {
    private static final List<String> TRIGGERS = List.of(
            "1st killer", "1st place", "winner:", "winners:", "you won", "victory!", "game over",
            "#1 victory", "top survivors", "победитель", "победили", "вы победили", "игра окончена");
    private final ModeSetting message = add(new ModeSetting("Сообщение", "gg", "gg", "GG", "gg wp", "Good game", "gg ez"));
    private final NumberSetting delay = add(new NumberSetting("Задержка", 1.0, 0, 5, 0.5, "с"));
    private long sendAt = -1, lastSent;

    public AutoGG() { super("Auto GG", "Автоматически пишет gg после победы/конца игры", Category.MISC); }

    public void onGameMessage(Text text) {
        if (!isEnabled() || mc.player == null) return;
        String s = text.getString().toLowerCase();
        for (String t : TRIGGERS) {
            if (s.contains(t) && System.currentTimeMillis() - lastSent > 10000) {
                sendAt = System.currentTimeMillis() + (long) (delay.get() * 1000);
                lastSent = System.currentTimeMillis();
                return;
            }
        }
    }

    @Override
    public void onTick() {
        if (sendAt > 0 && System.currentTimeMillis() >= sendAt) {
            sendAt = -1;
            if (mc.player != null && mc.getNetworkHandler() != null) mc.getNetworkHandler().sendChatMessage(message.get());
        }
    }
}
