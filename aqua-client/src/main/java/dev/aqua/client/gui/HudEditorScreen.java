package dev.aqua.client.gui;

import dev.aqua.client.config.ConfigManager;
import dev.aqua.client.module.HudModule;
import dev.aqua.client.module.ModuleManager;
import dev.aqua.client.util.Anim;
import dev.aqua.client.util.ColorUtil;
import dev.aqua.client.util.Lang;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Редактор HUD: перетаскивайте элементы мышью, колёсико — размер,
 * ПКМ — настройки модуля. Прилипание к центру и краям экрана.
 */
public class HudEditorScreen extends Screen {
    private static final float SNAP = 4;
    private HudModule dragging;
    private float grabX, grabY;
    private boolean snapX, snapY;
    private final Anim fade = new Anim(0);
    private long lastNs = System.nanoTime();

    public HudEditorScreen() { super(Text.literal("Aqua HUD Editor")); }

    @Override
    //#if MC >= 1.20.2
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
    //#else
    public void renderBackground(DrawContext ctx) {
    //#endif
        // без размытия — видно реальную игру
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastNs) / 1e9f);
        lastNs = now;
        fade.to(1);
        float a = fade.update(dt, 8);

        ctx.fill(0, 0, width, height, ColorUtil.fade(0x50000000, a));
        // сетка
        int grid = ColorUtil.fade(0x143EE6FF, a);
        for (int x = 0; x < width; x += 20) ctx.fill(x, 0, x + 1, height, grid);
        for (int y = 0; y < height; y += 20) ctx.fill(0, y, width, y + 1, grid);
        // направляющие центра
        int guide = ColorUtil.fade(dragging != null && snapX ? 0xFF5FF2C8 : 0x403EE6FF, a);
        ctx.fill(width / 2, 0, width / 2 + 1, height, guide);
        guide = ColorUtil.fade(dragging != null && snapY ? 0xFF5FF2C8 : 0x403EE6FF, a);
        ctx.fill(0, height / 2, width, height / 2 + 1, guide);

        List<HudModule> huds = ModuleManager.huds();
        for (HudModule h : huds) {
            if (!h.isEnabled()) continue;
            h.render(ctx, true);
            float x = h.getScreenX(), y = h.getScreenY(), w = h.getScaledWidth(), hh = h.getScaledHeight();
            boolean hov = Render2D.hover(mx, my, x, y, w, hh);
            int border = h == dragging ? 0xFF5FF2C8 : hov ? 0xFF3EE6FF : 0x803EE6FF;
            Render2D.roundOutline(ctx, x - 2, y - 2, w + 4, hh + 4, 2, ColorUtil.fade(border, a));
            if (hov && dragging == null) {
                String tip = h.getName() + "  •  " + Math.round(h.getScale() * 100) + "%";
                int tw = textRenderer.getWidth(tip);
                float ty = y - 14 < 2 ? y + hh + 4 : y - 14;
                Render2D.roundRect(ctx, x, ty, tw + 8, 12, 3, 0xE0061A2C);
                ctx.drawText(textRenderer, tip, (int) x + 4, (int) ty + 2, 0xFFE9FBFF, false);
            }
        }

        // панель подсказок сверху
        String help = Lang.tr("ЛКМ — двигать   •   Колёсико — размер   •   ПКМ — настройки   •   ESC — назад");
        int hw = textRenderer.getWidth(help) + 20;
        float hx = (width - hw) / 2f, hy = 8 - (1 - a) * 20;
        Render2D.roundRect(ctx, hx, hy, hw, 18, 5, ColorUtil.fade(0xE0061A2C, a));
        Render2D.roundOutline(ctx, hx, hy, hw, 18, 5, ColorUtil.fade(0xFF1B4A6B, a));
        ctx.drawText(textRenderer, help, (int) hx + 10, (int) hy + 5, ColorUtil.textFade(0xFFCFEFFF, a), false);

        // кнопки снизу
        drawButton(ctx, mx, my, width / 2f - 104, height - 28, 100, Lang.tr("‹ Меню модулей"), a);
        drawButton(ctx, mx, my, width / 2f + 4, height - 28, 100, Lang.tr("Сбросить всё"), a);
    }

    private void drawButton(DrawContext ctx, int mx, int my, float x, float y, float w, String label, float a) {
        boolean hov = Render2D.hover(mx, my, x, y, w, 18);
        Render2D.roundRect(ctx, x, y, w, 18, 5, ColorUtil.fade(hov ? Theme.ACCENT : Theme.ACCENT_DARK, a));
        Render2D.centered(ctx, label, x + w / 2, y + 5, ColorUtil.textFade(0xFFFFFFFF, a));
    }

    private HudModule at(double mx, double my) {
        List<HudModule> huds = ModuleManager.huds();
        for (int i = huds.size() - 1; i >= 0; i--) {
            HudModule h = huds.get(i);
            if (h.isEnabled() && Render2D.hover(mx, my, h.getScreenX() - 2, h.getScreenY() - 2, h.getScaledWidth() + 4, h.getScaledHeight() + 4)) return h;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (Render2D.hover(mx, my, width / 2f - 104, height - 28, 100, 18)) { close(); return true; }
        if (Render2D.hover(mx, my, width / 2f + 4, height - 28, 100, 18)) {
            ModuleManager.huds().forEach(HudModule::resetPosition);
            ConfigManager.save();
            return true;
        }
        HudModule h = at(mx, my);
        if (h == null) return super.mouseClicked(mx, my, button);
        if (button == 1) {
            if (client != null) client.setScreen(new ClickGuiScreen(h));
            return true;
        }
        dragging = h;
        grabX = (float) mx - h.getScreenX();
        grabY = (float) my - h.getScreenY();
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging == null) return super.mouseDragged(mx, my, button, dx, dy);
        float w = dragging.getScaledWidth(), h = dragging.getScaledHeight();
        float nx = (float) mx - grabX, ny = (float) my - grabY;
        snapX = snapY = false;
        // прилипание к центру
        if (Math.abs(nx + w / 2 - width / 2f) < SNAP) { nx = width / 2f - w / 2; snapX = true; }
        if (Math.abs(ny + h / 2 - height / 2f) < SNAP) { ny = height / 2f - h / 2; snapY = true; }
        // прилипание к краям
        if (nx < SNAP) nx = 1;
        if (ny < SNAP) ny = 1;
        if (nx + w > width - SNAP) nx = width - w - 1;
        if (ny + h > height - SNAP) ny = height - h - 1;
        // прилипание к другим элементам
        for (HudModule o : ModuleManager.huds()) {
            if (o == dragging || !o.isEnabled()) continue;
            float ox = o.getScreenX(), oy = o.getScreenY();
            if (Math.abs(nx - ox) < SNAP) nx = ox;
            if (Math.abs(ny - (oy + o.getScaledHeight() + 2)) < SNAP) ny = oy + o.getScaledHeight() + 2;
        }
        dragging.setScreenPos(nx, ny);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null) {
            dragging = null;
            snapX = snapY = false;
            ConfigManager.save();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    //#if MC >= 1.20.2
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
    //#else
    public boolean mouseScrolled(double mx, double my, double vertical) {
    //#endif
        HudModule h = at(mx, my);
        if (h != null) {
            h.setScale(h.getScale() + (vertical > 0 ? 0.05 : -0.05));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        ConfigManager.save();
        if (client != null) client.setScreen(new ClickGuiScreen());
    }

    @Override
    public boolean shouldPause() { return false; }
}
