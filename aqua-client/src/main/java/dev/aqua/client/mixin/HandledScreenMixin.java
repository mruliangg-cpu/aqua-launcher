package dev.aqua.client.mixin;

import dev.aqua.client.module.impl.FastTransfer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

/** Fast Transfer: зажал ЛКМ и ведёшь по слотам — предметы быстро перекидываются. */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Shadow protected abstract Slot getSlotAt(double x, double y);

    @Unique private final Set<Integer> aqua$moved = new HashSet<>();

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void aqua$click(double mx, double my, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!FastTransfer.on() || button != 0 || !FastTransfer.onClickToo()) return;
        aqua$moved.clear();
        if (aqua$transfer(mx, my)) cir.setReturnValue(true);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"))
    private void aqua$drag(double mx, double my, int button, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        if (!FastTransfer.on() || button != 0) return;
        aqua$transfer(mx, my);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void aqua$release(double mx, double my, int button, CallbackInfoReturnable<Boolean> cir) {
        aqua$moved.clear();
    }

    @Unique
    private boolean aqua$transfer(double mx, double my) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return false;
        ScreenHandler h = ((HandledScreen<?>) (Object) this).getScreenHandler();
        if (!h.getCursorStack().isEmpty()) return false;           // только с пустой рукой
        Slot s = getSlotAt(mx, my);
        if (s == null || !s.hasStack()) return false;
        if (!aqua$moved.add(s.id)) return false;                   // каждый слот один раз за протяг
        mc.interactionManager.clickSlot(h.syncId, s.id, 0, SlotActionType.QUICK_MOVE, mc.player);
        return true;
    }
}
