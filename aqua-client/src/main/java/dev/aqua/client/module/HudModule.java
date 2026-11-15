package dev.aqua.client.module;

import dev.aqua.client.gui.Render2D;
import dev.aqua.client.gui.Theme;
import dev.aqua.client.setting.BoolSetting;
import dev.aqua.client.setting.ColorSetting;
import dev.aqua.client.setting.NumberSetting;
import net.minecraft.client.gui.DrawContext;

/** Модуль, который рисует элемент на экране и двигается в HUD-редакторе. */
public abstract class HudModule extends Module {
    /** Позиция как доля от размера экрана (0..1) — не ломается при смене разрешения. */
    public float x, y;
    private final float defX, defY;
    protected int width = 40, height = 16;

    protected final NumberSetting scale = add(new NumberSetting("Размер", 1.0, 0.5, 2.5, 0.05, "x"));
    protected final BoolSetting background = add(new BoolSetting("Фон", true));
    protected final ColorSetting bgColor = add(new ColorSetting("Цвет фона", 0x8008243A), background::isOn);
    protected final ColorSetting textColor = add(new ColorSetting("Цвет текста", 0xFFFFFFFF));
    protected final BoolSetting shadow = add(new BoolSetting("Тень текста", true));

    protected HudModule(String name, String description, Category category, float x, float y) {
        super(name, description, category);
        this.x = this.defX = x;
        this.y = this.defY = y;
    }

    /** Рисует содержимое в локальных координатах (0,0) и выставляет width/height. */
    protected abstract void draw(DrawContext ctx, boolean editor);

    public final void render(DrawContext ctx, boolean editor) {
        float s = getScale();
        float px = getScreenX(), py = getScreenY();
        ctx.getMatrices().push();
        ctx.getMatrices().translate(px, py, 0);
        ctx.getMatrices().scale(s, s, 1);
        draw(ctx, editor);
        ctx.getMatrices().pop();
    }

    public float getScale() { return scale.getFloat(); }
    public float getScaledWidth() { return width * getScale(); }
    public float getScaledHeight() { return height * getScale(); }

    public float getScreenX() {
        int sw = mc.getWindow().getScaledWidth();
        return Math.max(0, Math.min(sw - getScaledWidth(), x * sw));
    }

    public float getScreenY() {
        int sh = mc.getWindow().getScaledHeight();
        return Math.max(0, Math.min(sh - getScaledHeight(), y * sh));
    }

    public void setScreenPos(float px, float py) {
        x = px / mc.getWindow().getScaledWidth();
        y = py / mc.getWindow().getScaledHeight();
    }

    public void resetPosition() { x = defX; y = defY; scale.reset(); }

    public void setScale(double v) { scale.set(v); }

    // ---------- помощники для наследников ----------
    protected void drawBackground(DrawContext ctx, int w, int h) {
        if (background.isOn()) Render2D.roundRect(ctx, 0, 0, w, h, 3, bgColor.color());
    }

    protected int text(DrawContext ctx, String s, int x, int y) {
        return text(ctx, s, x, y, textColor.color(x / 200f));
    }

    protected int text(DrawContext ctx, String s, int x, int y, int color) {
        return ctx.drawText(mc.textRenderer, s, x, y, color, shadow.isOn());
    }

    protected int accent() { return Theme.ACCENT; }
}
