package com.axalotl.async.common.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import java.util.function.Consumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTickList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Preserves insertion order and snapshots iteration without copying maps after each mutation. */
@Mixin(EntityTickList.class)
public class EntityTickListMixin {
    @Unique private final ReferenceLinkedOpenHashSet<Entity> async$entities = new ReferenceLinkedOpenHashSet<>();

    @WrapMethod(method = "add")
    private void async$add(Entity entity, Operation<Void> original) {
        synchronized (async$entities) { async$entities.add(entity); }
    }

    @WrapMethod(method = "remove")
    private void async$remove(Entity entity, Operation<Void> original) {
        synchronized (async$entities) { async$entities.remove(entity); }
    }

    @WrapMethod(method = "contains")
    private boolean async$contains(Entity entity, Operation<Boolean> original) {
        synchronized (async$entities) { return async$entities.contains(entity); }
    }

    @WrapMethod(method = "forEach")
    private void async$forEach(Consumer<Entity> action, Operation<Void> original) {
        Entity[] snapshot;
        synchronized (async$entities) { snapshot = async$entities.toArray(new Entity[0]); }
        for (Entity entity : snapshot) action.accept(entity);
    }
}
