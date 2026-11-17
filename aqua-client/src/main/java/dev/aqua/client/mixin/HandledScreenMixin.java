package dev.aqua.client.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Доступ к getSlotAt для Fast Transfer. Не трогаем методы мыши напрямую —
 * в 1.21.9 у них сменилась сигнатура (MouseButtonEvent), поэтому логика вынесена
 * на события экрана Fabric (см. AquaClient), а здесь только безопасный вызов getSlotAt.
 */
@Mixin(HandledScreen.class)
public interface HandledScreenMixin {
    @Invoker("getSlotAt")
    Slot aqua$getSlotAt(double x, double y);
}
