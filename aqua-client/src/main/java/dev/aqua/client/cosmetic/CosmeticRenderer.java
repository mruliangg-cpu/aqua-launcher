package dev.aqua.client.cosmetic;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

/** Рисует 3D-косметику из каталога: группы кубиков + анимации. */
public final class CosmeticRenderer {
    private CosmeticRenderer() {}

    public static void draw(CosmeticRegistry.Item item, Voxel v, float t, boolean flying) {
        if (item == null || item.groups().isEmpty()) return;
        MatrixStack m = v.matrices();
        float flap = flying ? (float) Math.sin(t * 10) * 35 : (float) Math.sin(t * 2.5f) * 12;
        for (CosmeticRegistry.Group g : item.groups()) {
            m.push();
            m.translate(g.ox() / 16f, g.oy() / 16f, g.oz() / 16f);
            for (String a : g.anims()) {
                switch (a) {
                    case "spin" -> m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 40));
                    case "spinFast" -> m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 90));
                    case "propeller" -> m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 720));
                    case "bobHalo" -> m.translate(0, (float) Math.sin(t * 2) * 0.6f / 16f, 0);
                    case "bob" -> m.translate(0, (float) Math.sin(t * 2.2f) * 1.2f / 16f, 0);
                    case "sway" -> m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) Math.sin(t * 0.8f) * 35));
                    case "wiggle" -> m.translate((float) Math.sin(t * 8) * 0.8f / 16f, 0, 0);
                    case "buzz" -> m.translate(0, -(float) Math.abs(Math.sin(t * 30)) * 0.8f / 16f, 0);
                    case "flicker" -> m.scale(1, 0.6f + (float) Math.abs(Math.sin(t * 20)) * 0.8f, 1);
                    case "flapL" -> m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(18 + flap));
                    case "flapR" -> m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-18 - flap));
                    default -> {}
                }
            }
            for (CosmeticRegistry.Box b : g.boxes()) v.box(b.x1(), b.y1(), b.z1(), b.x2(), b.y2(), b.z2(), b.color(), b.glow(), b.flat());
            if (g.sprite() != null) v.sprite(g.sprite());
            m.pop();
        }
    }
}
