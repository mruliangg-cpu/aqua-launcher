package dev.aqua.client.mixin;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** Доступ к списку drawables экрана, чтобы вставить свой фон ПОД кнопки. */
@Mixin(Screen.class)
public interface ScreenDrawablesAccessor {
    @Accessor("drawables")
    List<Drawable> aqua$drawables();
}
