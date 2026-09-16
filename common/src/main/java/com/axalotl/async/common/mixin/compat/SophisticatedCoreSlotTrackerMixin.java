package com.axalotl.async.common.mixin.compat;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets={"net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandlerSlotTracker"})
public abstract class SophisticatedCoreSlotTrackerMixin {
    @Shadow(remap=false)
    @Mutable
    private Map<?, Set<?>> fullStackSlots;
    @Shadow(remap=false)
    @Mutable
    private Map<Integer, ?> fullSlotStacks;
    @Shadow(remap=false)
    @Mutable
    private Map<?, Set<?>> partiallyFilledStackSlots;
    @Shadow(remap=false)
    @Mutable
    private Map<Integer, ?> partiallyFilledSlotStacks;
    @Shadow(remap=false)
    @Mutable
    private Map<?, Set<?>> itemStackKeys;
    @Shadow(remap=false)
    @Mutable
    private Set<Integer> emptySlots;

    @Inject(method={"<init>"}, at={@At(value="RETURN")}, remap=false)
    private void async$replaceCollections(CallbackInfo ci) {
        if (this.fullStackSlots != null && !(this.fullStackSlots instanceof ConcurrentHashMap)) {
            this.fullStackSlots = new ConcurrentHashMap(this.fullStackSlots);
        }
        if (this.fullSlotStacks != null && !(this.fullSlotStacks instanceof ConcurrentHashMap)) {
            this.fullSlotStacks = new ConcurrentHashMap(this.fullSlotStacks);
        }
        if (this.partiallyFilledStackSlots != null && !(this.partiallyFilledStackSlots instanceof ConcurrentHashMap)) {
            this.partiallyFilledStackSlots = new ConcurrentHashMap(this.partiallyFilledStackSlots);
        }
        if (this.partiallyFilledSlotStacks != null && !(this.partiallyFilledSlotStacks instanceof ConcurrentHashMap)) {
            this.partiallyFilledSlotStacks = new ConcurrentHashMap(this.partiallyFilledSlotStacks);
        }
        if (this.itemStackKeys != null && !(this.itemStackKeys instanceof ConcurrentHashMap)) {
            this.itemStackKeys = new ConcurrentHashMap(this.itemStackKeys);
        }
        if (this.emptySlots != null && !(this.emptySlots instanceof ConcurrentSkipListSet)) {
            this.emptySlots = new ConcurrentSkipListSet<Integer>(this.emptySlots);
        }
    }
}
