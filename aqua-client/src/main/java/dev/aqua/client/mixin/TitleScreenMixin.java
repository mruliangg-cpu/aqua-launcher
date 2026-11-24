package dev.aqua.client.mixin;

import dev.aqua.client.gui.AquaMenu;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Тёмное главное меню в стиле Lunar: свой фон вместо панорамы и
 * аккуратно переставленные кнопки по центру.
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Text title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void aqua$menu(CallbackInfo ci) {
        // 1) тёмный фон: вставляем первым drawable — рисуется поверх панорамы, но под кнопками
        try {
            Drawable bg = (ctx, mx, my, delta) -> AquaMenu.renderBackground(ctx, this.width, this.height);
            ((ScreenDrawablesAccessor) this).aqua$drawables().add(0, bg);
        } catch (Throwable ignored) {}

        // 2) переставляем кнопки в ровную центральную колонку
        try {
            List<ClickableWidget> wide = new ArrayList<>();
            List<ClickableWidget> half = new ArrayList<>();
            for (var e : this.children()) {
                if (e instanceof ClickableWidget cw) {
                    int wd = cw.getWidth();
                    if (wd >= 180) wide.add(cw);
                    else if (wd >= 90) half.add(cw);
                }
            }
            if (wide.isEmpty()) return;
            wide.sort((a, b) -> Integer.compare(a.getY(), b.getY()));
            half.sort((a, b) -> Integer.compare(a.getX(), b.getX()));

            int bh = 20, gap = 5, cx = this.width / 2;
            int rows = wide.size() + (half.size() >= 2 ? 1 : half.size());
            int total = rows * bh + (rows - 1) * gap;
            int y = Math.max((this.height - total) / 2 + 14, 72);

            for (ClickableWidget cw : wide) { cw.setWidth(200); cw.setX(cx - 100); cw.setY(y); y += bh + gap; }
            if (half.size() >= 2) {
                half.get(0).setWidth(98); half.get(0).setX(cx - 100); half.get(0).setY(y);
                half.get(1).setWidth(98); half.get(1).setX(cx + 2);   half.get(1).setY(y);
            } else {
                for (ClickableWidget cw : half) { cw.setWidth(200); cw.setX(cx - 100); cw.setY(y); y += bh + gap; }
            }
        } catch (Throwable ignored) {}
    }
}
