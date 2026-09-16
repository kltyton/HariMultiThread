package com.axalotl.async.common.mixin.entity;

import com.axalotl.async.common.parallelised.ConcurrentCollections;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Uses the UUID and Set storage contracts of Minecraft 1.20.1. */
@Mixin(AttributeInstance.class)
public class AttributeInstanceMixin {
    @Shadow @Final @Mutable private Map<AttributeModifier.Operation, Set<AttributeModifier>> modifiersByOperation;
    @Shadow @Final @Mutable private Map<UUID, AttributeModifier> modifierById;
    @Shadow @Final @Mutable private Set<AttributeModifier> permanentModifiers;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void async$collections(CallbackInfo ci) {
        Map<AttributeModifier.Operation, Set<AttributeModifier>> operations = ConcurrentCollections.newHashMap();
        modifiersByOperation.forEach((operation, modifiers) -> {
            Set<AttributeModifier> copy = ConcurrentCollections.newHashSet();
            copy.addAll(modifiers);
            operations.put(operation, copy);
        });
        modifiersByOperation = operations;
        Map<UUID, AttributeModifier> byId = ConcurrentCollections.newHashMap();
        byId.putAll(modifierById);
        modifierById = byId;
        Set<AttributeModifier> permanent = ConcurrentCollections.newHashSet();
        permanent.addAll(permanentModifiers);
        permanentModifiers = permanent;
    }

    @WrapMethod(method = "getModifiers(Lnet/minecraft/world/entity/ai/attributes/AttributeModifier$Operation;)Ljava/util/Set;")
    private Set<AttributeModifier> async$getModifiers(AttributeModifier.Operation operation,
                                                     Operation<Set<AttributeModifier>> original) {
        return modifiersByOperation.computeIfAbsent(operation, ignored -> ConcurrentCollections.newHashSet());
    }
}
