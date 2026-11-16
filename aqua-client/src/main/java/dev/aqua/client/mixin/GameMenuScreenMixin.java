package dev.aqua.client.mixin;

import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Кнопка «Сетевая игра» в меню ESC: открывает список серверов, чтобы перейти на другой сервер,
 * не выходя в главное меню. ESC ещё раз — как обычно, назад в игру.
 */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Text title) { super(title); }

    @Inject(method = "initWidgets", at = @At("TAIL"))
    private void aqua$multiplayerButton(CallbackInfo ci) {
        int w = 204, x = this.width / 2 - w / 2, y = this.height - 30;
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("menu.multiplayer"),
                b -> { if (this.client != null) this.client.setScreen(new MultiplayerScreen(this)); })
                .dimensions(x, y, w, 20).build());
    }
}
