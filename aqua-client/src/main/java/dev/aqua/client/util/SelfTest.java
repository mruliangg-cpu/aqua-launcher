package dev.aqua.client.util;

import dev.aqua.client.gui.ClickGuiScreen;
import dev.aqua.client.gui.HudEditorScreen;
import dev.aqua.client.module.Module;
import dev.aqua.client.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Самопроверка для облачного теста (только если рядом с игрой лежит aqua-selftest.flag):
 * создаёт мир, по очереди включает каждый модуль, открывает меню и редактор HUD и пишет в лог
 * строки «AQUA-SELFTEST …». У обычных игроков ничего не делает.
 */
public final class SelfTest {
    private static final boolean ON = new File("aqua-selftest.flag").exists();
    private static int tick, stage, idx, wait;
    private static final List<String> FAIL = new ArrayList<>();

    private SelfTest() {}

    public static boolean enabled() { return ON; }

    private static void log(String s) { System.out.println("AQUA-SELFTEST " + s); }

    public static void onTick(MinecraftClient mc) {
        if (!ON) return;
        if (tick == 0) log("started");
        tick++;
        try {
            step(mc);
        } catch (Throwable t) {
            log("STEP-FAIL stage=" + stage + " " + t);
            stage = 99;
        }
    }

    private static void step(MinecraftClient mc) throws Exception {
        if (wait > 0) { wait--; return; }
        switch (stage) {
            case 0 -> { // ждём главное меню и открываем создание мира
                if (tick % 200 == 0) log("tick " + tick + " screen=" + (mc.currentScreen == null ? "null" : mc.currentScreen.getClass().getName()));
                if (tick < 100) return;
                if (!(mc.currentScreen instanceof TitleScreen)) {
                    // первый запуск показывает экран «Доступность» — пропускаем его
                    if (tick > 300 && mc.getOverlay() == null) { log("skip screen " + (mc.currentScreen == null ? "null" : mc.currentScreen.getClass().getSimpleName())); mc.setScreen(new TitleScreen()); }
                    return;
                }
                log("title screen reached");
                Class<?> cws = Class.forName("net.minecraft.client.gui.screen.world.CreateWorldScreen");
                for (Method m : cws.getDeclaredMethods()) {
                    if (!java.lang.reflect.Modifier.isStatic(m.getModifiers()) || !m.getName().equals("show")) continue;
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length != 2 || p[0] != MinecraftClient.class) continue;
                    m.setAccessible(true);
                    Object parent = p[1] == Runnable.class ? (Runnable) () -> {} : p[1].isAssignableFrom(Screen.class) ? mc.currentScreen : null;
                    m.invoke(null, mc, parent);
                    log("CreateWorldScreen.show(" + p[1].getSimpleName() + ")");
                    break;
                }
                stage = 1; wait = 60;
            }
            case 1 -> { // нажимаем «Создать мир»
                Screen s = mc.currentScreen;
                if (s == null) return;
                Method create = null;
                for (Class<?> c = s.getClass(); c != null && create == null; c = c.getSuperclass())
                    for (Method m : c.getDeclaredMethods()) if (m.getName().equals("createLevel") && m.getParameterCount() == 0) create = m;
                if (create == null) { log("no createLevel on " + s.getClass().getName()); stage = 99; return; }
                create.setAccessible(true);
                create.invoke(s);
                log("createLevel invoked");
                stage = 2;
            }
            case 2 -> { // ждём игрока в мире
                if (mc.player == null || mc.world == null) { if (tick > 6000) { log("world timeout"); stage = 99; } return; }
                log("in world");
                for (Module m : ModuleManager.all()) if (m.isEnabled()) m.setEnabled(false);
                stage = 3; wait = 40;
            }
            case 3 -> { // по очереди включаем модули
                List<Module> all = ModuleManager.all();
                if (idx > 0) {
                    Module prev = all.get(idx - 1);
                    try { prev.setEnabled(false); } catch (Throwable t) { FAIL.add(prev.getName() + " disable: " + t); log("FAIL " + prev.getName() + " disable: " + t); }
                }
                if (idx >= all.size()) { stage = 4; return; }
                Module m = all.get(idx++);
                try {
                    m.setEnabled(true);
                    for (int i = 0; i < 3; i++) m.onTick();
                    log("ok " + m.getName());
                } catch (Throwable t) {
                    FAIL.add(m.getName() + ": " + t);
                    log("FAIL " + m.getName() + ": " + t);
                    StackTraceElement[] st = t.getStackTrace();
                    for (int i = 0; i < Math.min(4, st.length); i++) log("   at " + st[i]);
                }
                wait = 6; // несколько кадров с включённым модулем (HUD, мир)
            }
            case 4 -> { // все модули сразу + меню + редактор HUD
                for (Module m : ModuleManager.all()) { try { m.setEnabled(true); } catch (Throwable ignored) {} }
                mc.setScreen(new ClickGuiScreen());
                log("ClickGui opened");
                stage = 5; wait = 60;
            }
            case 5 -> {
                mc.setScreen(new HudEditorScreen());
                log("HudEditor opened");
                stage = 6; wait = 60;
            }
            case 6 -> {
                mc.setScreen(null);
                stage = 7; wait = 100;
            }
            case 7 -> {
                log("DONE modules=" + ModuleManager.all().size() + " failures=" + FAIL.size());
                for (String f : FAIL) log("SUMMARY " + f);
                stage = 99;
            }
            case 99 -> { log("stopping"); stage = 100; mc.scheduleStop(); }
            default -> {}
        }
    }
}
