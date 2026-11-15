package dev.aqua.client.module;

import dev.aqua.client.setting.ModeSetting;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Supplier;

/** Простой текстовый HUD-элемент в стиле Lunar: «[144 FPS]». */
public class TextHudModule extends HudModule {
    private final Supplier<String> text;
    private final Supplier<String> preview;
    protected final ModeSetting style = add(new ModeSetting("Стиль", "Плашка", "Плашка", "Скобки", "Текст"));

    public TextHudModule(String name, String description, Category category, float x, float y, Supplier<String> text) {
        this(name, description, category, x, y, text, null);
    }

    public TextHudModule(String name, String description, Category category, float x, float y, Supplier<String> text, Supplier<String> preview) {
        super(name, description, category, x, y);
        this.text = text;
        this.preview = preview;
    }

    /** null = ничего не показывать (вне редактора). */
    protected String content(boolean editor) {
        String s = null;
        try { s = text.get(); } catch (Exception ignored) {}
        if (s == null && editor) s = preview != null ? preview.get() : getName();
        return s;
    }

    @Override
    protected void draw(DrawContext ctx, boolean editor) {
        String s = content(editor);
        if (s == null) { width = 0; height = 0; return; }
        if (style.is("Скобки")) s = "[" + s + "]";
        int tw = mc.textRenderer.getWidth(s);
        if (style.is("Плашка")) {
            width = Math.max(tw + 10, 46);
            height = 16;
            drawBackground(ctx, width, height);
            text(ctx, s, (width - tw) / 2, 4);
        } else {
            width = tw;
            height = 9;
            text(ctx, s, 0, 0);
        }
    }
}
