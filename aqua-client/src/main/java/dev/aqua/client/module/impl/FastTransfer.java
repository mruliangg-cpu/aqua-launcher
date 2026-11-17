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

/**
 * Быстрый перенос (как в Lunar): в сундуке, инвентаре, шалкере или эндер-сундуке
 * зажми ЛКМ и веди курсором по предметам — каждый, которого коснёшься, перекидывается
 * в другой инвентарь (как шифт-клик). Работает с пустой рукой: начни протяг с пустого
 * слота и веди по предметам. Опрос мыши идёт через GLFW в событии отрисовки экрана —
 * кросс-версийно и без краша (методы мыши в 1.21.9 сменили сигнатуру).
 */
public class FastTransfer extends Module {
    private static FastTransfer instance;
    private static int lastSlot = -1;   // слот, который уже обработали, пока курсор на нём

    public FastTransfer() {
        super("Fast Transfer", "Зажми ЛКМ и веди по предметам — быстрый перенос в другой инвентарь", Category.MISC);
        instance = this;
    }

    public static boolean on() { return instance != null && instance.isEnabled(); }

    /** Вызывается каждый кадр из события отрисовки экрана (AquaClient). */
    public static void frame(HandledScreen<?> screen, double mx, double my) {
        if (!on() || screen == null) { lastSlot = -1; return; }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null || mc.player == null || mc.interactionManager == null) { lastSlot = -1; return; }

        boolean down = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (!down) { lastSlot = -1; return; }                        // кнопку отпустили — сбрасываем

        ScreenHandler h = screen.getScreenHandler();
        if (!h.getCursorStack().isEmpty()) return;                   // только с пустой рукой
        Slot s = ((HandledScreenMixin) screen).aqua$getSlotAt(mx, my);
        if (s == null) { lastSlot = -1; return; }                    // ушли с любого слота — можно заходить заново
        if (s.id == lastSlot) return;                                // всё ещё на том же слоте — не спамим
        lastSlot = s.id;                                             // зашли на новый слот — запоминаем
        if (s.hasStack()) mc.interactionManager.clickSlot(h.syncId, s.id, 0, SlotActionType.QUICK_MOVE, mc.player);
    }
}
