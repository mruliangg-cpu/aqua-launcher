package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.TextHudModule;
import dev.aqua.client.setting.ButtonSetting;
import dev.aqua.client.setting.KeySetting;

/** Секундомер для спидранов и паркура. */
public class Stopwatch extends TextHudModule {
    private static long start, accumulated;
    private static boolean running;
    private final KeySetting toggleKey = add(new KeySetting("Старт / стоп", -1));

    public Stopwatch() {
        super("Stopwatch", "Секундомер: клавиша старт/стоп в настройках", Category.HUD, 0.45f, 0.1f, Stopwatch::text, () -> "⏱ 01:23.45");
        add(new ButtonSetting("Сбросить", "Сброс", () -> { running = false; accumulated = 0; }));
    }

    private static long elapsed() { return accumulated + (running ? System.currentTimeMillis() - start : 0); }

    private static String text() {
        long ms = elapsed();
        return String.format("⏱ %02d:%02d.%02d", ms / 60000, (ms / 1000) % 60, (ms / 10) % 100);
    }

    @Override
    public void onTick() {
        if (toggleKey.pressed()) {
            if (running) { accumulated += System.currentTimeMillis() - start; running = false; }
            else { start = System.currentTimeMillis(); running = true; }
        }
    }
}
