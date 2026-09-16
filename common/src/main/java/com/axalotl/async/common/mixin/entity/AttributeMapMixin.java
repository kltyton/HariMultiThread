/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Holder
 *  net.minecraft.world.entity.ai.attributes.Attribute
 *  net.minecraft.world.entity.ai.attributes.AttributeInstance
 *  net.minecraft.world.entity.ai.attributes.AttributeMap
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 */
package com.axalotl.async.common.mixin.entity;

import com.axalotl.async.common.parallelised.ConcurrentCollections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={AttributeMap.class})
public class AttributeMapMixin {
    @Shadow @Final @Mutable
    private Set<AttributeInstance> dirtyAttributes;
    @Shadow @Final @Mutable
    private Map<Attribute, AttributeInstance> attributes;

    @Inject(method="<init>", at=@At("RETURN"))
    private void async$init(CallbackInfo ci) {
        Set<AttributeInstance> concurrent = ConcurrentHashMap.newKeySet();
        concurrent.addAll(dirtyAttributes);
        dirtyAttributes = concurrent;
        attributes = new ConcurrentHashMap<>(attributes);
    }
}

