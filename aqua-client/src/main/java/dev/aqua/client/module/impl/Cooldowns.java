package dev.aqua.client.module.impl;

import dev.aqua.client.gui.Render2D;
import dev.aqua.client.gui.Theme;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.HudModule;
import dev.aqua.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Перезарядка жемчуга, хоруса, щита, заряда ветра и рога. */
public class Cooldowns extends HudModule {
    private static final Item[] ITEMS = {Items.ENDER_PEARL, Items.CHORUS_FRUIT, Items.SHIELD, Items.WIND_CHARGE, Items.GOAT_HORN};

    public Cooldowns() {
        super("Cooldowns", "Перезарядка жемчуга Края, щита и др.", Category.COMBAT, 0.45f, 0.75f);
    }

    @Override
    protected void draw(DrawContext ctx, boolean editor) {
        List<Item> active = new ArrayList<>();
        List<Float> progress = new ArrayList<>();
        if (mc.player != null) {
            for (Item it : ITEMS) {
                float p = mc.player.getItemCooldownManager().getCooldownProgress(it, 0f);
                if (p > 0) { active.add(it); progress.add(p); }
            }
        }
        if (active.isEmpty() && editor) { active.add(Items.ENDER_PEARL); progress.add(0.6f); }
        if (active.isEmpty()) { width = 0; height = 0; return; }
        width = 90;
        height = active.size() * 20 + 2;
        drawBackground(ctx, width, height);
        for (int i = 0; i < active.size(); i++) {
            int y = 2 + i * 20;
            ctx.drawItem(new ItemStack(active.get(i)), 2, y);
            float p = progress.get(i);
            Render2D.roundRect(ctx, 22, y + 6, 62, 5, 2.5f, 0xFF0E3350);
            Render2D.roundRect(ctx, 22, y + 6, Math.max(5, 62 * p), 5, 2.5f, ColorUtil.lerp(Theme.EYE, Theme.ACCENT, p));
        }
    }
}
