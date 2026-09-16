package com.axalotl.async.common.parallelised.utils;

import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PortalCreationCache {

    private record Key(ResourceKey<Level> dimension, BlockPos position, boolean nether) {}
    private static final Map<Key, BlockUtil.FoundRectangle> cache = new ConcurrentHashMap<>();

    public static BlockUtil.FoundRectangle get(ResourceKey<Level> dimension, BlockPos position, boolean nether) {
        return cache.get(new Key(dimension, position, nether));
    }

    public static void put(ResourceKey<Level> dimension, BlockPos position, boolean nether, BlockUtil.FoundRectangle rectangle) {
        cache.put(new Key(dimension, position.immutable(), nether), rectangle);
    }

    public static void clear() {
        cache.clear();
    }
}
