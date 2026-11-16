package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;

/**
 * Быстрый перенос (как в Lunar): в сундуке, инвентаре, шалкере или эндер-сундуке
 * зажми ЛКМ и веди курсором по предметам — каждый, которого коснёшься, перекидывается
 * в другой инвентарь (как шифт-клик). Работает только когда рука пустая.
 */
public class FastTransfer extends Module {
    private static FastTransfer instance;
    private final BoolSetting onClick = add(new BoolSetting("Переносить и по клику", true));

    public FastTransfer() {
        super("Fast Transfer", "Зажми ЛКМ и веди по предметам — быстрый перенос в другой инвентарь", Category.MISC);
        instance = this;
    }

    public static boolean on() { return instance != null && instance.isEnabled(); }
    public static boolean onClickToo() { return instance != null && instance.onClick.isOn(); }
}
