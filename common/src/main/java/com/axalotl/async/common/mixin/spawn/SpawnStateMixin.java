package com.axalotl.async.common.mixin.spawn;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;

/** Protects both reads and updates of the shared spawn counts and last checked spawn cost. */
@Mixin(NaturalSpawner.SpawnState.class)
public class SpawnStateMixin {
    @WrapMethod(method = "canSpawn")
    private boolean async$canSpawn(EntityType<?> type, BlockPos pos, ChunkAccess chunk, Operation<Boolean> original) {
        synchronized (this) { return original.call(type, pos, chunk); }
    }

    @WrapMethod(method = "afterSpawn")
    private void async$afterSpawn(Mob mob, ChunkAccess chunk, Operation<Void> original) {
        synchronized (this) { original.call(mob, chunk); }
    }

    @WrapMethod(method = "canSpawnForCategory")
    private boolean async$canSpawnCategory(MobCategory category, ChunkPos pos, Operation<Boolean> original) {
        synchronized (this) { return original.call(category, pos); }
    }
}
