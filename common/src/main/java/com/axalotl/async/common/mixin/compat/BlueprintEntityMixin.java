package com.axalotl.async.common.mixin.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={Entity.class})
public abstract class BlueprintEntityMixin {
    @Unique
    private static final Field ASYNC$DATA_MAP_FIELD = BlueprintEntityMixin.async$resolveDataMapField();

    @Unique
    private static Field async$resolveDataMapField() {
        try {
            Field field = Entity.class.getDeclaredField("dataMap");
            if (Modifier.isStatic(field.getModifiers()) || !Map.class.isAssignableFrom(field.getType())) {
                return null;
            }
            field.setAccessible(true);
            return field;
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    @Inject(method={"<init>"}, at={@At(value="RETURN")})
    private void async$replaceDataMap(CallbackInfo ci) {
        Field field = ASYNC$DATA_MAP_FIELD;
        if (field == null) {
            return;
        }
        try {
            Object current = field.get(this);
            if (!(current instanceof ConcurrentHashMap)) {
                field.set(this, current == null ? new ConcurrentHashMap() : new ConcurrentHashMap((Map)current));
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }
}
