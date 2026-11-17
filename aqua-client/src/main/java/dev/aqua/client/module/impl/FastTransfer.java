package dev.aqua.client.module.impl;

import dev.aqua.client.mixin.HandledScreenMixin;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.HashSet;
import java.util.Set;

/**
 * Быстрый перенос (как в Lunar): в сундуке, инвентаре, шалкере или эндер-сундуке
 * зажми ЛКМ и веди курсором по предметам — каждый, которого коснёшься, перекидывается
 * в другой инвентарь (как шифт-клик). Работает только когда рука пустая.
 * Логику дёргают события экрана Fabric в AquaClient — кросс-версийно и без краша.
 */
public class FastTransfer extends Module {
    private static FastTransfer instance;
    private final BoolSetting onClick = add(new BoolSetting("Переносить и по клику", true));

    private static boolean dragging;
    private static final Set<Integer> moved = new HashSet<>();

    public FastTransfer() {
        super("Fast Transfer", "Зажми ЛКМ и веди по предметам — быстрый перенос в другой инвентарь", Category.MISC);
        instance = this;
    }

    public static boolean on() { return instance != null && instance.isEnabled(); }
    public static boolean onClickToo() { return instance != null && instance.onClick.isOn(); }
    public static boolean dragging() { return dragging; }
    public static void begin() { dragging = true; moved.clear(); }
    public static void end() { dragging = false; moved.clear(); }

    /** Перекинуть предмет из слота под курсором. true — если перекинули. */
    public static boolean transfer(HandledScreen<?> screen, double mx, double my) {
        if (!on() || screen == null) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return false;
        ScreenHandler h = screen.getScreenHandler();
        if (!h.getCursorStack().isEmpty()) return false;                 // только с пустой рукой
        Slot s = ((HandledScreenMixin) screen).aqua$getSlotAt(mx, my);
        if (s == null || !s.hasStack()) return false;
        if (!moved.add(s.id)) return false;                              // каждый слот один раз за протяг
        mc.interactionManager.clickSlot(h.syncId, s.id, 0, SlotActionType.QUICK_MOVE, mc.player);
        return true;
    }
}
