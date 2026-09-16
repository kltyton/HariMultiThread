package com.axalotl.async.common.utils;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;

public final class LastChunkCache {
    private static final int SIZE = 4;
    private final Object[] owners = new Object[4];
    private final long[] positions = new long[4];
    private final ChunkStatus[] statuses = new ChunkStatus[4];
    private final ChunkAccess[] chunks = new ChunkAccess[4];
    private final int[] ticks = new int[4];
    private int nextIndex = 0;

    @Nullable
    public ChunkAccess find(Object owner, long positionKey, ChunkStatus status, int tick) {
        for (int i = 0; i < 4; ++i) {
            if (this.ticks[i] != tick || this.owners[i] != owner || this.positions[i] != positionKey || this.statuses[i] != status) continue;
            return this.chunks[i];
        }
        return null;
    }

    public void store(Object owner, long positionKey, ChunkStatus status, ChunkAccess chunk, int tick) {
        int slot = this.nextIndex;
        this.owners[slot] = owner;
        this.positions[slot] = positionKey;
        this.statuses[slot] = status;
        this.chunks[slot] = chunk;
        this.ticks[slot] = tick;
        this.nextIndex = (slot + 1) % 4;
    }
}
