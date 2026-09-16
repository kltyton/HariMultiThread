package com.axalotl.async.common;

import com.axalotl.async.common.config.AsyncConfig;
import com.axalotl.async.common.gpu.GpuEntityModule;
import com.axalotl.async.common.mixin.accessor.EntityAccessor;
import com.axalotl.async.common.parallelised.utils.AsyncCompatible;
import com.axalotl.async.common.utils.EntityTickCircuitBreaker;
import com.axalotl.async.common.utils.TickStats;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ParallelProcessor {
    public static final Logger LOGGER = LogManager.getLogger(ParallelProcessor.class);
    private static MinecraftServer server;
    public static final AtomicInteger currentEntities = new AtomicInteger();
    private static final AtomicInteger threadPoolID = new AtomicInteger();
    public static ExecutorService tickPool;
    private static final Map<String, Set<WeakReference<Thread>>> mcThreadTracker = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Boolean> asyncApiCache = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> portalSyncUntil = new ConcurrentHashMap<>();
    private static volatile boolean isShuttingDown;
    private static final Object ENTITY_ADD_LOCK = new Object();
    private static final ConcurrentLinkedQueue<Runnable> entityCallbacks = new ConcurrentLinkedQueue<>();
    private static final EntityTickCircuitBreaker circuitBreaker = new EntityTickCircuitBreaker();
    private static final LongAdder totalAsyncTicks = new LongAdder();
    private static final LongAdder totalAsyncFailures = new LongAdder();
    private static final LongAdder totalTimeoutWarnings = new LongAdder();
    private static final AtomicInteger lastWorkerCount = new AtomicInteger();
    public static final CostModel ENTITY_TICK_COST = new CostModel(25_000);
    public static final CostModel DESPAWN_COST = new CostModel(2_000);
    public static final CostModel GENERIC_COST = new CostModel(100_000);
    public static final CostModel SPAWN_COST = new CostModel(100_000);
    public static final Set<Class<?>> BLOCKED_ENTITIES = Set.of(
            FallingBlockEntity.class, Shulker.class, Boat.class, EnderDragon.class, LightningBolt.class);

    public static EntityTickCircuitBreaker getCircuitBreaker() { return circuitBreaker; }
    public static int getTotalAsyncTicks() { return totalAsyncTicks.intValue(); }
    public static int getTotalAsyncFailures() { return totalAsyncFailures.intValue(); }
    public static int getTotalTimeoutWarnings() { return totalTimeoutWarnings.intValue(); }
    public static int getLastWorkerCount() { return lastWorkerCount.get(); }

    public static void setupThreadPool(int parallelism, Class<?> asyncClass) {
        isShuttingDown = false;
        int workers = Math.max(1, parallelism);
        ThreadPoolExecutor pool = new ThreadPoolExecutor(workers, workers, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(Math.max(16, workers * 4)), runnable -> {
                    Thread thread = new Thread(runnable, "Async-Tick-Pool-Thread-" + threadPoolID.getAndIncrement());
                    registerThread("Async-Tick", thread);
                    thread.setDaemon(true);
                    thread.setPriority(Thread.NORM_PRIORITY - 1);
                    thread.setContextClassLoader(asyncClass.getClassLoader());
                    return thread;
                });
        pool.prestartAllCoreThreads();
        tickPool = pool;
        LOGGER.info("Initialized Pool with {} threads; adaptive batches and main-thread assistance", workers);
        if (AsyncConfig.enableGpuCollision.getValue()) {
            GpuEntityModule.initialize();
        }
    }

    public static void registerThread(String poolName, Thread thread) {
        mcThreadTracker.computeIfAbsent(poolName, key -> ConcurrentHashMap.newKeySet()).add(new WeakReference<>(thread));
    }

    public static boolean isServerExecutionThread() {
        Thread current = Thread.currentThread();
        for (WeakReference<Thread> reference : mcThreadTracker.getOrDefault("Async-Tick", Set.of())) {
            if (reference.get() == current) return true;
        }
        return false;
    }

    public static int getPoolSize() {
        return tickPool instanceof ThreadPoolExecutor pool ? pool.getCorePoolSize() : 0;
    }

    public static void callEntityTickBatch(ServerLevel world, List<Entity> entities) {
        callEntityTickBatch(world, entities, null);
    }

    public static void callEntityTickBatch(ServerLevel world, List<Entity> entities, List<Entity> despawnOnly) {
        lastWorkerCount.set(0);
        boolean despawn = despawnOnly != null;
        List<Entity> syncEntities = new ArrayList<>();
        List<Entity> asyncEntities = new ArrayList<>(entities.size());
        for (Entity entity : entities) {
            if (entity.isRemoved()) continue;
            if (isPortalTickRequired(entity)) {
                portalSyncUntil.put(entity.getUUID(), world.getServer().getTickCount() + 40);
            }
            (shouldTickSynchronously(entity) ? syncEntities : asyncEntities).add(entity);
        }
        Consumer<Entity> tick = entity -> {
            if (despawn) entity.checkDespawn();
            if (!entity.isRemoved()) tickEntity(world, entity);
        };
        List<Entity> syncDespawn = new ArrayList<>();
        List<Entity> asyncDespawn = new ArrayList<>();
        if (despawn) {
            for (Entity entity : despawnOnly) {
                (shouldTickSynchronously(entity) ? syncDespawn : asyncDespawn).add(entity);
            }
        }
        if (!canParallelize() || ENTITY_TICK_COST.shouldRunSequentially(asyncEntities.size())
                && DESPAWN_COST.shouldRunSequentially(asyncDespawn.size())) {
            for (Entity entity : syncDespawn) checkDespawn(entity);
            for (Entity entity : asyncDespawn) checkDespawn(entity);
            ENTITY_TICK_COST.measure(asyncEntities.size(), () -> asyncEntities.forEach(tick));
            syncEntities.forEach(tick);
            return;
        }
        List<Runnable> tasks = buildSpatialWork(asyncEntities, tick);
        addSlices(tasks, asyncDespawn, DESPAWN_COST, ParallelProcessor::checkDespawn);
        ParallelBatch batch = submitParallel(tasks);
        try {
            syncDespawn.forEach(ParallelProcessor::checkDespawn);
            syncEntities.forEach(tick);
        } finally {
            finishParallel(batch);
        }
    }

    private static void checkDespawn(Entity entity) {
        if (!entity.isRemoved()) entity.checkDespawn();
    }

    private static List<Runnable> buildSpatialWork(List<Entity> entities, Consumer<Entity> action) {
        List<Runnable> tasks = new ArrayList<>();
        if (!AsyncConfig.enableAffinityRouting.getValue()) {
            addSlices(tasks, entities, ENTITY_TICK_COST, action);
            return tasks;
        }
        Long2ObjectOpenHashMap<List<Entity>> sections = new Long2ObjectOpenHashMap<>();
        for (Entity entity : entities) {
            long key = (long) (entity.chunkPosition().x >> 2) << 32
                    | (entity.chunkPosition().z >> 2) & 0xffffffffL;
            sections.computeIfAbsent(key, ignored -> new ArrayList<>()).add(entity);
        }
        long[] keys = sections.keySet().toLongArray();
        Arrays.sort(keys);
        int size = ENTITY_TICK_COST.chunkSize(entities.size());
        List<Entity> pending = new ArrayList<>(size);
        for (long key : keys) {
            for (Entity entity : sections.get(key)) {
                pending.add(entity);
                if (pending.size() == size) {
                    emit(tasks, pending, ENTITY_TICK_COST, action);
                    pending = new ArrayList<>(size);
                }
            }
        }
        if (!pending.isEmpty()) emit(tasks, pending, ENTITY_TICK_COST, action);
        return tasks;
    }

    private static <T> void emit(List<Runnable> tasks, List<T> items, CostModel cost, Consumer<T> action) {
        tasks.add(() -> cost.measure(items.size(), () -> items.forEach(action)));
    }

    private static <T> void addSlices(List<Runnable> tasks, List<T> items, CostModel cost, Consumer<T> action) {
        int size = cost.chunkSize(items.size());
        for (int i = 0; i < items.size(); i += size) {
            emit(tasks, items.subList(i, Math.min(i + size, items.size())), cost, action);
        }
    }

    public static <T> void forEachParallel(List<T> items, Consumer<T> action) {
        forEachParallel(items, action, GENERIC_COST);
    }

    public static <T> void forEachParallel(List<T> items, Consumer<T> action, CostModel cost) {
        if (items.isEmpty()) return;
        // A nested worker batch must not wait for another slot in its own fixed pool.
        if (!canParallelize() || isServerExecutionThread() || cost.shouldRunSequentially(items.size())) {
            cost.measure(items.size(), () -> items.forEach(action));
            return;
        }
        List<Runnable> tasks = new ArrayList<>();
        addSlices(tasks, items, cost, action);
        finishParallel(submitParallel(tasks));
    }

    private static boolean canParallelize() {
        return !isShuttingDown && !AsyncConfig.disabled.getValue() && tickPool != null && !tickPool.isShutdown();
    }

    private static ParallelBatch submitParallel(List<Runnable> tasks) {
        ParallelBatch batch = new ParallelBatch(new ConcurrentLinkedQueue<>(), new CountDownLatch(tasks.size()),
                new AtomicReference<>());
        for (Runnable task : tasks) {
            batch.queue().add(() -> {
                try {
                    task.run();
                } catch (RuntimeException | Error failure) {
                    batch.failure().compareAndSet(null, failure);
                } finally {
                    batch.done().countDown();
                }
            });
        }
        int workers = Math.min(getPoolSize(), tasks.size());
        lastWorkerCount.set(workers);
        for (int i = 0; i < workers; i++) {
            try {
                tickPool.execute(() -> drain(batch));
            } catch (RejectedExecutionException rejected) {
                // The caller owns the same queue and will drain every unclaimed task.
                break;
            }
        }
        return batch;
    }

    private static void drain(ParallelBatch batch) {
        Runnable task;
        while ((task = batch.queue().poll()) != null) task.run();
    }

    private static void finishParallel(ParallelBatch batch) {
        Runnable task;
        while ((task = batch.queue().poll()) != null) {
            task.run();
            pumpMainThreadTasks(batch);
        }
        long start = System.nanoTime();
        long threshold = TimeUnit.MILLISECONDS.toNanos(Math.max(50, AsyncConfig.staleTaskTimeoutMs.getValue()));
        boolean warned = false;
        boolean interrupted = false;
        while (batch.done().getCount() > 0) {
            pumpMainThreadTasks(batch);
            if (!warned && System.nanoTime() - start > threshold) {
                warned = true;
                totalTimeoutWarnings.increment();
                LOGGER.warn("Async batch exceeded {}ms; waiting for {} tasks before advancing the world",
                        AsyncConfig.staleTaskTimeoutMs.getValue(), batch.done().getCount());
            }
            try {
                batch.done().await(200, TimeUnit.MICROSECONDS);
            } catch (InterruptedException exception) {
                interrupted = true;
            }
        }
        Runnable callback;
        while ((callback = entityCallbacks.poll()) != null) {
            try {
                callback.run();
            } catch (RuntimeException | Error failure) {
                batch.failure().compareAndSet(null, failure);
            }
        }
        if (interrupted) Thread.currentThread().interrupt();
        Throwable failure = batch.failure().get();
        if (failure instanceof Error error) throw error;
        if (failure instanceof RuntimeException exception) throw exception;
    }

    private static void pumpMainThreadTasks(ParallelBatch batch) {
        try {
            pumpMainThreadTasks();
        } catch (RuntimeException | Error failure) {
            // Even a main-thread task failure cannot allow workers into the next world phase.
            batch.failure().compareAndSet(null, failure);
        }
    }

    public static void queueEntityCallback(Runnable callback) {
        if (isServerExecutionThread()) entityCallbacks.add(callback);
        else callback.run();
    }

    private static void pumpMainThreadTasks() {
        if (server != null && server.isSameThread()) {
            for (ServerLevel level : server.getAllLevels()) {
                level.getChunkSource().mainThreadProcessor.pollTask();
            }
        }
    }

    public static boolean shouldTickSynchronously(Entity entity) {
        if (isShuttingDown || entity.level().isClientSide() || AsyncConfig.disabled.getValue()
                || entitySupportsAsyncApi(entity) || entity instanceof Projectile || entity instanceof AbstractMinecart
                || entity instanceof Player || entity.isVehicle() || BLOCKED_ENTITIES.contains(entity.getClass())
                || AsyncConfig.isEntitySynchronized(EntityType.getKey(entity.getType()))) return true;
        if (AsyncConfig.enableCircuitBreaker.getValue() && !circuitBreaker.shouldTickAsync(entity.getType())) return true;
        Integer until = portalSyncUntil.get(entity.getUUID());
        if (until != null) {
            if (server != null && server.getTickCount() - until < 0) return true;
            portalSyncUntil.remove(entity.getUUID(), until);
        }
        return isPortalTickRequired(entity);
    }

    public static boolean entitySupportsAsyncApi(Entity entity) {
        // Several entity types can share a class, so only the annotation is cached by class.
        return !"minecraft".equals(EntityType.getKey(entity.getType()).getNamespace())
                && !asyncApiCache.computeIfAbsent(entity.getClass(), type -> type.isAnnotationPresent(AsyncCompatible.class)
                        || type.isAnnotationPresent(com.axalotl.async.api.utils.AsyncCompatible.class));
    }

    private static boolean isPortalTickRequired(Entity entity) {
        return entity instanceof EntityAccessor accessor && accessor.isInsidePortal();
    }

    private static void tickEntity(ServerLevel world, Entity entity) {
        boolean recording = TickStats.isRecording();
        long start = recording ? System.nanoTime() : 0L;
        boolean async = isServerExecutionThread();
        currentEntities.incrementAndGet();
        EntityType<?> type = entity.getType();
        try {
            world.tickNonPassenger(entity);
            if (async) {
                totalAsyncTicks.increment();
                if (AsyncConfig.enableCircuitBreaker.getValue()) circuitBreaker.recordSuccess(type);
            }
        } catch (Exception failure) {
            if (!async) throw failure;
            totalAsyncFailures.increment();
            if (AsyncConfig.enableCircuitBreaker.getValue()) circuitBreaker.recordFailure(type, failure);
            LOGGER.error("Async entity tick failed for {} ({}); it is not replayed this tick", type, entity.getUUID(), failure);
        } finally {
            currentEntities.decrementAndGet();
            if (recording) {
                long elapsed = System.nanoTime() - start;
                (async ? TickStats.ASYNC_TICK_TIME_NS : TickStats.TICK_TIME_NS)
                        .computeIfAbsent(type, ignored -> new LongAdder()).add(elapsed);
                (async ? TickStats.ASYNC_TICK_COUNT : TickStats.TICK_COUNT)
                        .computeIfAbsent(type, ignored -> new LongAdder()).increment();
            }
        }
    }

    public static Object getEntityAddLock() { return ENTITY_ADD_LOCK; }
    public static MinecraftServer getServer() { return server; }
    public static void setServer(MinecraftServer newServer) { server = newServer; }

    public static void stop() {
        if (isShuttingDown) return;
        isShuttingDown = true;
        if (tickPool != null) {
            tickPool.shutdown();
            boolean interrupted = false;
            while (!tickPool.isTerminated()) {
                pumpMainThreadTasks();
                try {
                    tickPool.awaitTermination(1, TimeUnit.MILLISECONDS);
                } catch (InterruptedException exception) {
                    interrupted = true;
                }
            }
            if (interrupted) Thread.currentThread().interrupt();
            tickPool = null;
        }
        AsyncConfig.clearCaches();
        asyncApiCache.clear();
        portalSyncUntil.clear();
        mcThreadTracker.clear();
        entityCallbacks.clear();
        circuitBreaker.reset();
        totalAsyncTicks.reset();
        totalAsyncFailures.reset();
        totalTimeoutWarnings.reset();
        lastWorkerCount.set(0);
        ENTITY_TICK_COST.reset();
        DESPAWN_COST.reset();
        GENERIC_COST.reset();
        SPAWN_COST.reset();
        GpuEntityModule.shutdown();
        TickStats.resetEntityTickStats();
        server = null;
    }

    private record ParallelBatch(ConcurrentLinkedQueue<Runnable> queue, CountDownLatch done,
                                 AtomicReference<Throwable> failure) {}

    public static final class CostModel {
        private final double initialCost;
        private volatile double nanosPerItem;
        private final LongAdder nanos = new LongAdder();
        private final LongAdder items = new LongAdder();

        CostModel(double initialCost) {
            this.initialCost = initialCost;
            nanosPerItem = initialCost;
        }

        public void measure(int count, Runnable task) {
            if (count == 0) return;
            long start = System.nanoTime();
            try {
                task.run();
            } finally {
                nanos.add(System.nanoTime() - start);
                items.add(count);
            }
        }

        private synchronized void fold() {
            long count = items.sumThenReset();
            if (count > 0) {
                double sample = Math.max(100, Math.min(2_000_000, (double) nanos.sumThenReset() / count));
                nanosPerItem += (sample - nanosPerItem) * 0.25;
            }
        }

        public boolean shouldRunSequentially(int count) {
            fold();
            return count * nanosPerItem < 500_000;
        }

        public int chunkSize(int count) {
            fold();
            int fairShare = Math.max(1, (count + getPoolSize()) / (getPoolSize() + 1));
            int byCost = Math.max(1, (int) (250_000 / nanosPerItem));
            return Math.min(fairShare, Math.min(byCost, Math.max(1, AsyncConfig.entitiesPerWorker.getValue())));
        }

        private void reset() {
            nanos.reset();
            items.reset();
            nanosPerItem = initialCost;
        }
    }
}
