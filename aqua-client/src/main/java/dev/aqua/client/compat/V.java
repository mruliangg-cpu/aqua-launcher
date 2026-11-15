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
        //#if MC >= 1.21
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

    public static Sprite effectSprite(StatusEffectInstance e) {
        return MinecraftClient.getInstance().getStatusEffectSpriteManager().getSprite(e.getEffectType());
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
}
