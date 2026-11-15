package dev.aqua.client.module.impl;

import dev.aqua.client.cosmetic.SkinMesh;
import dev.aqua.client.cosmetic.Voxel;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.Module;
import dev.aqua.client.module.ModuleManager;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.PlayerEntityModel;

import java.util.List;

/** 3D Skins: второй слой скина (шапка, куртка, рукава, штаны) становится объёмным у всех игроков рядом. */
public class Skin3D extends Module {
    private static Skin3D instance;
    public static final ThreadLocal<Boolean> FIRST_PERSON = ThreadLocal.withInitial(() -> false);

    private final NumberSetting distance = add(new NumberSetting("Дальность", 16, 4, 48, 2, " бл."));
    private final NumberSetting thickness = add(new NumberSetting("Толщина", 1.0, 0.5, 1.5, 0.1));
    private final BoolSetting head = add(new BoolSetting("Голова", true));
    private final BoolSetting body = add(new BoolSetting("Тело", true));
    private final BoolSetting arms = add(new BoolSetting("Руки", true));
    private final BoolSetting legs = add(new BoolSetting("Ноги", true));

    public Skin3D() {
        super("3D Skins", "Делает второй слой скина и одежду объёмными (3D) у всех игроков", Category.COSMETIC);
        instance = this;
    }

    @Override
    protected void onDisable() { SkinMesh.clear(); }

    private static Skin3D on() { return instance != null && instance.isEnabled() ? instance : null; }

    private static SkinMesh.Mesh mesh(AbstractClientPlayerEntity p) {
        Skin3D s = on();
        if (s == null || FIRST_PERSON.get() || p.isInvisible()) return null;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.gameRenderer == null || mc.gameRenderer.getCamera() == null) return null;
        double dist = mc.gameRenderer.getCamera().getPos().distanceTo(p.getPos());
        if (dist > s.distance.get()) return null;
        return SkinMesh.get(p.getSkinTextures(), s.thickness.get().floatValue());
    }

    /** Вызывается после выставления позы модели: прячем плоский второй слой там, где нарисуем объёмный. */
    public static void hideFlatLayers(AbstractClientPlayerEntity p, PlayerEntityModel<AbstractClientPlayerEntity> m) {
        Skin3D s = on();
        if (s == null || mesh(p) == null) return;
        if (s.head.isOn()) m.hat.visible = false;
        if (s.body.isOn()) m.jacket.visible = false;
        if (s.arms.isOn()) { m.leftSleeve.visible = false; m.rightSleeve.visible = false; }
        if (s.legs.isOn()) { m.leftPants.visible = false; m.rightPants.visible = false; }
    }

    /** Рисует объёмный слой (из CosmeticFeatureRenderer). */
    public static void render(AbstractClientPlayerEntity p, PlayerEntityModel<AbstractClientPlayerEntity> m, Voxel v) {
        Skin3D s = on();
        if (s == null) return;
        SkinMesh.Mesh mesh = mesh(p);
        if (mesh == null) return;
        if (s.head.isOn() && m.head.visible) draw(v, m.head, mesh.head());
        if (s.body.isOn() && m.body.visible) draw(v, m.body, mesh.body());
        if (s.arms.isOn()) { if (m.rightArm.visible) draw(v, m.rightArm, mesh.rightArm()); if (m.leftArm.visible) draw(v, m.leftArm, mesh.leftArm()); }
        if (s.legs.isOn()) { if (m.rightLeg.visible) draw(v, m.rightLeg, mesh.rightLeg()); if (m.leftLeg.visible) draw(v, m.leftLeg, mesh.leftLeg()); }
    }

    private static void draw(Voxel v, ModelPart part, List<SkinMesh.Box> boxes) {
        if (boxes.isEmpty()) return;
        v.matrices().push();
        part.rotate(v.matrices());
        for (SkinMesh.Box b : boxes) v.box(b.x1(), b.y1(), b.z1(), b.x2(), b.y2(), b.z2(), b.color(), false, true);
        v.matrices().pop();
    }

    public static boolean active() { return on() != null; }
}
