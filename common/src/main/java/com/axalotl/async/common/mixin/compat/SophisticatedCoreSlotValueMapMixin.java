package com.axalotl.async.common.mixin.compat;

import java.util.HashSet;
import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The plugin synchronizes SlotValueMap operations so its two indexes change together. */
@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.util.SlotValueMap", remap = false)
public abstract class SophisticatedCoreSlotValueMapMixin {
    @Inject(method = {"getSlots", "keySet"}, at = @At("RETURN"), cancellable = true, remap = false)
    private void async$snapshotSlots(CallbackInfoReturnable<Set<?>> cir) {
        // Callers iterate after the method releases the monitor.
        cir.setReturnValue(new HashSet<>(cir.getReturnValue()));
    }
}
