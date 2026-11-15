package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.HudModule;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ModeSetting;
import dev.aqua.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

public class ArmorStatus extends HudModule {
    private final ModeSetting mode = add(new ModeSetting("Прочность", "Число", "Число", "Процент", "Нет"));
    private final BoolSetting hand = add(new BoolSetting("Предмет в руке", true));
    private final BoolSetting horizontal = add(new BoolSetting("Горизонтально", false));

    public ArmorStatus() {
        super("Armor Status", "Броня и предмет в руке с прочностью", Category.HUD, 0.005f, 0.55f);
        background.set(false);
    }

    @Override
    protected void draw(DrawContext ctx, boolean editor) {
        List<ItemStack> stacks = new ArrayList<>();
        if (mc.player != null) {
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            for (EquipmentSlot s : slots) {
                ItemStack st = mc.player.getEquippedStack(s);
                if (!st.isEmpty()) stacks.add(st);
            }
            if (hand.isOn() && !mc.player.getMainHandStack().isEmpty()) stacks.add(mc.player.getMainHandStack());
        }
        if (stacks.isEmpty() && editor) {
            stacks.add(new ItemStack(Items.DIAMOND_HELMET));
            stacks.add(new ItemStack(Items.DIAMOND_CHESTPLATE));
            stacks.add(new ItemStack(Items.DIAMOND_LEGGINGS));
            stacks.add(new ItemStack(Items.DIAMOND_BOOTS));
        }
        if (stacks.isEmpty()) { width = 0; height = 0; return; }

        boolean hor = horizontal.isOn();
        int maxText = 0;
        for (ItemStack st : stacks) maxText = Math.max(maxText, mc.textRenderer.getWidth(label(st)));
        int cellW = hor ? 18 + Math.max(0, maxText - 4) : 18 + (maxText > 0 ? maxText + 3 : 0);
        width = hor ? stacks.size() * cellW + 4 : cellW + 4;
        height = hor ? (mode.is("Нет") ? 20 : 30) : stacks.size() * 18 + 4;
        drawBackground(ctx, width, height);

        int i = 0;
        for (ItemStack st : stacks) {
            int x = hor ? 2 + i * cellW : 2, y = hor ? 2 : 2 + i * 18;
            ctx.drawItem(st, x, y);
            dev.aqua.client.compat.V.itemOverlay(ctx, mc.textRenderer, st, x, y);
            String l = label(st);
            if (!l.isEmpty()) {
                int col = durabilityColor(st);
                if (hor) ctx.drawText(mc.textRenderer, l, x + 8 - mc.textRenderer.getWidth(l) / 2, y + 18, col, shadow.isOn());
                else ctx.drawText(mc.textRenderer, l, x + 20, y + 4, col, shadow.isOn());
            }
            i++;
        }
    }

    private String label(ItemStack st) {
        if (mode.is("Нет")) return "";
        if (!st.isDamageable()) return st.getCount() > 1 ? String.valueOf(st.getCount()) : "";
        int left = st.getMaxDamage() - st.getDamage();
        return mode.is("Процент") ? Math.round(left * 100f / st.getMaxDamage()) + "%" : String.valueOf(left);
    }

    private int durabilityColor(ItemStack st) {
        if (!st.isDamageable()) return textColor.color();
        float p = (st.getMaxDamage() - st.getDamage()) / (float) st.getMaxDamage();
        return ColorUtil.fromHsb(p / 3f, 0.75f, 1f, 255);
    }
}
