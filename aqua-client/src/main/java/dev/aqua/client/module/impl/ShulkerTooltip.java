package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Наведи на шалкер-ящик — увидишь, что внутри, не открывая его. */
public class ShulkerTooltip extends Module {
    private static ShulkerTooltip instance;
    private final BoolSetting counts = add(new BoolSetting("Показывать количество", true));

    public ShulkerTooltip() {
        super("Shulker Tooltip", "Содержимое шалкер-ящика в подсказке", Category.RENDER);
        instance = this;
    }

    public static boolean on() { return instance != null && instance.isEnabled(); }

    /** Добавляет строки с содержимым в подсказку предмета. */
    public static void appendTooltip(ItemStack stack, List<Text> lines) {
        if (!on() || stack == null || !isShulker(stack)) return;
        //#if MC >= 1.20.5
        net.minecraft.component.type.ContainerComponent c = stack.get(net.minecraft.component.DataComponentTypes.CONTAINER);
        if (c == null) return;
        java.util.List<ItemStack> items = c.stream().filter(s -> !s.isEmpty()).limit(27).toList();
        //#else
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        net.minecraft.nbt.NbtCompound tag = stack.getSubNbt("BlockEntityTag");
        if (tag != null && tag.contains("Items")) {
            net.minecraft.util.collection.DefaultedList<ItemStack> inv = net.minecraft.util.collection.DefaultedList.ofSize(27, ItemStack.EMPTY);
            net.minecraft.inventory.Inventories.readNbt(tag, inv);
            for (ItemStack s : inv) if (!s.isEmpty()) items.add(s);
        }
        //#endif
        if (items.isEmpty()) { lines.add(Text.literal("  " + tr("пусто", "empty")).formatted(Formatting.DARK_GRAY)); return; }
        for (ItemStack s : items) {
            String cnt = instance.counts.isOn() && s.getCount() > 1 ? " §7×" + s.getCount() : "";
            lines.add(Text.literal("  ").append(s.getName().copy().formatted(Formatting.GRAY)).append(Text.literal(cnt)));
        }
    }

    private static boolean isShulker(ItemStack s) {
        return s.isOf(Items.SHULKER_BOX) || s.isOf(Items.WHITE_SHULKER_BOX) || s.isOf(Items.ORANGE_SHULKER_BOX)
                || s.isOf(Items.MAGENTA_SHULKER_BOX) || s.isOf(Items.LIGHT_BLUE_SHULKER_BOX) || s.isOf(Items.YELLOW_SHULKER_BOX)
                || s.isOf(Items.LIME_SHULKER_BOX) || s.isOf(Items.PINK_SHULKER_BOX) || s.isOf(Items.GRAY_SHULKER_BOX)
                || s.isOf(Items.LIGHT_GRAY_SHULKER_BOX) || s.isOf(Items.CYAN_SHULKER_BOX) || s.isOf(Items.PURPLE_SHULKER_BOX)
                || s.isOf(Items.BLUE_SHULKER_BOX) || s.isOf(Items.BROWN_SHULKER_BOX) || s.isOf(Items.GREEN_SHULKER_BOX)
                || s.isOf(Items.RED_SHULKER_BOX) || s.isOf(Items.BLACK_SHULKER_BOX);
    }

    private static String tr(String ru, String en) { return dev.aqua.client.util.Lang.t(ru, en); }
}
