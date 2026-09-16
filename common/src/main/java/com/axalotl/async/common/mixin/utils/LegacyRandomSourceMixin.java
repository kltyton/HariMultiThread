package com.axalotl.async.common.mixin.utils;

import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.MarsagliaPolarGaussian;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/** Uses the vanilla LCG with retrying atomic updates and protects the Gaussian cache. */
@Mixin(LegacyRandomSource.class)
public abstract class LegacyRandomSourceMixin {
    @Shadow @Final private AtomicLong seed;
    @Shadow @Final private MarsagliaPolarGaussian gaussianSource;

    /** @author Async contributors @reason Concurrent callers must retry the same LCG transition. */
    @Overwrite
    public int next(int bits) {
        long previous;
        long next;
        do {
            previous = seed.get();
            next = (previous * 25214903917L + 11L) & 0xffffffffffffL;
        } while (!seed.compareAndSet(previous, next));
        return (int) (next >>> (48 - bits));
    }

    /** @author Async contributors @reason Reseeding also invalidates the cached Gaussian value. */
    @Overwrite
    public void setSeed(long value) {
        synchronized (gaussianSource) {
            seed.set((value ^ 25214903917L) & 0xffffffffffffL);
            gaussianSource.reset();
        }
    }

    /** @author Async contributors @reason MarsagliaPolarGaussian has mutable cached state. */
    @Overwrite
    public double nextGaussian() {
        synchronized (gaussianSource) { return gaussianSource.nextGaussian(); }
    }
}
