package dev.aqua.client.cosmetic;

import dev.aqua.client.module.ModuleManager;
import dev.aqua.client.module.impl.AquaNetwork;
import dev.aqua.client.net.AquaNet;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.UUID;

/** Значок у ника (в мире и в TAB) для игроков Aqua Launcher + метка владельца. */
public final class NameTags {
    public static final String OWNER = "MrUlian_gg";
    public static final int OCEAN_BLUE = 0x1CA7EC;
    private static final Identifier FONT = dev.aqua.client.compat.V.id("aquaclient", "icons");

    private NameTags() {}

    public static String glyph(String icon) { return CosmeticRegistry.glyph(icon); }

    public static Text icon(String id) {
        //#if MC >= 1.21.9
        return Text.literal(glyph(id)).styled(s -> s.withFont(new net.minecraft.text.StyleSpriteSource.Font(FONT)).withColor(0xFFFFFF));
        //#else
        return Text.literal(glyph(id)).styled(s -> s.withFont(FONT).withColor(0xFFFFFF));
        //#endif
    }

    /** Плавное «свечение» цвета ника: переливается между двумя оттенками. */
    private static int glow(int from, int to) {
        float t = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 380.0);
        int r = (int) (((from >> 16) & 255) + (((to >> 16) & 255) - ((from >> 16) & 255)) * t);
        int g = (int) (((from >> 8) & 255) + (((to >> 8) & 255) - ((from >> 8) & 255)) * t);
        int b = (int) ((from & 255) + ((to & 255) - (from & 255)) * t);
        return (r << 16) | (g << 8) | b;
    }

    public static Text decorate(UUID id, String name, Text original) {
        if (original == null) return null;
        boolean owner = name != null && OWNER.equalsIgnoreCase(name);
        AquaNetwork net = ModuleManager.get(AquaNetwork.class);
        if (!owner && (net == null || !net.isEnabled() || !net.icons())) return original;
        if (!owner && !AquaNet.isUser(id)) return original;
        String ic = owner ? "turtle" : AquaNet.cosmetic(id, Slot.ICON);
        // владелец — всегда зелёный, игроки Aqua Launcher — океанский синий
        int color = owner ? glow(0x2ECC71, 0x8CFFB0) : glow(0x1CA7EC, 0x7FDBFF);
        Text nameText = name != null && original.getString().equals(name)
                ? Text.literal(name).styled(s -> s.withColor(color))
                : original;
        MutableText out = Text.empty();
        if (!"none".equals(ic) || owner) out.append(icon(ic)).append(Text.literal(" "));
        if (owner) out.append(Text.literal("OWNER ").styled(s -> s.withColor(OCEAN_BLUE).withBold(true)));
        else {
            String[] tt = AquaNet.title(id);
            if (tt != null) { int tc = TitleRules.rgb(tt[1]); out.append(Text.literal(tt[0] + " ").styled(s -> s.withColor(tc).withBold(true))); }
        }
        return out.append(nameText);
    }
}
