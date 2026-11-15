package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ModeSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;

/** Больше частиц и звук при ударе — только у вас на клиенте. */
public class HitParticles extends Module {
    private final ModeSetting type = add(new ModeSetting("Частицы", "Крит + магия", "Крит", "Магия", "Крит + магия", "Портал"));
    private final NumberSetting multiplier = add(new NumberSetting("Множитель", 2, 1, 6, 1, "x"));
    private final BoolSetting sound = add(new BoolSetting("Звук попадания", false));

    public HitParticles() { super("Hit Particles", "Дополнительные частицы при ударе", Category.COMBAT); }

    public void onAttack(Entity target) {
        if (!isEnabled() || mc.particleManager == null) return;
        for (int i = 0; i < multiplier.getInt(); i++) {
            if (type.is("Крит") || type.is("Крит + магия")) mc.particleManager.addEmitter(target, ParticleTypes.CRIT);
            if (type.is("Магия") || type.is("Крит + магия")) mc.particleManager.addEmitter(target, ParticleTypes.ENCHANTED_HIT);
            if (type.is("Портал") && mc.world != null) {
                for (int j = 0; j < 8; j++) {
                    double x = target.getX() + (Math.random() - 0.5) * target.getWidth() * 1.5;
                    double y = target.getY() + Math.random() * target.getHeight();
                    double z = target.getZ() + (Math.random() - 0.5) * target.getWidth() * 1.5;
                    dev.aqua.client.compat.V.particle(ParticleTypes.PORTAL, x, y, z, (Math.random() - 0.5) * 2, -Math.random(), (Math.random() - 0.5) * 2);
                }
            }
        }
        if (sound.isOn() && mc.player != null) mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 1.6f);
    }
}
