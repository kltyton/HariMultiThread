package com.axalotl.async.common.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.IntConsumer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets={"net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler"})
public abstract class SophisticatedCoreInventoryHandlerMixin {
    @Shadow(remap=false)
    @Mutable
    private Map<Integer, ?> stackNbts;
    @Shadow(remap=false)
    @Mutable
    private List<IntConsumer> onContentsChangedListeners;

    @Inject(method={"<init>"}, at={@At(value="RETURN")}, remap=false)
    private void async$replaceCollections(CallbackInfo ci) {
        if (this.stackNbts != null && !(this.stackNbts instanceof ConcurrentHashMap)) {
            this.stackNbts = new ConcurrentHashMap(this.stackNbts);
        }
        if (this.onContentsChangedListeners != null && !(this.onContentsChangedListeners instanceof CopyOnWriteArrayList)) {
            this.onContentsChangedListeners = new CopyOnWriteArrayList<IntConsumer>(this.onContentsChangedListeners);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @WrapMethod(method={"insertItem(ILnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;"}, remap=false)
    private ItemStack async$syncInsert(int slot, ItemStack stack, boolean simulate, Operation<ItemStack> original) {
        SophisticatedCoreInventoryHandlerMixin sophisticatedCoreInventoryHandlerMixin = this;
        synchronized (sophisticatedCoreInventoryHandlerMixin) {
            return (ItemStack)original.call(new Object[]{slot, stack, simulate});
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @WrapMethod(method={"insertItem(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;"}, remap=false)
    private ItemStack async$syncInsertNoSlot(ItemStack stack, boolean simulate, Operation<ItemStack> original) {
        SophisticatedCoreInventoryHandlerMixin sophisticatedCoreInventoryHandlerMixin = this;
        synchronized (sophisticatedCoreInventoryHandlerMixin) {
            return (ItemStack)original.call(new Object[]{stack, simulate});
        }
    }
}
