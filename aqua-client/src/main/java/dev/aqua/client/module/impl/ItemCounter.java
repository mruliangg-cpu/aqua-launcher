package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.HudModule;
import dev.aqua.client.setting.BoolSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.LinkedHashMap;
import java.util.Map;

public class ItemCounter extends HudModule {
    private final Map<Item, BoolSetting> items = new LinkedHashMap<>();

    public ItemCounter() {
        super("Item Counter", "Сколько у вас жемчуга, яблок, стрел и тотемов", Category.HUD, 0.005f, 0.9f);
        items.put(Items.ENDER_PEARL, add(new BoolSetting("Жемчуг Края", true)));
        items.put(Items.GOLDEN_APPLE, add(new BoolSetting("Золотые яблоки", true)));
        items.put(Items.ARROW, add(new BoolSetting("Стрелы", true)));
        items.put(Items.TOTEM_OF_UNDYING, add(new BoolSetting("Тотемы", true)));
        items.put(Items.EXPERIENCE_BOTTLE, add(new BoolSetting("Пузырьки опыта", false)));
        items.put(Items.COBWEB, add(new BoolSetting("Паутина", false)));
    }

    @Override
    protected void draw(DrawContext ctx, boolean editor) {
        int x = 2, shown = 0;
        int startW = 0;
        for (Map.Entry<Item, BoolSetting> e : items.entrySet()) {
            if (!e.getValue().isOn()) continue;
            int count = mc.player != null ? mc.player.getInventory().count(e.getKey()) : 0;
            if (count == 0 && !editor) continue;
            if (count == 0) count = 16;
            String c = String.valueOf(count);
            startW += 18 + mc.textRenderer.getWidth(c) + 4;
            shown++;
        }
        if (shown == 0) { width = 0; height = 0; return; }
        width = startW + 2;
        height = 20;
        drawBackground(ctx, width, height);
        for (Map.Entry<Item, BoolSetting> e : items.entrySet()) {
            if (!e.getValue().isOn()) continue;
            int count = mc.player != null ? mc.player.getInventory().count(e.getKey()) : 0;
            if (count == 0 && !editor) continue;
            if (count == 0) count = 16;
            ctx.drawItem(new ItemStack(e.getKey()), x, 2);
            String c = String.valueOf(count);
            text(ctx, c, x + 18, 6);
            x += 18 + mc.textRenderer.getWidth(c) + 4;
        }
    }
}
