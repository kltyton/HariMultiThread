package com.axalotl.async.common;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;

/** Samples on the server, reads candidate states in parallel, and applies callbacks on the server. */
public final class AsyncRandomTicks {
    private AsyncRandomTicks() {}

    public static void tickChunks(ServerLevel level, List<LevelChunk> chunks, int speed) {
        LongArrayList positions = new LongArrayList();
        for (int c = 0; c < chunks.size(); c++) {
            LevelChunk chunk = chunks.get(c);
            level.tickChunk(chunk, 0);
            if (speed <= 0) continue;
            LevelChunkSection[] sections = chunk.getSections();
            for (int s = 0; s < sections.length; s++) {
                if (!sections[s].isRandomlyTicking()) continue;
                int minY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(s));
                for (int n = 0; n < speed; n++) {
                    BlockPos pos = level.getBlockRandomPos(chunk.getPos().getMinBlockX(), minY,
                            chunk.getPos().getMinBlockZ(), 15);
                    positions.add((long) c << 32 | (long) s << 12
                            | (long) (pos.getX() & 15) << 8 | (long) (pos.getY() & 15) << 4 | pos.getZ() & 15L);
                }
            }
        }
        if (positions.isEmpty()) return;
        BlockState[] states = new BlockState[positions.size()];
        int batchSize = Math.max(256, (states.length + ParallelProcessor.getPoolSize() * 4)
                / (Math.max(1, ParallelProcessor.getPoolSize()) * 4));
        List<Integer> starts = new ArrayList<>();
        for (int i = 0; i < states.length; i += batchSize) starts.add(i);
        ParallelProcessor.forEachParallel(starts, start -> {
            for (int i = start; i < Math.min(states.length, start + batchSize); i++) {
                long packed = positions.getLong(i);
                states[i] = chunks.get((int) (packed >>> 32)).getSections()[(int) (packed >>> 12 & 0xfffff)]
                        .getBlockState((int) (packed >>> 8 & 15), (int) (packed >>> 4 & 15), (int) (packed & 15));
            }
        });
        boolean callbacksMayHaveChangedWorld = false;
        for (int i = 0; i < states.length; i++) {
            long packed = positions.getLong(i);
            LevelChunk chunk = chunks.get((int) (packed >>> 32));
            int sectionIndex = (int) (packed >>> 12 & 0xfffff);
            int x = (int) (packed >>> 8 & 15);
            int y = (int) (packed >>> 4 & 15);
            int z = (int) (packed & 15);
            BlockState state = callbacksMayHaveChangedWorld
                    ? chunk.getSections()[sectionIndex].getBlockState(x, y, z) : states[i];
            FluidState fluid = state.getFluidState();
            if (!state.isRandomlyTicking() && !fluid.isRandomlyTicking()) continue;
            BlockPos pos = new BlockPos(chunk.getPos().getMinBlockX() + x,
                    SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(sectionIndex)) + y,
                    chunk.getPos().getMinBlockZ() + z);
            callbacksMayHaveChangedWorld = true;
            if (state.isRandomlyTicking()) state.randomTick(level, pos, level.random);
            if (fluid.isRandomlyTicking()) fluid.randomTick(level, pos, level.random);
        }
    }
}
