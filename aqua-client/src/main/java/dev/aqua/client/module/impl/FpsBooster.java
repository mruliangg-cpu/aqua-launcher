package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.BoolSetting;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
//#if MC >= 1.21.2
import net.minecraft.particle.ParticlesMode;
//#else
import net.minecraft.client.option.ParticlesMode;
//#endif
import net.minecraft.client.option.SimpleOption;

/**
 * Быстрые настройки графики для FPS. Вместе с Sodium/Lithium/EntityCulling,
 * которые ставит Aqua Launcher, даёт максимальный прирост.
 */
public class FpsBooster extends Module {
    private final BoolSetting clouds = add(new BoolSetting("Убрать облака", true));
    private final BoolSetting particles = add(new BoolSetting("Минимум частиц", true));
    private final BoolSetting shadows = add(new BoolSetting("Без теней мобов", true));
    private final BoolSetting biomeBlend = add(new BoolSetting("Без смешивания биомов", true));
    private final BoolSetting entityDistance = add(new BoolSetting("Мобы ближе (75%)", false));
    private final BoolSetting fastGraphics = add(new BoolSetting("Быстрая графика", false));

    private CloudRenderMode oClouds;
    private ParticlesMode oParticles;
    private Boolean oShadows;
    private Integer oBlend;
    private Double oDistance;
    private GraphicsMode oGraphics;
    private int timer;

    public FpsBooster() { super("FPS Booster", "Снижает тяжёлые настройки графики ради FPS", Category.PERFORMANCE); }

    @Override
    public void onTick() {
        if (timer-- > 0) return;
        timer = 20;
        var o = mc.options;
        if (oClouds == null) {
            oClouds = o.getCloudRenderMode().getValue();
            oParticles = o.getParticles().getValue();
            oShadows = o.getEntityShadows().getValue();
            oBlend = o.getBiomeBlendRadius().getValue();
            oDistance = o.getEntityDistanceScaling().getValue();
        //#if MC < 1.21.11
            oGraphics = o.getGraphicsMode().getValue();
        //#endif
        }
        apply(o.getCloudRenderMode(), clouds.isOn() ? CloudRenderMode.OFF : oClouds);
        apply(o.getParticles(), particles.isOn() ? ParticlesMode.MINIMAL : oParticles);
        apply(o.getEntityShadows(), shadows.isOn() ? Boolean.FALSE : oShadows);
        apply(o.getBiomeBlendRadius(), biomeBlend.isOn() ? Integer.valueOf(0) : oBlend);
        apply(o.getEntityDistanceScaling(), entityDistance.isOn() ? Double.valueOf(0.75) : oDistance);
        //#if MC < 1.21.11
        apply(o.getGraphicsMode(), fastGraphics.isOn() ? GraphicsMode.FAST : oGraphics);
        //#endif
    }

    private static <T> void apply(SimpleOption<T> option, T value) {
        if (value != null && !value.equals(option.getValue())) option.setValue(value);
    }

    @Override
    protected void onDisable() {
        if (oClouds == null) return;
        var o = mc.options;
        apply(o.getCloudRenderMode(), oClouds);
        apply(o.getParticles(), oParticles);
        apply(o.getEntityShadows(), oShadows);
        apply(o.getBiomeBlendRadius(), oBlend);
        apply(o.getEntityDistanceScaling(), oDistance);
        //#if MC < 1.21.11
        apply(o.getGraphicsMode(), oGraphics);
        //#endif
        oClouds = null;
        timer = 0;
    }
}
