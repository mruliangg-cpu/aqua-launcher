package dev.aqua.client.module.impl;

import dev.aqua.client.compat.V;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.ModeSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;

/** Красивые частицы, когда ломаешь блок. Чисто визуально, на твоём экране. */
public class BreakingParticles extends Module {
    private static BreakingParticles instance;
    private final ModeSetting type = add(new ModeSetting("Частицы", "Искры", "Искры", "Пламя", "Облако", "Сердца", "Магия", "Души", "Счастье"));
    private final NumberSetting count = add(new NumberSetting("Количество", 12, 3, 40, 1));
    private final NumberSetting speed = add(new NumberSetting("Разлёт", 30, 5, 100, 5, "%"));

    public BreakingParticles() {
        super("Breaking Particles", "Частицы при разрушении блока", Category.RENDER);
        instance = this;
    }

    public static void onBreak(BlockPos pos) {
        if (instance == null || !instance.isEnabled() || pos == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        ParticleEffect pe = switch (instance.type.get()) {
            case "Пламя" -> ParticleTypes.FLAME;
            case "Облако" -> ParticleTypes.CLOUD;
            case "Сердца" -> ParticleTypes.HEART;
            case "Магия" -> ParticleTypes.WITCH;
            case "Души" -> ParticleTypes.SOUL;
            case "Счастье" -> ParticleTypes.HAPPY_VILLAGER;
            default -> ParticleTypes.CRIT;
        };
        double sp = instance.speed.get() / 100.0;
        var r = mc.world.random;
        for (int i = 0; i < instance.count.getInt(); i++)
            V.particle(pe, pos.getX() + r.nextDouble(), pos.getY() + r.nextDouble(), pos.getZ() + r.nextDouble(),
                    (r.nextDouble() - 0.5) * sp, r.nextDouble() * sp, (r.nextDouble() - 0.5) * sp);
    }
}
