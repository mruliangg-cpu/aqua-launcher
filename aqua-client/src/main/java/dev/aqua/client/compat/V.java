package dev.aqua.client.compat;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.util.Identifier;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleEffect;

/**
 * Слой совместимости Aqua Client: всё, что отличается между версиями Minecraft, собрано здесь.
 * Нужный вариант выбирает препроцессор (//#if MC ...) при сборке под конкретную версию.
 */
public final class V {
    private V() {}

    public static Identifier id(String ns, String path) {
        //#if MC >= 1.21
        return Identifier.of(ns, path);
        //#else
        return new Identifier(ns, path);
        //#endif
    }

    /** Нормаль вершины. */
    public static VertexConsumer normal(VertexConsumer vc, MatrixStack.Entry e, float x, float y, float z) {
        //#if MC >= 1.20.5
        return vc.normal(e, x, y, z);
        //#else
        return vc.normal(e.getNormalMatrix(), x, y, z);
        //#endif
    }

    /** Конец вершины (до 1.21 обязателен next()). */
    public static void end(VertexConsumer vc) {
        //#if MC < 1.21
        vc.next();
        //#endif
    }

    public static float tickDelta(WorldRenderContext ctx) {
        //#if MC >= 1.21.5
        return ctx.tickCounter().getTickProgress(true);
        //#elseif MC >= 1.21
        return ctx.tickCounter().getTickDelta(true);
        //#else
        return ctx.tickDelta();
        //#endif
    }

    public static String packId(ResourcePackProfile p) {
        //#if MC >= 1.20.5
        return p.getId();
        //#else
        return p.getName();
        //#endif
    }

    public static String effectName(StatusEffectInstance e) {
        //#if MC >= 1.20.5
        return e.getEffectType().value().getName().getString();
        //#else
        return e.getEffectType().getName().getString();
        //#endif
    }

    /** Иконка эффекта зелья. */
    public static void drawEffect(DrawContext c, StatusEffectInstance e, int x, int y, int size) {
        //#if MC >= 1.21.6
        c.drawGuiTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, net.minecraft.client.gui.hud.InGameHud.getEffectTexture(e.getEffectType()), x, y, size, size);
        //#else
        sprite(c, MinecraftClient.getInstance().getStatusEffectSpriteManager().getSprite(e.getEffectType()), x, y, size, size);
        //#endif
    }

    public static int enchantCount(ItemStack st) {
        //#if MC >= 1.20.5
        return st.getEnchantments().getSize();
        //#else
        return st.getEnchantments().size();
        //#endif
    }

    public static void drawHead(DrawContext ctx, AbstractClientPlayerEntity p, int x, int y, int size) {
        //#if MC >= 1.20.2
        PlayerSkinDrawer.draw(ctx, p.getSkinTextures(), x, y, size);
        //#else
        PlayerSkinDrawer.draw(ctx, p.getSkinTexture(), x, y, size);
        //#endif
    }

    /** Данные скина для 3D-слоя: текстура, адрес загрузки (может быть null) и тонкие руки. */
    public record Skin(Identifier texture, String url, boolean slim) {}

    public static Skin skin(AbstractClientPlayerEntity p) {
        //#if MC >= 1.20.2
        var s = p.getSkinTextures();
        return new Skin(s.texture(), s.textureUrl(), s.model() == net.minecraft.client.util.SkinTextures.Model.SLIM);
        //#else
        String url = null;
        try {
            var tex = MinecraftClient.getInstance().getSkinProvider().getTextures(p.getGameProfile())
                    .get(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN);
            if (tex != null) url = tex.getUrl();
        } catch (Exception ignored) {}
        return new Skin(p.getSkinTexture(), url, "slim".equals(p.getModel()));
        //#endif
    }

    // ------------------------------------------------------------------ 2D (меню и HUD)
    public static void push(DrawContext c) {
        //#if MC >= 1.21.6
        c.getMatrices().pushMatrix();
        //#else
        c.getMatrices().push();
        //#endif
    }

    public static void pop(DrawContext c) {
        //#if MC >= 1.21.6
        c.getMatrices().popMatrix();
        //#else
        c.getMatrices().pop();
        //#endif
    }

    public static void translate(DrawContext c, float x, float y) {
        //#if MC >= 1.21.6
        c.getMatrices().translate(x, y);
        //#else
        c.getMatrices().translate(x, y, 0);
        //#endif
    }

    public static void scale(DrawContext c, float s) {
        //#if MC >= 1.21.6
        c.getMatrices().scale(s, s);
        //#else
        c.getMatrices().scale(s, s, 1);
        //#endif
    }

    public static void rotateZ(DrawContext c, float degrees) {
        //#if MC >= 1.21.6
        c.getMatrices().rotate((float) Math.toRadians(degrees));
        //#else
        c.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(degrees));
        //#endif
    }

    /** Текст; возвращает x после текста (как раньше drawText). */
    public static int text(DrawContext c, TextRenderer tr, String s, int x, int y, int color, boolean shadow) {
        //#if MC >= 1.21.6
        c.drawText(tr, s, x, y, color, shadow);
        return x + tr.getWidth(s) + (shadow ? 1 : 0);
        //#else
        return c.drawText(tr, s, x, y, color, shadow);
        //#endif
    }

    public static void texture(DrawContext c, Identifier id, int x, int y, float u, float v, int w, int h, int tw, int th) {
        //#if MC >= 1.21.6
        c.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, id, x, y, u, v, w, h, tw, th);
        //#elseif MC >= 1.21.2
        c.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, id, x, y, u, v, w, h, tw, th);
        //#else
        c.drawTexture(id, x, y, u, v, w, h, tw, th);
        //#endif
    }

    public static void sprite(DrawContext c, Sprite sp, int x, int y, int w, int h) {
        //#if MC >= 1.21.6
        c.drawSpriteStretched(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, sp, x, y, w, h);
        //#elseif MC >= 1.21.2
        c.drawSpriteStretched(net.minecraft.client.render.RenderLayer::getGuiTextured, sp, x, y, w, h);
        //#else
        c.drawSprite(x, y, 0, w, h, sp);
        //#endif
    }

    public static void itemOverlay(DrawContext c, TextRenderer tr, ItemStack st, int x, int y) {
        //#if MC >= 1.21.2
        c.drawStackOverlay(tr, st, x, y);
        //#else
        c.drawItemInSlot(tr, st, x, y);
        //#endif
    }

    // ------------------------------------------------------------------ текстуры
    public static NativeImageBackedTexture texture(NativeImage img) {
        //#if MC >= 1.21.5
        return new NativeImageBackedTexture(() -> "aquaclient", img);
        //#else
        return new NativeImageBackedTexture(img);
        //#endif
    }

    public static NativeImageBackedTexture texture(int w, int h) {
        //#if MC >= 1.21.5
        return new NativeImageBackedTexture(() -> "aquaclient", w, h, true);
        //#else
        return new NativeImageBackedTexture(w, h, true);
        //#endif
    }

    public static int getArgb(NativeImage img, int x, int y) {
        //#if MC >= 1.21.2
        return img.getColorArgb(x, y);
        //#else
        int abgr = img.getColor(x, y);
        return (abgr & 0xFF00FF00) | ((abgr & 0xFF) << 16) | ((abgr >> 16) & 0xFF);
        //#endif
    }

    public static void setArgb(NativeImage img, int x, int y, int argb) {
        //#if MC >= 1.21.2
        img.setColorArgb(x, y, argb);
        //#else
        img.setColor(x, y, (argb & 0xFF00FF00) | ((argb & 0xFF) << 16) | ((argb >> 16) & 0xFF));
        //#endif
    }

    // ------------------------------------------------------------------ мир
    public static void particle(ParticleEffect e, double x, double y, double z, double vx, double vy, double vz) {
        MinecraftClient.getInstance().particleManager.addParticle(e, x, y, z, vx, vy, vz);
    }

    public static void setTimeOfDay(long t) {
        var w = MinecraftClient.getInstance().world;
        if (w == null) return;
        //#if MC >= 1.21.2
        w.setTime(w.getTime(), t, false);
        //#else
        w.setTimeOfDay(t);
        //#endif
    }

    public static float cooldown(net.minecraft.client.network.ClientPlayerEntity p, Item item) {
        //#if MC >= 1.21.2
        return p.getItemCooldownManager().getCooldownProgress(item.getDefaultStack(), 0);
        //#else
        return p.getItemCooldownManager().getCooldownProgress(item, 0);
        //#endif
    }

    public static void filledBox(MatrixStack ms, VertexConsumer vc, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float a) {
        //#if MC >= 1.21.2
        net.minecraft.client.render.VertexRendering.drawFilledBox(ms, vc, x1, y1, z1, x2, y2, z2, r, g, b, a);
        //#else
        net.minecraft.client.render.WorldRenderer.renderFilledBox(ms, vc, x1, y1, z1, x2, y2, z2, r, g, b, a);
        //#endif
    }

    /** Применить поворот/сдвиг части модели (до 1.21.5 — rotate). */
    public static void applyPart(ModelPart part, MatrixStack ms) {
        //#if MC >= 1.21.5
        part.applyTransform(ms);
        //#else
        part.rotate(ms);
        //#endif
    }
}
