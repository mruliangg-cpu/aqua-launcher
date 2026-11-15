package dev.aqua.client.module.impl;

import dev.aqua.client.compat.V;
import dev.aqua.client.gui.Render2D;
import dev.aqua.client.gui.Theme;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.HudModule;
import dev.aqua.client.module.Module;
import dev.aqua.client.module.TextHudModule;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ModeSetting;
import dev.aqua.client.setting.NumberSetting;
import dev.aqua.client.util.ClickTracker;
import dev.aqua.client.util.CombatTracker;
import dev.aqua.client.util.Lang;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 50 PvP-модулей Aqua Client. Только информация, звук и визуал — как в Lunar/Badlion:
 * ничего не бьёт, не целится и не ходит за игрока.
 */
public final class PvpModules {
    private PvpModules() {}

    private static MinecraftClient mc() { return MinecraftClient.getInstance(); }
    private static ClientPlayerEntity p() { return mc().player; }
    private static String t(String ru, String en) { return Lang.t(ru, en); }
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    // =================================================================== статистика боя
    static int hits, hitsTaken, maxCombo, kills, deaths, streak;
    static double maxReach;
    static final ArrayDeque<Double> REACH = new ArrayDeque<>();
    static final Map<Integer, Long> RECENT = new HashMap<>();      // id сущности → время последнего удара
    static final Map<Integer, Float> LAST_HP = new HashMap<>();    // id → здоровье в прошлом тике
    static final Map<Integer, Integer> TARGET_HITS = new HashMap<>();
    static final List<String[]> KILL_FEED = new ArrayList<>();     // {имя, время}
    static final Map<String, Integer> POPS = new LinkedHashMap<>();
    static final Map<String, Long> POP_TIME = new HashMap<>();
    static final ArrayDeque<Integer> CPS = new ArrayDeque<>();
    static long lastCombatTime, fightStart, hitMarker, critTime, sprintTime, comboTime;
    static boolean lastCrit, lastSprint;
    static int fightHits, fightTaken, fightCombo, comboShown;
    static float fightDamage, ownHp = -1;
    static String comboText;
    static int prevHurt;
    static boolean wasDead;
    static long lastCpsSample;
    private static final List<Popup> POPUPS = new ArrayList<>();

    private record Popup(String text, int color, long start, int dx) {}

    // ссылки на модули, которые рисуют поверх экрана
    static Module damageIndicator, damageTaken, hitMarkerM, critM, sprintM, comboM, targetInd, crossHealth, fightSummary;
    static HitSound hitSound;
    static KillSound killSound;
    static KillEffect killEffect;
    static TotemPops totemPops;
    static NoTotemAnimation noTotem;

    // =================================================================== регистрация
    private static int idx;
    private static TextHudModule hud(String name, String desc, Category cat, Supplier<String> text, String preview) {
        int i = idx++;
        float x = 0.74f - (i % 3) * 0.2f, y = 0.10f + (i / 3 % 14) * 0.055f;
        return new TextHudModule(name, desc, cat, x, y, () -> { try { return p() == null ? null : text.get(); } catch (Exception e) { return null; } }, () -> preview);
    }

    public static List<Module> all() {
        List<Module> L = new ArrayList<>();
        Category C = Category.COMBAT, H = Category.HUD;

        // ---------------- информация о цели (6)
        L.add(new TargetArmor());
        L.add(hud("Target Ping", "Пинг игрока, которого ты бьёшь", C, () -> {
            if (!(CombatTracker.lastTarget instanceof PlayerEntity pl) || mc().getNetworkHandler() == null) return null;
            PlayerListEntry e = mc().getNetworkHandler().getPlayerListEntry(pl.getUuid());
            return e == null ? null : t("Пинг цели: ", "Target ping: ") + e.getLatency() + " ms"; }, "Target ping: 45 ms"));
        L.add(hud("Target Held Item", "Что у цели в руке", C, () -> {
            LivingEntity e = CombatTracker.lastTarget; if (e == null || e.getMainHandStack().isEmpty()) return null;
            return t("У цели: ", "Target holds: ") + e.getMainHandStack().getName().getString(); }, "Target holds: Netherite Sword"));
        L.add(hud("Target Hits", "Сколько раз ты ударил текущую цель", C, () -> {
            LivingEntity e = CombatTracker.lastTarget; if (e == null) return null;
            return t("Удары по цели: ", "Hits on target: ") + TARGET_HITS.getOrDefault(e.getId(), 0); }, "Hits on target: 7"));
        L.add(new ExtraModules.Alert("Target Low HP", "Цель почти без здоровья", C, () -> {
            LivingEntity e = CombatTracker.lastTarget;
            return e != null && e.isAlive() && e.getHealth() / Math.max(1, e.getMaxHealth()) < 0.3f ? String.format(t("Цель почти мертва: %.1f ❤", "Target almost dead: %.1f ❤"), e.getHealth() / 2f) : null; }, false));
        L.add(new ExtraModules.Alert("Target Armor Break", "У цели ломается броня", C, () -> {
            LivingEntity e = CombatTracker.lastTarget; if (e == null) return null;
            for (EquipmentSlot s : ARMOR) { ItemStack st = e.getEquippedStack(s); int pc = pct(st); if (pc >= 0 && pc < 15) return t("Броня цели ломается: ", "Target armor breaking: ") + st.getName().getString() + " " + pc + "%"; }
            return null; }, false));

        // ---------------- статистика (14)
        L.add(hud("Hit Counter", "Сколько ударов ты нанёс за игру", C, () -> t("Удары: ", "Hits: ") + hits, "Hits: 152"));
        L.add(hud("Hits Taken", "Сколько ударов получил за игру", C, () -> t("Получено: ", "Taken: ") + hitsTaken, "Taken: 40"));
        L.add(hud("Accuracy", "Процент кликов, которые попали", C, () -> { long c = Math.max(hits, ClickTracker.totalLeft); return c == 0 ? null : t("Точность: ", "Accuracy: ") + hits * 100 / c + "%"; }, "Accuracy: 64%"));
        L.add(hud("Max Combo", "Лучшее комбо за игру", C, () -> t("Лучшее комбо: ", "Best combo: ") + maxCombo, "Best combo: 9"));
        L.add(hud("Average Reach", "Средняя дистанция удара (последние 20)", C, () -> { if (REACH.isEmpty()) return null; double s = 0; for (double d : REACH) s += d; return String.format(t("Ср. дистанция: %.2f", "Avg reach: %.2f"), s / REACH.size()); }, "Avg reach: 2.87"));
        L.add(hud("Max Reach", "Самый дальний удар", C, () -> maxReach <= 0 ? null : String.format(t("Макс. дистанция: %.2f", "Max reach: %.2f"), maxReach), "Max reach: 3.00"));
        L.add(hud("Kill Counter", "Сколько убийств за игру", C, () -> t("Убийства: ", "Kills: ") + kills, "Kills: 12"));
        L.add(hud("Kill Streak", "Убийства подряд без смерти", C, () -> t("Серия: ", "Streak: ") + streak, "Streak: 4"));
        L.add(hud("KD Ratio", "Соотношение убийств и смертей", C, () -> String.format("K/D %.2f", kills / (double) Math.max(1, deaths)), "K/D 2.40"));
        L.add(hud("Fight Timer", "Сколько длится текущий бой", C, () -> inFight() ? t("Бой ", "Fight ") + time(System.currentTimeMillis() - fightStart) : null, "Fight 0:18"));
        L.add(new CombatTag());
        L.add(fightSummary = new Simple("Fight Summary", "Итог боя в чате: удары, урон, комбо, время"));
        L.add(new KillFeed());
        L.add(totemPops = new TotemPops());

        // ---------------- отклик на удар (13)
        L.add(damageIndicator = new Simple("Damage Indicator", "Показывает урон, который ты нанёс, у прицела"));
        L.add(damageTaken = new Simple("Damage Taken Popup", "Показывает урон, который ты получил"));
        L.add(hitMarkerM = new Simple("Hit Marker", "Крестик у прицела, когда удар попал"));
        L.add(critM = new Simple("Crit Indicator", "Надпись «КРИТ!», когда удар критический"));
        L.add(sprintM = new Simple("Sprint Hit Indicator", "Был ли удар с разбега (для W-tap)"));
        L.add(comboM = new Simple("Combo Announcer", "Большая надпись на 3, 5, 10 комбо"));
        L.add(targetInd = new Simple("Target Indicator", "Кольцо у прицела, когда наводишься на игрока"));
        L.add(crossHealth = new Simple("Crosshair Health", "Полоска твоего здоровья под прицелом"));
        L.add(hitSound = new HitSound());
        L.add(killSound = new KillSound());
        L.add(killEffect = new KillEffect());
        L.add(noTotem = new NoTotemAnimation());
        L.add(new CpsGraph());

        // ---------------- предметы (14)
        L.add(hud("Potion Counter", "Сколько зелий в инвентаре", C, () -> count(t("Зелья", "Potions"), Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION), "Potions: 8"));
        L.add(hud("Soup Counter", "Сколько супа (soup PvP)", C, () -> count(t("Суп", "Soup"), Items.MUSHROOM_STEW, Items.RABBIT_STEW, Items.BEETROOT_SOUP), "Soup: 32"));
        L.add(hud("Crystal Counter", "Сколько кристаллов Края", C, () -> count(t("Кристаллы", "Crystals"), Items.END_CRYSTAL), "Crystals: 64"));
        L.add(hud("Obsidian Counter", "Сколько обсидиана", C, () -> count(t("Обсидиан", "Obsidian"), Items.OBSIDIAN), "Obsidian: 64"));
        L.add(hud("XP Bottle Counter", "Сколько пузырьков опыта", C, () -> count(t("Пузырьки", "XP bottles"), Items.EXPERIENCE_BOTTLE), "XP bottles: 128"));
        L.add(hud("Firework Counter", "Сколько ракет для элитр", C, () -> count(t("Ракеты", "Rockets"), Items.FIREWORK_ROCKET), "Rockets: 32"));
        L.add(hud("Cobweb Counter", "Сколько паутины", C, () -> count(t("Паутина", "Cobwebs"), Items.COBWEB), "Cobwebs: 16"));
        L.add(hud("Water Bucket Counter", "Сколько вёдер воды (для MLG)", C, () -> count(t("Вёдра воды", "Water buckets"), Items.WATER_BUCKET), "Water buckets: 2"));
        L.add(hud("Notch Apple Counter", "Сколько зачарованных золотых яблок", C, () -> count(t("Чарки", "God apples"), Items.ENCHANTED_GOLDEN_APPLE), "God apples: 3"));
        L.add(hud("Food Counter", "Сколько всей еды", C, () -> { int n = 0; var inv = p().getInventory(); for (int i = 0; i < inv.size(); i++) if (isFood(inv.getStack(i))) n += inv.getStack(i).getCount(); return n == 0 ? null : t("Еда: ", "Food: ") + n; }, "Food: 48"));
        L.add(hud("Elytra HUD", "Прочность элитр и количество ракет", C, () -> {
            ItemStack ch = p().getEquippedStack(EquipmentSlot.CHEST); if (!ch.isOf(Items.ELYTRA)) return null;
            return t("Элитры ", "Elytra ") + pct(ch) + "% · " + t("ракеты ", "rockets ") + p().getInventory().count(Items.FIREWORK_ROCKET); }, "Elytra 87% · rockets 32"));
        L.add(hud("Bow Charge", "Натяжение лука и заряд арбалета", C, () -> {
            if (p().isUsingItem() && p().getActiveItem().isOf(Items.BOW)) { int pc = (int) (BowItem.getPullProgress(p().getItemUseTime()) * 100); return t("Лук ", "Bow ") + pc + "%" + (pc >= 100 ? " ✔" : ""); }
            for (ItemStack s : new ItemStack[]{p().getMainHandStack(), p().getOffHandStack()}) if (s.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(s)) return t("Арбалет заряжен", "Crossbow loaded");
            return null; }, "Bow 100% ✔"));
        L.add(hud("Shield Status", "Прочность щита и сколько он отключён", C, () -> {
            ItemStack s = p().getOffHandStack().isOf(Items.SHIELD) ? p().getOffHandStack() : p().getMainHandStack().isOf(Items.SHIELD) ? p().getMainHandStack() : null;
            if (s == null) return null;
            float cd = V.cooldown(p(), Items.SHIELD);
            return t("Щит ", "Shield ") + pct(s) + "%" + (cd > 0 ? String.format(t(" · отключён %.1f с", " · disabled %.1fs"), cd * 5) : p().isBlocking() ? t(" · блок", " · blocking") : ""); }, "Shield 90% · blocking"));
        L.add(new InventoryHud());

        // ---------------- эффекты и опасность (3)
        L.add(hud("PvP Buffs", "Сила, скорость, регенерация и др. с таймерами", C, PvpModules::buffs, "Str 1:23 · Spd 2:10"));
        L.add(new ExtraModules.Alert("Debuff Alert", "Тебя отравили, замедлили или ослабили", C, () -> {
            StatusEffectInstance i = p().getStatusEffect(StatusEffects.POISON);
            if (i == null) i = p().getStatusEffect(StatusEffects.WITHER);
            if (i == null) i = p().getStatusEffect(StatusEffects.WEAKNESS);
            if (i == null) i = p().getStatusEffect(StatusEffects.SLOWNESS);
            if (i == null) i = p().getStatusEffect(StatusEffects.BLINDNESS);
            return i == null ? null : t("На тебе: ", "Debuff: ") + V.effectName(i) + " " + time(i.getDuration() * 50L); }, true));
        L.add(hud("Fall Damage Preview", "Сколько урона будет от падения", H, () -> {
            double fd = (double) p().fallDistance;
            if (p().isOnGround() || fd <= 3.5 || p().getAbilities().flying || p().isTouchingWater()) return null;
            return String.format(t("Падение: −%.1f ❤", "Fall: −%.1f ❤"), Math.ceil(fd - 3) / 2.0); }, "Fall: −4.0 ❤"));
        return L;
    }

    // =================================================================== события
    /** Вызывается при твоём ударе (до того, как игра сбросит перезарядку). */
    public static void onAttack(Entity target) {
        ClientPlayerEntity pl = p();
        if (pl == null || !(target instanceof LivingEntity le) || !le.isAlive()) return;
        long now = System.currentTimeMillis();
        lastCrit = (double) pl.fallDistance > 0 && !pl.isOnGround() && !pl.isClimbing() && !pl.isTouchingWater()
                && !pl.hasVehicle() && !pl.hasStatusEffect(StatusEffects.BLINDNESS) && pl.getAttackCooldownProgress(0.5f) > 0.9f;
        lastSprint = pl.isSprinting();
        hits++;
        TARGET_HITS.merge(le.getId(), 1, Integer::sum);
        RECENT.put(le.getId(), now);
        LAST_HP.putIfAbsent(le.getId(), le.getHealth());
        hitMarker = now;
        if (lastCrit) critTime = now;
        sprintTime = now;
        if (CombatTracker.lastReach > 0) {
            REACH.addLast(CombatTracker.lastReach);
            if (REACH.size() > 20) REACH.removeFirst();
            maxReach = Math.max(maxReach, CombatTracker.lastReach);
        }
        maxCombo = Math.max(maxCombo, CombatTracker.combo);
        startFight(now);
        fightHits++;
        fightCombo = Math.max(fightCombo, CombatTracker.combo);
        int c = CombatTracker.combo;
        if ((c == 3 || c == 5 || c == 10 || c == 15 || c == 20 || (c > 20 && c % 10 == 0)) && c != comboShown) {
            comboShown = c; comboText = c + " COMBO!"; comboTime = now;
            if (comboM.isEnabled()) play(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.8f + Math.min(c, 20) * 0.05f, 0.5f);
        }
        if (hitSound.isEnabled()) hitSound.play();
    }

    /** Статусы сущностей с сервера (35 — сработал тотем, 3 — смерть). */
    public static void onEntityStatus(Entity e, byte status) {
        if (e == null) return;
        String name = e.getName().getString();
        if (status == 35) {
            int n = POPS.merge(name, 1, Integer::sum);
            POP_TIME.put(name, System.currentTimeMillis());
            if (totemPops != null && totemPops.isEnabled() && totemPops.chat.isOn() && e != p())
                mc().inGameHud.getChatHud().addMessage(Text.literal("§b[Aqua] §f" + name + t(" §7потерял тотем §e#", " §7popped a totem §e#") + n));
        } else if (status == 3 && e instanceof PlayerEntity) {
            POPS.remove(name);
        }
    }

    public static void tick() {
        ClientPlayerEntity pl = p();
        if (pl == null || mc().world == null) { RECENT.clear(); LAST_HP.clear(); ownHp = -1; return; }
        long now = System.currentTimeMillis();

        // убийства и урон по целям
        for (Iterator<Map.Entry<Integer, Long>> it = RECENT.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            Entity e = mc().world.getEntityById(en.getKey());
            if (!(e instanceof LivingEntity le) || now - en.getValue() > 6000) { it.remove(); LAST_HP.remove(en.getKey()); continue; }
            float hp = le.getHealth(), before = LAST_HP.getOrDefault(le.getId(), hp);
            if (hp < before - 0.01f) {
                float dmg = before - hp;
                fightDamage += dmg;
                if (damageIndicator.isEnabled()) POPUPS.add(new Popup(String.format("-%.1f ❤", dmg / 2f), lastCrit ? 0xFFFFC23D : 0xFF7CF0FF, now, 14));
            }
            LAST_HP.put(le.getId(), hp);
            if ((le.isDead() || hp <= 0) && now - en.getValue() < 4000) {
                it.remove(); LAST_HP.remove(en.getKey());
                onKill(le);
            }
        }

        // урон по тебе
        float hp = pl.getHealth();
        if (ownHp >= 0 && hp < ownHp - 0.01f && damageTaken.isEnabled())
            POPUPS.add(new Popup(String.format("-%.1f ❤", (ownHp - hp) / 2f), 0xFFFF5566, now, -44));
        ownHp = hp;
        if (pl.hurtTime > prevHurt) { hitsTaken++; startFight(now); fightTaken++; }
        prevHurt = pl.hurtTime;

        // твоя смерть
        boolean dead = pl.isDead() || hp <= 0;
        if (dead && !wasDead) { deaths++; streak = 0; endFight(); }
        wasDead = dead;

        // бой закончился
        if (fightStart > 0 && now - lastCombatTime > 8000) endFight();
        if (CombatTracker.combo < 3) comboShown = 0;

        // CPS для графика
        if (now - lastCpsSample >= 100) { lastCpsSample = now; CPS.addLast(ClickTracker.left()); if (CPS.size() > 60) CPS.removeFirst(); }
        KILL_FEED.removeIf(k -> now - Long.parseLong(k[1]) > 30000);
        POP_TIME.entrySet().removeIf(en -> { if (now - en.getValue() > 90000) { POPS.remove(en.getKey()); return true; } return false; });
    }

    private static void onKill(LivingEntity le) {
        kills++; streak++;
        KILL_FEED.add(0, new String[]{le.getName().getString(), String.valueOf(System.currentTimeMillis())});
        if (KILL_FEED.size() > 6) KILL_FEED.remove(KILL_FEED.size() - 1);
        if (killSound.isEnabled()) killSound.play();
        if (killEffect.isEnabled()) killEffect.spawn(le);
    }

    private static void startFight(long now) { if (fightStart == 0) { fightStart = now; fightHits = 0; fightTaken = 0; fightCombo = 0; fightDamage = 0; } lastCombatTime = now; }
    private static boolean inFight() { return fightStart > 0 && System.currentTimeMillis() - lastCombatTime < 8000; }
    private static void endFight() {
        if (fightStart == 0) return;
        if (fightSummary.isEnabled() && p() != null && (fightHits > 0 || fightTaken > 0))
            mc().inGameHud.getChatHud().addMessage(Text.literal(String.format(t(
                    "§b[Aqua] §fИтог боя: §e%d§f ударов, получено §c%d§f, комбо §e%d§f, урон §e%.1f❤§f, время §e%s",
                    "§b[Aqua] §fFight: §e%d§f hits, taken §c%d§f, combo §e%d§f, damage §e%.1f❤§f, time §e%s"),
                    fightHits, fightTaken, fightCombo, fightDamage / 2f, time(lastCombatTime - fightStart))));
        fightStart = 0;
    }

    // =================================================================== рисование поверх экрана
    public static void render(DrawContext ctx) {
        if (p() == null) return;
        long now = System.currentTimeMillis();
        int cx = ctx.getScaledWindowWidth() / 2, cy = ctx.getScaledWindowHeight() / 2;
        boolean noScreen = mc().currentScreen == null;

        if (hitMarkerM.isEnabled() && now - hitMarker < 250 && noScreen) {
            int a = (int) (255 * (1 - (now - hitMarker) / 250f)), col = (Math.max(a, 16) << 24) | (lastCrit ? 0xFFC23D : 0xFFFFFF);
            for (int i = 3; i <= 6; i++) {
                Render2D.rect(ctx, cx - i, cy - i, 1, 1, col); Render2D.rect(ctx, cx + i, cy - i, 1, 1, col);
                Render2D.rect(ctx, cx - i, cy + i, 1, 1, col); Render2D.rect(ctx, cx + i, cy + i, 1, 1, col);
            }
        }
        if (targetInd.isEnabled() && noScreen && mc().targetedEntity instanceof PlayerEntity)
            Render2D.roundOutline(ctx, cx - 8, cy - 8, 17, 17, 8, 0xC0FF5577);
        if (crossHealth.isEnabled() && noScreen) {
            float f = Math.max(0, Math.min(1, p().getHealth() / Math.max(1, p().getMaxHealth())));
            Render2D.rect(ctx, cx - 15, cy + 10, 30, 2, 0x80000000);
            Render2D.rect(ctx, cx - 15, cy + 10, 30 * f, 2, f > 0.5f ? 0xFF5CFF8A : f > 0.25f ? 0xFFFFD36B : 0xFFFF5566);
        }
        if (critM.isEnabled() && now - critTime < 700) Render2D.centered(ctx, t("КРИТ!", "CRIT!"), cx, cy - 22, 0xFFFFC23D);
        if (sprintM.isEnabled() && now - sprintTime < 700)
            Render2D.centered(ctx, lastSprint ? t("Удар с разбега", "Sprint hit") : t("Без разбега", "No sprint"), cx, cy + 16, lastSprint ? 0xFF5CFF8A : 0xFFAAAAAA);
        if (comboM.isEnabled() && comboText != null && now - comboTime < 1200) {
            float s = 1.6f + Math.max(0, 0.4f - (now - comboTime) / 500f);
            String txt = comboText;
            Render2D.textScaled(ctx, txt, cx - Render2D.width(txt) * s / 2f, cy - 60, s, Theme.ACCENT, true);
        }
        for (Iterator<Popup> it = POPUPS.iterator(); it.hasNext(); ) {
            Popup pp = it.next();
            float age = (now - pp.start) / 900f;
            if (age >= 1) { it.remove(); continue; }
            int a = Math.max(16, (int) (255 * (1 - age)));
            ctx.drawText(mc().textRenderer, pp.text, cx + pp.dx, (int) (cy - 6 - age * 20), (a << 24) | (pp.color & 0xFFFFFF), true);
        }
        if (POPUPS.size() > 20) POPUPS.subList(0, POPUPS.size() - 20).clear();
    }

    // =================================================================== помощники
    static int pct(ItemStack s) {
        if (s == null || s.isEmpty() || !s.isDamageable()) return -1;
        return (s.getMaxDamage() - s.getDamage()) * 100 / s.getMaxDamage();
    }
    static int pctColor(int pc) { return pc < 0 ? 0xFFBBBBBB : pc > 60 ? 0xFF5CFF8A : pc > 25 ? 0xFFFFD36B : 0xFFFF5566; }
    private static String count(String label, Item... items) { int n = 0; for (Item i : items) n += p().getInventory().count(i); return n == 0 ? null : label + ": " + n; }
    private static boolean isFood(ItemStack s) {
        if (s.isEmpty()) return false;
        //#if MC >= 1.20.5
        return s.get(net.minecraft.component.DataComponentTypes.FOOD) != null;
        //#else
        return s.isFood();
        //#endif
    }
    static String time(long ms) { if (ms < 0) return "∞"; long s = ms / 1000; return s / 60 + ":" + String.format("%02d", s % 60); }
    private static String buffs() {
        StringBuilder b = new StringBuilder();
        buff(b, p().getStatusEffect(StatusEffects.STRENGTH), t("Сила", "Str"));
        buff(b, p().getStatusEffect(StatusEffects.SPEED), t("Скор.", "Spd"));
        buff(b, p().getStatusEffect(StatusEffects.REGENERATION), t("Реген", "Regen"));
        buff(b, p().getStatusEffect(StatusEffects.FIRE_RESISTANCE), t("Огнест.", "FireRes"));
        buff(b, p().getStatusEffect(StatusEffects.RESISTANCE), t("Защита", "Res"));
        buff(b, p().getStatusEffect(StatusEffects.ABSORPTION), t("Поглощ.", "Abs"));
        return b.length() == 0 ? null : b.toString();
    }
    private static void buff(StringBuilder b, StatusEffectInstance i, String name) {
        if (i == null) return;
        if (b.length() > 0) b.append(" · ");
        b.append(name).append(i.getAmplifier() > 0 ? " " + (i.getAmplifier() + 1) : "").append(' ').append(i.getDuration() < 0 ? "∞" : time(i.getDuration() * 50L));
    }
    static void play(SoundEvent ev, float pitch, float volume) {
        try {
            //#if MC >= 1.21.11
            mc().getSoundManager().play(PositionedSoundInstance.ui(ev, pitch, volume));
            //#else
            mc().getSoundManager().play(PositionedSoundInstance.master(ev, pitch, volume));
            //#endif
        } catch (Exception ignored) {}
    }
    static SoundEvent sound(String mode) {
        return switch (mode) {
            case "Попадание" -> SoundEvents.ENTITY_ARROW_HIT_PLAYER;
            case "Крит" -> SoundEvents.ENTITY_PLAYER_ATTACK_CRIT;
            case "Аметист" -> SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME;
            case "Уровень" -> SoundEvents.ENTITY_PLAYER_LEVELUP;
            default -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
        };
    }
    private static final String[] SOUNDS = {"Опыт", "Попадание", "Крит", "Аметист", "Уровень"};

    // =================================================================== модули
    /** Модуль без своей логики — его работу делает render()/tick() выше. */
    public static final class Simple extends Module {
        Simple(String name, String desc) { super(name, desc, Category.COMBAT); }
    }

    public static final class HitSound extends Module {
        final ModeSetting mode = add(new ModeSetting("Звук", "Опыт", SOUNDS));
        final NumberSetting pitch = add(new NumberSetting("Высота", 1.2, 0.5, 2.0, 0.1));
        final NumberSetting volume = add(new NumberSetting("Громкость", 60, 5, 100, 5, "%"));
        HitSound() { super("Hit Sound", "Звук, когда твой удар попал", Category.COMBAT); }
        void play() { PvpModules.play(sound(mode.get()), pitch.getFloat(), volume.getFloat() / 100f); }
    }

    public static final class KillSound extends Module {
        final ModeSetting mode = add(new ModeSetting("Звук", "Уровень", SOUNDS));
        final NumberSetting volume = add(new NumberSetting("Громкость", 80, 5, 100, 5, "%"));
        KillSound() { super("Kill Sound", "Звук, когда ты убил противника", Category.COMBAT); }
        void play() { PvpModules.play(sound(mode.get()), 1.0f, volume.getFloat() / 100f); }
    }

    public static final class KillEffect extends Module {
        final ModeSetting mode = add(new ModeSetting("Эффект", "Тотем", "Тотем", "Сердца", "Искры", "Души"));
        KillEffect() { super("Kill Effect", "Красивые частицы там, где ты убил противника", Category.COMBAT); }
        void spawn(LivingEntity e) {
            ParticleEffect pe = switch (mode.get()) {
                case "Сердца" -> ParticleTypes.HEART;
                case "Искры" -> ParticleTypes.FIREWORK;
                case "Души" -> ParticleTypes.SOUL;
                default -> ParticleTypes.TOTEM_OF_UNDYING;
            };
            var pos = V.pos(e);
            var r = e.getRandom();
            for (int i = 0; i < 40; i++)
                V.particle(pe, pos.x + (r.nextDouble() - 0.5) * e.getWidth(), pos.y + r.nextDouble() * e.getHeight(), pos.z + (r.nextDouble() - 0.5) * e.getWidth(),
                        (r.nextDouble() - 0.5) * 0.4, r.nextDouble() * 0.4, (r.nextDouble() - 0.5) * 0.4);
        }
    }

    public static final class NoTotemAnimation extends Module {
        NoTotemAnimation() { super("No Totem Animation", "Убирает большую анимацию тотема на весь экран", Category.COMBAT); }
        public static boolean hide(ItemStack s) { return noTotem != null && noTotem.isEnabled() && s != null && s.isOf(Items.TOTEM_OF_UNDYING); }
    }

    /** Броня цели: иконки и проценты прочности. */
    public static final class TargetArmor extends HudModule {
        TargetArmor() { super("Target Armor", "Броня цели и сколько процентов у каждой вещи", Category.COMBAT, 0.55f, 0.66f); }
        @Override protected void draw(DrawContext ctx, boolean editor) {
            LivingEntity e = CombatTracker.lastTarget;
            if (e == null && !editor) { width = 0; height = 0; return; }
            ItemStack[] st = new ItemStack[6];
            if (e != null) { for (int i = 0; i < 4; i++) st[i] = e.getEquippedStack(ARMOR[i]); st[4] = e.getMainHandStack(); st[5] = e.getOffHandStack(); }
            else { st[0] = new ItemStack(Items.DIAMOND_HELMET); st[1] = new ItemStack(Items.NETHERITE_CHESTPLATE); st[2] = new ItemStack(Items.DIAMOND_LEGGINGS); st[3] = new ItemStack(Items.IRON_BOOTS); st[4] = new ItemStack(Items.DIAMOND_SWORD); st[5] = new ItemStack(Items.TOTEM_OF_UNDYING); }
            width = 6 * 24 + 6; height = 34;
            drawBackground(ctx, width, height);
            int cur = 0, max = 0;
            for (int i = 0; i < 6; i++) {
                int x = 4 + i * 24;
                if (st[i] == null || st[i].isEmpty()) { Render2D.roundRect(ctx, x + 2, 4, 14, 14, 3, 0x40FFFFFF); continue; }
                ctx.drawItem(st[i], x + 1, 3);
                int pc = pct(st[i]);
                if (i < 4 && pc >= 0) { cur += st[i].getMaxDamage() - st[i].getDamage(); max += st[i].getMaxDamage(); }
                String s = pc < 0 ? (st[i].getCount() > 1 ? "×" + st[i].getCount() : "") : pc + "%";
                Render2D.textScaled(ctx, s, x + 9 - Render2D.width(s) * 0.3f, 22, 0.6f, pctColor(pc), false);
            }
            if (max > 0) Render2D.textScaled(ctx, t("броня ", "armor ") + cur * 100 / max + "%", 4, 28.5f, 0.5f, 0xFFAAAAAA, false);
        }
    }

    /** Счётчик боя (combat tag): обратный отсчёт после последнего удара. */
    public static final class CombatTag extends HudModule {
        final NumberSetting secs = add(new NumberSetting("Секунд", 15, 5, 60, 1, " с"));
        CombatTag() { super("Combat Tag Timer", "Сколько ещё ты «в бою» (нельзя выходить)", Category.COMBAT, 0.45f, 0.20f); }
        @Override protected void draw(DrawContext ctx, boolean editor) {
            long left = lastCombatTime == 0 ? -1 : (long) (secs.get() * 1000) - (System.currentTimeMillis() - lastCombatTime);
            if (left <= 0 && !editor) { width = 0; height = 0; return; }
            String s = "⚔ " + t("В бою ", "In combat ") + String.format("%.1f", Math.max(0, left) / 1000f) + t(" с", "s");
            if (left <= 0) s = "⚔ " + t("В бою 12.0 с", "In combat 12.0s");
            width = Render2D.width(s) + 10; height = 16;
            drawBackground(ctx, width, height);
            text(ctx, s, 5, 4, 0xFFFF7A8A);
        }
    }

    public static final class KillFeed extends HudModule {
        KillFeed() { super("Kill Feed", "Список твоих последних убийств", Category.COMBAT, 0.80f, 0.40f); }
        @Override protected void draw(DrawContext ctx, boolean editor) {
            List<String> names = new ArrayList<>();
            for (String[] k : KILL_FEED) names.add(k[0]);
            if (names.isEmpty()) { if (!editor) { width = 0; height = 0; return; } names.add("Steve"); names.add("Zombie"); }
            width = 70; for (String n : names) width = Math.max(width, Render2D.width("⚔ " + n) + 10);
            height = names.size() * 11 + 6;
            drawBackground(ctx, width, height);
            for (int i = 0; i < names.size(); i++) text(ctx, "§c⚔ §f" + names.get(i), 5, 4 + i * 11);
        }
    }

    public static final class TotemPops extends HudModule {
        final BoolSetting chat = add(new BoolSetting("Сообщения в чат", true));
        TotemPops() { super("Totem Pop Counter", "Сколько тотемов потратил каждый игрок рядом", Category.COMBAT, 0.80f, 0.55f); }
        @Override protected void draw(DrawContext ctx, boolean editor) {
            List<String> rows = new ArrayList<>();
            for (var e : POPS.entrySet()) rows.add(e.getKey() + " §e" + e.getValue());
            if (rows.isEmpty()) { if (!editor) { width = 0; height = 0; return; } rows.add("Steve §e3"); }
            width = 80; for (String r : rows) width = Math.max(width, Render2D.width("Тотемы " + r) + 10);
            height = rows.size() * 11 + 17;
            drawBackground(ctx, width, height);
            text(ctx, t("Тотемы", "Totem pops"), 5, 4, Theme.ACCENT);
            for (int i = 0; i < rows.size() && i < 8; i++) text(ctx, rows.get(i), 5, 15 + i * 11);
        }
    }

    public static final class InventoryHud extends HudModule {
        InventoryHud() { super("Inventory HUD", "Показывает весь инвентарь на экране", Category.COMBAT, 0.40f, 0.82f); }
        @Override protected void draw(DrawContext ctx, boolean editor) {
            width = 9 * 18 + 6; height = 3 * 18 + 6;
            drawBackground(ctx, width, height);
            if (p() == null) return;
            for (int i = 0; i < 27; i++) {
                ItemStack s = p().getInventory().getStack(9 + i);
                int x = 3 + i % 9 * 18, y = 3 + i / 9 * 18;
                Render2D.rect(ctx, x, y, 16, 16, 0x30FFFFFF);
                if (s.isEmpty()) continue;
                ctx.drawItem(s, x, y);
                V.itemOverlay(ctx, mc.textRenderer, s, x, y);
            }
        }
    }

    public static final class CpsGraph extends HudModule {
        CpsGraph() { super("CPS Graph", "График кликов в секунду за 6 секунд", Category.COMBAT, 0.02f, 0.86f); }
        @Override protected void draw(DrawContext ctx, boolean editor) {
            width = 128; height = 44;
            drawBackground(ctx, width, height);
            int max = 10; for (int v : CPS) max = Math.max(max, v);
            int i = 0;
            for (int v : CPS) { int h = v * 28 / max; Render2D.rect(ctx, 4 + i * 2, 40 - h, 2, h, Theme.ACCENT); i++; }
            text(ctx, "CPS " + ClickTracker.left(), 4, 3);
        }
    }
}
