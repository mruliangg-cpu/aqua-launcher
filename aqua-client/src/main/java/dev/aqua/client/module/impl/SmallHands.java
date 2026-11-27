package dev.aqua.client.module.impl;

import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.setting.ModeSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

/** Руки от первого лица: размер, положение (X/Y/Z) и наклон (X/Y/Z) ОТДЕЛЬНО для каждой руки, плюс готовые пресеты наклона. */
public class SmallHands extends Module {
    private static SmallHands instance;

    private final ModeSetting preset = add(new ModeSetting("Пресет наклона", "Свой", "Свой", "Читер", "Боевой", "Высокий", "Плоский"));

    // Правая рука (основная)
    private final NumberSetting sR  = add(new NumberSetting("ПР — размер", 0.75, 0.20, 2.00, 0.05, "x"));
    private final NumberSetting xR  = add(new NumberSetting("ПР — X", 0, -1.0, 1.0, 0.02, ""));
    private final NumberSetting yR  = add(new NumberSetting("ПР — Y", 0, -1.0, 1.0, 0.02, ""));
    private final NumberSetting zR  = add(new NumberSetting("ПР — Z", 0, -1.0, 1.0, 0.02, ""));
    private final NumberSetting rxR = add(new NumberSetting("ПР — наклон X", 0, -180, 180, 1, "°"));
    private final NumberSetting ryR = add(new NumberSetting("ПР — наклон Y", 0, -180, 180, 1, "°"));
    private final NumberSetting rzR = add(new NumberSetting("ПР — наклон Z", 0, -180, 180, 1, "°"));

    // Левая рука (вторая)
    private final NumberSetting sL  = add(new NumberSetting("ЛВ — размер", 0.75, 0.20, 2.00, 0.05, "x"));
    private final NumberSetting xL  = add(new NumberSetting("ЛВ — X", 0, -1.0, 1.0, 0.02, ""));
    private final NumberSetting yL  = add(new NumberSetting("ЛВ — Y", 0, -1.0, 1.0, 0.02, ""));
    private final NumberSetting zL  = add(new NumberSetting("ЛВ — Z", 0, -1.0, 1.0, 0.02, ""));
    private final NumberSetting rxL = add(new NumberSetting("ЛВ — наклон X", 0, -180, 180, 1, "°"));
    private final NumberSetting ryL = add(new NumberSetting("ЛВ — наклон Y", 0, -180, 180, 1, "°"));
    private final NumberSetting rzL = add(new NumberSetting("ЛВ — наклон Z", 0, -180, 180, 1, "°"));

    public SmallHands() {
        super("Small Hands", "Размер, положение и наклон каждой руки от первого лица + пресеты", Category.RENDER);
        instance = this;
    }

    public static boolean on() { return instance != null && instance.isEnabled(); }

    /** Применяет трансформацию к матрице руки. main=true — правая (основная), иначе левая (off-hand). */
    public static void transform(MatrixStack m, boolean main) {
        if (instance == null) return;
        float s  = main ? instance.sR.getFloat() : instance.sL.getFloat();
        float px = main ? instance.xR.getFloat() : instance.xL.getFloat();
        float py = main ? instance.yR.getFloat() : instance.yL.getFloat();
        float pz = main ? instance.zR.getFloat() : instance.zL.getFloat();
        float rx, ry, rz;
        if (instance.preset.is("Свой")) {
            rx = main ? instance.rxR.getFloat() : instance.rxL.getFloat();
            ry = main ? instance.ryR.getFloat() : instance.ryL.getFloat();
            rz = main ? instance.rzR.getFloat() : instance.rzL.getFloat();
        } else {
            float[] r = preset(instance.preset);
            rx = r[0];
            ry = r[1] * (main ? 1 : -1);  // зеркалим для левой руки
            rz = r[2] * (main ? 1 : -1);
        }
        float ax = 0.56f * (main ? 1 : -1), ay = -0.52f, az = -0.72f; // точка удержания предмета
        m.translate(px, py, pz);
        m.translate(ax, ay, az);
        if (rx != 0) m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rx));
        if (ry != 0) m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ry));
        if (rz != 0) m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rz));
        m.scale(s, s, s);
        m.translate(-ax, -ay, -az);
    }

    private static float[] preset(ModeSetting p) {
        if (p.is("Читер"))   return new float[]{-12, 0, 22};
        if (p.is("Боевой"))  return new float[]{6, 18, -8};
        if (p.is("Высокий")) return new float[]{-24, 0, 0};
        return new float[]{0, 0, 0}; // Плоский / по умолчанию
    }
}
