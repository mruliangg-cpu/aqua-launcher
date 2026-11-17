package dev.aqua.client.module.impl;

import dev.aqua.client.mixin.HandledScreenMixin;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

/**
 * Быстрый перенос (как в Lunar): в сундуке, инвентаре, шалкере или эндер-сундуке
 * зажми ЛКМ и веди курсором по предметам — каждый, которого коснёшься, перекидывается
 * в другой инвентарь (как шифт-клик). Работает с пустой рукой: начни протяг с пустого
 * слота и веди по предметам. Опрос мыши идёт через GLFW в событии отрисовки экрана —
 * кросс-версийно и без краша (методы мыши в 1.21.9 сменили сигнатуру).
 */
public class FastTransfer extends Module {
    private static FastTransfer instance;
    private static boolean wasDown;
    private static final Set<Integer> moved = new HashSet<>();

    public FastTransfer() {
        super("Fast Transfer", "Зажми ЛКМ и веди по предметам — быстрый перенос в другой инвентарь", Category.MISC);
        instance = this;
    }

    public static boolean on() { return instance != null && instance.isEnabled(); }

    /** Вызывается каждый кадр из события отрисовки экрана (AquaClient). */
    public static void frame(HandledScreen<?> screen, double mx, double my) {
        if (!on() || screen == null) { wasDown = false; moved.clear(); return; }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) return;
        boolean down = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (down && !wasDown) moved.clear();        // начало протяга
        if (down) transfer(screen, mx, my);          // ведём — переносим
        if (!down && wasDown) moved.clear();         // отпустили
        wasDown = down;
    }

    private static void transfer(HandledScreen<?> screen, double mx, double my) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;
        ScreenHandler h = screen.getScreenHandler();
        if (!h.getCursorStack().isEmpty()) return;                   // только с пустой рукой
        Slot s = ((HandledScreenMixin) screen).aqua$getSlotAt(mx, my);
        if (s == null || !s.hasStack()) return;
        if (!moved.add(s.id)) return;                                // каждый слот один раз за протяг
        mc.interactionManager.clickSlot(h.syncId, s.id, 0, SlotActionType.QUICK_MOVE, mc.player);
    }
}
