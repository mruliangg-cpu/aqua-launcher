package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.module.TextHudModule;
import dev.aqua.client.util.ClickTracker;
import dev.aqua.client.util.CombatTracker;
import dev.aqua.client.util.Lang;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 50 модулей для Bedwars и PvP. Только честные, разрешённые на серверах вещи:
 * информация, счётчики, индикаторы и подсказки на экране — ничего не бьёт, не целится
 * и не играет за тебя. Все выключены по умолчанию; нужные включаешь сам или сборкой.
 */
public final class BedwarsModules {
    private BedwarsModules() {}

    private static MinecraftClient mc() { return MinecraftClient.getInstance(); }
    private static ClientPlayerEntity p() { return mc().player; }
    private static String t(String ru, String en) { return Lang.t(ru, en); }
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static int idx;
    private static TextHudModule hud(String name, String desc, Category cat, Supplier<String> text, String preview) {
        int i = idx++;
        float x = 0.015f + (i / 16 % 3) * 0.33f, y = 0.08f + (i % 16) * 0.05f;
        return new TextHudModule(name, desc, cat, x, y, () -> { try { return p() == null ? null : text.get(); } catch (Exception e) { return null; } }, () -> preview);
    }

    // ---- общие помощники (всё только клиентские данные) ----
    private static int countItem(Item it) {
        if (p() == null) return 0;
        int n = 0;
        for (int i = 0; i < p().getInventory().size(); i++) { ItemStack s = p().getInventory().getStack(i); if (s.isOf(it)) n += s.getCount(); }
        return n;
    }
    private static int countBlocks() {
        if (p() == null) return 0;
        int n = 0;
        for (int i = 0; i < p().getInventory().size(); i++) { ItemStack s = p().getInventory().getStack(i); if (s.getItem() instanceof BlockItem) n += s.getCount(); }
        return n;
    }
    private static boolean hasType(Class<?> cls) {
        if (p() == null) return false;
        for (int i = 0; i < p().getInventory().size(); i++) if (cls.isInstance(p().getInventory().getStack(i).getItem())) return true;
        return false;
    }
    private static ItemStack firstOfType(Class<?> cls) {
        if (p() == null) return ItemStack.EMPTY;
        for (int i = 0; i < p().getInventory().size(); i++) { ItemStack s = p().getInventory().getStack(i); if (cls.isInstance(s.getItem())) return s; }
        return ItemStack.EMPTY;
    }
    private static String dir(float yaw) {
        yaw = (yaw % 360 + 360) % 360;
        String[] d = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        return d[Math.round(yaw / 45f) % 8];
    }
    private static double hSpeed() {
        if (p() == null) return 0;
        Vec3d v = p().getVelocity();
        return Math.sqrt(v.x * v.x + v.z * v.z) * 20.0;
    }
    private static PlayerEntity nearestPlayer() {
        if (p() == null || mc().world == null) return null;
        PlayerEntity best = null; double bd = Double.MAX_VALUE;
        for (PlayerEntity pl : mc().world.getPlayers()) {
            if (pl == p() || pl.isSpectator()) continue;
            double d = pl.squaredDistanceTo(p());
            if (d < bd) { bd = d; best = pl; }
        }
        return best;
    }
    private static boolean recent(long time, long ms) { return time > 0 && System.currentTimeMillis() - time < ms; }

    public static List<Module> all() {
        List<Module> L = new ArrayList<>();
        Category CB = Category.COMBAT, HD = Category.HUD, MS = Category.MISC, RN = Category.RENDER;

        // ===================== БОЙ / ФИДБЕК (15) =====================
        L.add(hud("CPS Left", "Кликов левой кнопкой в секунду", CB, () -> ClickTracker.left() + " CPS", "8 CPS"));
        L.add(hud("CPS Right", "Кликов правой кнопкой в секунду", CB, () -> ClickTracker.right() + t(" CPS (ПКМ)", " CPS (R)"), "6 CPS (R)"));
        L.add(hud("CPS Total", "Всего кликов в секунду", CB, () -> (ClickTracker.left() + ClickTracker.right()) + " CPS", "14 CPS"));
        L.add(hud("Combo", "Текущее комбо по цели", CB, () -> recent(CombatTracker.lastHitTime, 3000) && CombatTracker.combo > 1 ? t("Комбо ", "Combo ") + CombatTracker.combo : null, "Combo 5"));
        L.add(hud("Reach", "Дистанция последнего удара", CB, () -> recent(CombatTracker.lastReachTime, 3000) && CombatTracker.lastReach > 0 ? String.format(t("Дистанция %.2f", "Reach %.2f"), CombatTracker.lastReach) : null, "Reach 2.91"));
        L.add(hud("Attack Charge", "Заряд удара (бей на 100%)", CB, () -> p() == null ? null : Math.round(p().getAttackCooldownProgress(0f) * 100) + "%", "100%"));
        L.add(hud("Sprint", "Индикатор спринта", CB, () -> p() != null && p().isSprinting() ? t("Спринт", "Sprint") : null, "Sprint"));
        L.add(hud("Sneak", "Индикатор присяда", CB, () -> p() != null && p().isSneaking() ? t("Присед", "Sneak") : null, "Sneak"));
        L.add(hud("Velocity", "Горизонтальная скорость (бл/с)", CB, () -> String.format(t("Скорость %.1f", "Speed %.1f"), hSpeed()), "Speed 5.6"));
        L.add(hud("Target", "Имя того, кого бьёшь", CB, () -> recent(CombatTracker.lastTargetTime, 3000) && CombatTracker.lastTarget != null ? t("Цель: ", "Target: ") + CombatTracker.lastTarget.getName().getString() : null, "Target: Steve"));
        L.add(hud("Target Distance", "Расстояние до цели", CB, () -> {
            if (!recent(CombatTracker.lastTargetTime, 3000) || CombatTracker.lastTarget == null || p() == null) return null;
            return String.format(t("До цели %.1f", "Target %.1f м"), Math.sqrt(CombatTracker.lastTarget.squaredDistanceTo(p()))); }, "Target 2.4 м"));
        L.add(hud("Target HP", "Здоровье цели", CB, () -> recent(CombatTracker.lastTargetTime, 3000) && CombatTracker.lastTarget != null ? "❤ " + Math.round(CombatTracker.lastTarget.getHealth()) : null, "❤ 14"));
        L.add(hud("Weapon", "Оружие в руке", CB, () -> {
            if (p() == null) return null; ItemStack h = p().getMainHandStack();
            return (h.getItem() instanceof SwordItem || h.getItem() instanceof AxeItem) ? h.getName().getString() : null; }, "Diamond Sword"));
        L.add(hud("Using Item", "Ешь/пьёшь/натягиваешь лук", CB, () -> p() != null && p().isUsingItem() ? t("Использую…", "Using…") : null, "Using…"));
        L.add(hud("In Air", "Ты в воздухе (не на земле)", CB, () -> p() != null && !p().isOnGround() ? t("В воздухе", "In air") : null, "In air"));

        // ===================== BEDWARS / ИНФО (15) =====================
        L.add(hud("Coordinates", "Твои координаты XYZ", HD, () -> p() == null ? null : p().getBlockX() + " " + p().getBlockY() + " " + p().getBlockZ(), "124 67 -310"));
        L.add(hud("Facing", "Сторона света, куда смотришь", HD, () -> p() == null ? null : t("Смотрю: ", "Facing: ") + dir(p().getYaw()), "Facing: NE"));
        L.add(hud("Build Height", "Твоя высота Y", HD, () -> p() == null ? null : "Y " + p().getBlockY(), "Y 67"));
        L.add(hud("Blocks", "Сколько блоков для стройки у тебя есть", HD, () -> t("Блоки: ", "Blocks: ") + countBlocks(), "Blocks: 48"));
        L.add(hud("Held Count", "Количество в текущем стаке", HD, () -> { if (p() == null) return null; int c = p().getMainHandStack().getCount(); return c > 1 ? "×" + c : null; }, "×64"));
        L.add(hud("Gaps", "Сколько золотых яблок у тебя", HD, () -> { int g = countItem(Items.GOLDEN_APPLE) + countItem(Items.ENCHANTED_GOLDEN_APPLE); return g > 0 ? t("Яблоки: ", "Gaps: ") + g : null; }, "Gaps: 3"));
        L.add(hud("Fireballs", "Сколько фаерболов", HD, () -> { int f = countItem(Items.FIRE_CHARGE); return f > 0 ? t("Фаербол: ", "Fireballs: ") + f : null; }, "Fireballs: 2"));
        L.add(hud("TNT", "Сколько ТНТ", HD, () -> { int n = countItem(Items.TNT); return n > 0 ? "TNT: " + n : null; }, "TNT: 4"));
        L.add(hud("Pearls", "Сколько эндер-жемчуга", HD, () -> { int n = countItem(Items.ENDER_PEARL); return n > 0 ? t("Жемчуг: ", "Pearls: ") + n : null; }, "Pearls: 1"));
        L.add(hud("Arrows", "Сколько стрел", HD, () -> { int n = countItem(Items.ARROW); return n > 0 ? t("Стрелы: ", "Arrows: ") + n : null; }, "Arrows: 8"));
        L.add(hud("Held Block", "Какой блок в руке", HD, () -> { if (p() == null) return null; ItemStack h = p().getMainHandStack(); return h.getItem() instanceof BlockItem ? h.getName().getString() : null; }, "Wool"));
        L.add(hud("Bed Nearby", "Рядом есть кровать (≤6 блоков)", HD, () -> bedNearby() ? t("Кровать рядом", "Bed nearby") : null, "Bed nearby"));
        L.add(hud("Players Online", "Сколько игроков на сервере", HD, () -> mc().getNetworkHandler() == null ? null : t("Игроков: ", "Players: ") + mc().getNetworkHandler().getPlayerList().size(), "Players: 16"));
        L.add(hud("Players Nearby", "Игроков в радиусе 24 блоков", HD, () -> {
            if (p() == null || mc().world == null) return null; int n = 0;
            for (PlayerEntity pl : mc().world.getPlayers()) if (pl != p() && pl.squaredDistanceTo(p()) < 24 * 24) n++;
            return n > 0 ? t("Рядом: ", "Nearby: ") + n : null; }, "Nearby: 2"));
        L.add(hud("Server IP", "Адрес сервера", HD, () -> mc().getCurrentServerEntry() == null ? null : mc().getCurrentServerEntry().address, "mc.hypixel.net"));

        // ===================== МАГАЗИН / QoL (10) =====================
        L.add(hud("Armor Alert", "Предупреждает, если не хватает брони", MS, () -> {
            if (p() == null) return null; int missing = 0; for (EquipmentSlot s : ARMOR) if (p().getEquippedStack(s).isEmpty()) missing++;
            return missing > 0 ? "⚠ " + t("нет брони: ", "missing armor: ") + missing : null; }, "⚠ missing armor: 1"));
        L.add(hud("No Blocks", "Громко предупреждает, когда кончились блоки", MS, () -> p() != null && countBlocks() == 0 ? "⚠ " + t("НЕТ БЛОКОВ", "NO BLOCKS") : null, "⚠ NO BLOCKS"));
        L.add(hud("Low Hunger", "Мало еды", MS, () -> p() != null && p().getHungerManager().getFoodLevel() <= 6 ? "⚠ " + t("голод", "hunger") : null, "⚠ hunger"));
        L.add(hud("Inventory Full", "Инвентарь забит", MS, () -> p() != null && p().getInventory().getEmptySlot() < 0 ? "⚠ " + t("инвентарь полон", "inv full") : null, "⚠ inv full"));
        L.add(hud("Pickaxe", "Какая кирка есть", MS, () -> { ItemStack s = firstOfType(PickaxeItem.class); return s.isEmpty() ? null : s.getName().getString(); }, "Iron Pickaxe"));
        L.add(hud("Axe", "Какой топор есть", MS, () -> { ItemStack s = firstOfType(AxeItem.class); return s.isEmpty() ? null : s.getName().getString(); }, "Iron Axe"));
        L.add(hud("Shears", "Есть ли ножницы", MS, () -> hasType(ShearsItem.class) ? t("Ножницы ✔", "Shears ✔") : null, "Shears ✔"));
        L.add(hud("No Sword", "Нет меча в инвентаре", MS, () -> p() != null && !hasType(SwordItem.class) ? "⚠ " + t("нет меча", "no sword") : null, "⚠ no sword"));
        L.add(hud("Water Bucket", "Есть ли ведро воды", MS, () -> countItem(Items.WATER_BUCKET) > 0 ? t("Вода ✔", "Water ✔") : null, "Water ✔"));
        L.add(hud("Golden Carrots", "Сколько золотой моркови (еда)", MS, () -> { int n = countItem(Items.GOLDEN_CARROT); return n > 0 ? t("Морковь: ", "Carrots: ") + n : null; }, "Carrots: 12"));

        // ===================== ВИЗУАЛ / ПРЕДУПРЕЖДЕНИЯ (10) =====================
        L.add(hud("Low HP", "Крупно мигает, когда мало здоровья", RN, () -> p() != null && p().getHealth() <= 6 && p().getHealth() > 0 ? "§c❤ " + t("МАЛО HP", "LOW HP") : null, "❤ LOW HP"));
        L.add(hud("Health", "Текущее здоровье числом", RN, () -> p() == null ? null : "❤ " + Math.round(p().getHealth()) + "/" + Math.round(p().getMaxHealth()), "❤ 20/20"));
        L.add(hud("Absorption", "Сколько жёлтых сердец (абсорбция)", RN, () -> { if (p() == null) return null; float ab = p().getAbsorptionAmount(); return ab > 0 ? "§e+ " + Math.round(ab) : null; }, "+ 4"));
        L.add(hud("Nearest Player", "Ближайший игрок и дистанция", RN, () -> {
            PlayerEntity n = nearestPlayer(); if (n == null || p() == null) return null;
            return n.getName().getString() + " " + Math.round(Math.sqrt(n.squaredDistanceTo(p()))) + t(" бл", " m"); }, "Steve 6 m"));
        L.add(hud("Enemy Close", "Предупреждение, если игрок ближе 6 блоков", RN, () -> {
            PlayerEntity n = nearestPlayer(); if (n == null || p() == null) return null;
            return n.squaredDistanceTo(p()) < 36 ? "⚠ " + t("игрок рядом", "enemy close") : null; }, "⚠ enemy close"));
        L.add(hud("Projectile Warning", "Рядом летит стрела/снаряд", RN, () -> {
            if (p() == null || mc().world == null) return null;
            for (Entity e : mc().world.getEntities()) if (e instanceof PersistentProjectileEntity && e.squaredDistanceTo(p()) < 64) return "⚠ " + t("снаряд", "projectile");
            return null; }, "⚠ projectile"));
        L.add(hud("Falling", "Ты быстро падаешь", RN, () -> p() != null && p().getVelocity().y < -0.8 ? t("Падение", "Falling") : null, "Falling"));
        L.add(hud("Void Warning", "Близко к пустоте (Y < 5)", RN, () -> p() != null && p().getBlockY() < 5 ? "§c⚠ " + t("ПУСТОТА", "VOID") : null, "⚠ VOID"));
        L.add(hud("XP Level", "Твой уровень опыта", RN, () -> p() == null ? null : "§a" + p().experienceLevel + " lvl", "30 lvl"));
        L.add(hud("Saturation", "Насыщение (скрытая еда)", RN, () -> p() == null ? null : t("Насыщение ", "Saturation ") + Math.round(p().getHungerManager().getSaturationLevel()), "Saturation 5"));

        return L;
    }

    private static boolean bedNearby() {
        if (p() == null || mc().world == null) return false;
        net.minecraft.util.math.BlockPos base = p().getBlockPos();
        for (int dx = -6; dx <= 6; dx++) for (int dy = -3; dy <= 3; dy++) for (int dz = -6; dz <= 6; dz++) {
            net.minecraft.block.Block b = mc().world.getBlockState(base.add(dx, dy, dz)).getBlock();
            if (b instanceof net.minecraft.block.BedBlock) return true;
        }
        return false;
    }
}
