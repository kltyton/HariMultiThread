package com.axalotl.async.common.mixin.compat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets={"net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler$Accessor"})
public abstract class SophisticatedCoreAccessorMixin {
    @Shadow(remap=false)
    @Final
    @Mutable
    private Map<Class<?>, List<?>> interfaceWrappers;

    @Inject(method="<init>", at=@At("RETURN"), remap=false)
    private void async$initializeCache(CallbackInfo ci) {
        this.interfaceWrappers = new ConcurrentHashMap<>(this.interfaceWrappers);
    }
}
