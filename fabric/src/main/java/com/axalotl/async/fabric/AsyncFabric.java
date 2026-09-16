package com.axalotl.async.fabric;

import com.axalotl.async.common.AsyncCommon;
import com.axalotl.async.common.ParallelProcessor;
import com.axalotl.async.common.commands.AsyncCommand;
import com.axalotl.async.common.commands.StatsCommand;
import com.axalotl.async.common.config.AsyncConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axalotl.async.fabric.config.AsyncConfigFabric;

public class AsyncFabric extends AsyncCommon implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger(AsyncFabric.class);
    public static boolean LITHIUM = FabricLoader.getInstance().isModLoaded("lithium");
    public static final boolean VMP = FabricLoader.getInstance().isModLoaded("vmp");

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing TickWeave...");
        AsyncConfigFabric.init();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            LOGGER.info("TickWeave setting up thread-pool...");
            this.initialize();
            AsyncConfigFabric.loadConfig();
            StatsCommand.runStatsThread();
            ParallelProcessor.setServer(server);
            ParallelProcessor.setupThreadPool(AsyncConfig.getParallelism(), this.getClass());
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> AsyncCommand.register(dispatcher));

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("Shutting down TickWeave thread pool...");
            StatsCommand.shutdown();
            ParallelProcessor.stop();
        });

        LOGGER.info("TickWeave initialized successfully");
    }
}
