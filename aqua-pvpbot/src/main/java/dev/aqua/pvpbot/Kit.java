package dev.aqua.pvpbot;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/** The PvP kits. The same kit is given to you (/kit) and to the bot (/spawnbot), so fights are fair. */
public enum Kit {
    SWORD("sword", "Sword", ChatFormatting.AQUA),
    DIAMOND("diamond", "Diamond", ChatFormatting.AQUA),
    OP("op", "OP", ChatFormatting.LIGHT_PURPLE),
    NETHPOT("nethpot", "NethPot", ChatFormatting.RED),
    CRYSTAL("crystal", "Crystal", ChatFormatting.DARK_PURPLE),
    MACE("mace", "Mace", ChatFormatting.GRAY);

    public final String id;
    public final String label;
    public final ChatFormatting color;

    Kit(String id, String label, ChatFormatting color) { this.id = id; this.label = label; this.color = color; }

    public static Kit byId(String id) {
        for (Kit k : values()) if (k.id.equalsIgnoreCase(id)) return k;
        if (id.equalsIgnoreCase("pot") || id.equalsIgnoreCase("neth") || id.equalsIgnoreCase("netherite")) return NETHPOT;
        if (id.equalsIgnoreCase("cpvp") || id.equalsIgnoreCase("crystals")) return CRYSTAL;
        return null;
    }

    /** Clears the player's inventory and gives this kit. */
    public void apply(ServerPlayer p) {
        Inventory inv = p.getInventory();
        inv.clearContent();
        p.removeAllEffects();
        p.setHealth(p.getMaxHealth());
        p.getFoodData().setFoodLevel(20);
        p.getFoodData().setSaturation(10f);
        p.clearFire();
        p.setAbsorptionAmount(0f);
        new Builder(p).build(this);
        inv.setSelectedSlot(0);
        p.containerMenu.broadcastChanges();
        p.inventoryMenu.broadcastChanges();
    }

    private static final class Builder {
        final ServerPlayer p;
        final HolderLookup.RegistryLookup<Enchantment> ench;
        int next = 0; // next free hotbar / inventory slot

        Builder(ServerPlayer p) {
            this.p = p;
            this.ench = p.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        }

        Holder<Enchantment> e(ResourceKey<Enchantment> key) { return ench.getOrThrow(key); }

        ItemStack item(Item item, int count) { return new ItemStack(item, count); }

        ItemStack enchanted(Item item, Object... pairs) {
            ItemStack s = new ItemStack(item);
            for (int i = 0; i + 1 < pairs.length; i += 2) {
                @SuppressWarnings("unchecked") ResourceKey<Enchantment> key = (ResourceKey<Enchantment>) pairs[i];
                s.enchant(e(key), (Integer) pairs[i + 1]);
            }
            return s;
        }

        ItemStack potion(Item kind, Holder<Potion> potion, int count) {
            ItemStack s = PotionContents.createItemStack(kind, potion);
            s.setCount(count);
            return s;
        }

        void add(ItemStack s) {
            Inventory inv = p.getInventory();
            while (next < 36 && !inv.getItem(next).isEmpty()) next++;
            if (next < 36) inv.setItem(next++, s);
        }

        /** fill every empty main-inventory slot (not the hotbar) with copies of s */
        void fill(ItemStack s) {
            Inventory inv = p.getInventory();
            for (int i = 9; i < 36; i++) if (inv.getItem(i).isEmpty()) inv.setItem(i, s.copy());
        }

        void armor(Item helmet, Item chest, Item legs, Item boots, int prot, boolean blastLegs) {
            p.setItemSlot(EquipmentSlot.HEAD, enchanted(helmet, Enchantments.PROTECTION, prot, Enchantments.UNBREAKING, 3, Enchantments.MENDING, 1));
            p.setItemSlot(EquipmentSlot.CHEST, enchanted(chest, Enchantments.PROTECTION, prot, Enchantments.UNBREAKING, 3, Enchantments.MENDING, 1));
            p.setItemSlot(EquipmentSlot.LEGS, blastLegs
                    ? enchanted(legs, Enchantments.BLAST_PROTECTION, 4, Enchantments.UNBREAKING, 3, Enchantments.MENDING, 1)
                    : enchanted(legs, Enchantments.PROTECTION, prot, Enchantments.UNBREAKING, 3, Enchantments.MENDING, 1));
            p.setItemSlot(EquipmentSlot.FEET, enchanted(boots, Enchantments.PROTECTION, prot, Enchantments.FEATHER_FALLING, 4,
                    Enchantments.UNBREAKING, 3, Enchantments.MENDING, 1));
        }

        void netheriteArmor(boolean blastLegs) {
            armor(Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS, 4, blastLegs);
        }

        ItemStack netheriteSword() {
            return enchanted(Items.NETHERITE_SWORD, Enchantments.SHARPNESS, 5, Enchantments.UNBREAKING, 3, Enchantments.MENDING, 1);
        }

        void build(Kit kit) {
            switch (kit) {
                case SWORD -> {
                    armor(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS, 2, false);
                    add(enchanted(Items.DIAMOND_SWORD, Enchantments.SHARPNESS, 3, Enchantments.UNBREAKING, 3));
                }
                case DIAMOND -> {
                    armor(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS, 3, false);
                    add(enchanted(Items.DIAMOND_SWORD, Enchantments.SHARPNESS, 4, Enchantments.UNBREAKING, 3));
                    add(enchanted(Items.DIAMOND_AXE, Enchantments.SHARPNESS, 3, Enchantments.UNBREAKING, 3));
                    add(item(Items.GOLDEN_APPLE, 8));
                    add(enchanted(Items.BOW, Enchantments.POWER, 3, Enchantments.UNBREAKING, 3));
                    add(item(Items.COOKED_BEEF, 32));
                    add(item(Items.ARROW, 32));
                    p.setItemSlot(EquipmentSlot.OFFHAND, enchanted(Items.SHIELD, Enchantments.UNBREAKING, 3));
                }
                case OP -> {
                    netheriteArmor(false);
                    add(enchanted(Items.NETHERITE_SWORD, Enchantments.SHARPNESS, 5, Enchantments.FIRE_ASPECT, 2,
                            Enchantments.UNBREAKING, 3, Enchantments.MENDING, 1));
                    add(enchanted(Items.NETHERITE_AXE, Enchantments.SHARPNESS, 5, Enchantments.UNBREAKING, 3));
                    add(item(Items.ENCHANTED_GOLDEN_APPLE, 4));
                    add(item(Items.GOLDEN_APPLE, 32));
                    add(item(Items.ENDER_PEARL, 16));
                    add(potion(Items.POTION, Potions.STRONG_STRENGTH, 1));
                    add(potion(Items.POTION, Potions.STRONG_SWIFTNESS, 1));
                    add(item(Items.TOTEM_OF_UNDYING, 1));
                    add(enchanted(Items.SHIELD, Enchantments.UNBREAKING, 3));
                    add(potion(Items.POTION, Potions.STRONG_STRENGTH, 1));
                    add(potion(Items.POTION, Potions.STRONG_SWIFTNESS, 1));
                    add(potion(Items.POTION, Potions.LONG_FIRE_RESISTANCE, 1));
                    add(item(Items.TOTEM_OF_UNDYING, 1));
                    add(item(Items.TOTEM_OF_UNDYING, 1));
                    p.setItemSlot(EquipmentSlot.OFFHAND, item(Items.TOTEM_OF_UNDYING, 1));
                }
                case NETHPOT -> {
                    netheriteArmor(false);
                    add(netheriteSword());
                    add(enchanted(Items.NETHERITE_AXE, Enchantments.SHARPNESS, 5, Enchantments.UNBREAKING, 3));
                    add(item(Items.ENDER_PEARL, 16));
                    add(potion(Items.SPLASH_POTION, Potions.STRONG_STRENGTH, 1));
                    add(potion(Items.SPLASH_POTION, Potions.STRONG_SWIFTNESS, 1));
                    add(item(Items.GOLDEN_APPLE, 16));
                    add(potion(Items.SPLASH_POTION, Potions.STRONG_HEALING, 1));
                    add(potion(Items.SPLASH_POTION, Potions.STRONG_HEALING, 1));
                    add(item(Items.EXPERIENCE_BOTTLE, 64));
                    add(item(Items.TOTEM_OF_UNDYING, 1));
                    add(potion(Items.SPLASH_POTION, Potions.STRONG_STRENGTH, 1));
                    add(potion(Items.SPLASH_POTION, Potions.STRONG_SWIFTNESS, 1));
                    fill(potion(Items.SPLASH_POTION, Potions.STRONG_HEALING, 1));
                    p.setItemSlot(EquipmentSlot.OFFHAND, item(Items.TOTEM_OF_UNDYING, 1));
                }
                case CRYSTAL -> {
                    netheriteArmor(true);
                    add(netheriteSword());
                    add(item(Items.OBSIDIAN, 64));
                    add(item(Items.END_CRYSTAL, 64));
                    add(item(Items.GOLDEN_APPLE, 64));
                    add(item(Items.ENDER_PEARL, 16));
                    add(item(Items.RESPAWN_ANCHOR, 32));
                    add(item(Items.GLOWSTONE, 64));
                    add(item(Items.EXPERIENCE_BOTTLE, 64));
                    add(enchanted(Items.NETHERITE_PICKAXE, Enchantments.EFFICIENCY, 5, Enchantments.UNBREAKING, 3, Enchantments.MENDING, 1));
                    add(item(Items.OBSIDIAN, 64));
                    add(item(Items.END_CRYSTAL, 64));
                    add(item(Items.EXPERIENCE_BOTTLE, 64));
                    for (int i = 0; i < 12; i++) add(item(Items.TOTEM_OF_UNDYING, 1));
                    p.setItemSlot(EquipmentSlot.OFFHAND, item(Items.TOTEM_OF_UNDYING, 1));
                }
                case MACE -> {
                    netheriteArmor(false);
                    add(enchanted(Items.MACE, Enchantments.DENSITY, 5, Enchantments.WIND_BURST, 1, Enchantments.UNBREAKING, 3));
                    add(netheriteSword());
                    add(item(Items.WIND_CHARGE, 64));
                    add(item(Items.GOLDEN_APPLE, 16));
                    add(item(Items.ENDER_PEARL, 16));
                    add(enchanted(Items.NETHERITE_AXE, Enchantments.SHARPNESS, 5, Enchantments.UNBREAKING, 3));
                    add(enchanted(Items.SHIELD, Enchantments.UNBREAKING, 3));
                    add(item(Items.WIND_CHARGE, 64));
                    for (int i = 0; i < 4; i++) add(item(Items.TOTEM_OF_UNDYING, 1));
                    p.setItemSlot(EquipmentSlot.OFFHAND, item(Items.TOTEM_OF_UNDYING, 1));
                }
            }
        }
    }
}
