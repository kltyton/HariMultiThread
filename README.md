# TickWeave

[English](README.md) | [简体中文](README.zh-CN.md)

TickWeave spreads Minecraft entity ticking across CPU workers to reduce server tick time in entity-heavy worlds. Built for **Minecraft 1.20.1**, with **Forge and Fabric** editions. It works on dedicated servers and the integrated server in single-player.

## Installation

Use Java 17 and choose the file matching your loader:

| Loader | Runtime | Release file |
| --- | --- | --- |
| Forge | Forge 47.x; built against 47.4.16 | `tickweave-forge-1.20.1-2.1.1-all.jar` |
| Fabric | Fabric Loader 0.18.2+ and Fabric API for 1.20.1 | `tickweave-fabric-1.20.1-2.1.1.jar` |

Place the JAR in `mods`. Dedicated-server players do not need TickWeave on their clients. For single-player, install it on the client. Back up your world before changing tick-processing mods. Remove older TickWeave, HariMultiThread or Async JARs before installing; these implementations must not run together.

## Features

- Adaptive task sizes based on measured entity cost, with spatial grouping and main-thread assistance.
- A bounded worker pool and completion barriers between processing phases.
- Worker-local chunk lookup caching; missing chunks are requested through the server executor.
- Optional parallel natural spawning, configurable synchronous entity types and a failure circuit breaker.
- Live statistics for server tick time, worker activity and entity costs.
- Experimental random-tick batching, disabled by default. Block and fluid callbacks remain on the server thread.

Players, passenger/vehicle groups and selected sensitive entity types stay synchronous. Modded entities require an explicit compatibility annotation to tick asynchronously. Parallel execution changes entity ordering: results depend on the world, CPU and modpack, and speedups are not guaranteed. GPU computation is not used by the entity scheduler.

## Configuration

The file is `config/tickweave.toml`. Forge stores these keys under `["Async Config"]`; Fabric uses top-level keys. When migrating from `harimt.toml`, copy values into the configuration generated for the same loader. Configuration files are not interchangeable between loaders.

| Key | Default | Purpose |
| --- | --- | --- |
| `disabled` | `false` | Disable asynchronous processing |
| `paraMax` | `-1` | Worker count; automatic at -1, capped by available processors; restart after changing |
| `enableAsyncSpawn` | `true` | Parallel natural spawning |
| `enableAsyncRandomTicks` | `false` | Experimental random-tick preparation |
| `enableAffinityRouting` | `true` | Group nearby entities in batches |
| `enableCircuitBreaker` | `true` | Return repeatedly failing entity types to synchronous ticking |
| `entitiesPerWorker` | `25` | Maximum entities per task; Forge accepts 5–200 |
| `staleTaskTimeoutMs` | `200` | Slow-batch warning threshold in milliseconds; does not cancel running ticks |
| `synchronizedEntities` | Built-in list | Entity IDs or `namespace:*` patterns that must stay synchronous |

Administrative commands:

```text
/tickweave stats
/tickweave stats entity 10 100
/tickweave config toggle
/tickweave config reload
/tickweave config setAsyncEntitySpawn false
/tickweave config setAsyncRandomTicks false
/tickweave config synchronizedEntities add minecraft:zombie
/tickweave config synchronizedEntities add examplemod:*
/tickweave config synchronizedEntities remove minecraft:zombie
```

`stats entity 10 100` samples 100 server ticks and lists the ten most expensive entity types. Its entity-time totals overlap across threads and are not wall-clock savings. `Completed Worker Entity Ticks` confirms actual worker execution; thread-pool startup alone does not.

## Compatibility

Do not combine with Moonrise or Cupboard. Carpet's `lagFreeSpawning` rule conflicts with parallel spawning; disable parallel spawning when using that rule. Existing compatibility hooks are conditional on the corresponding mods being installed. Version 2.1.1 updates the Forge SophisticatedCore hooks for `SlotValueMap` (Core 1.3.21.1676 / Backpacks 3.24.35.1675).

Test a copy of your modpack and world. For problematic entities, add their ID or namespace to `synchronizedEntities`. Report the loader, mod versions, `latest.log`, crash report and reproduction steps through [GitHub Issues](https://github.com/kltyton/HariMultiThread/issues).

## Building

Use the included Gradle wrapper with Java 17:

```sh
./gradlew :forge:build :fabric:build
./gradlew :forge:runClient
./gradlew :fabric:runClient
```

On Windows use `gradlew.bat`. Release files are in `forge/build/libs` (use the `-all.jar`) and `fabric/build/libs` (use the remapped JAR, not sources). The existing `common/libs/Harium-1.0.0.jar` is a compile-only integration dependency, not bundled into the releases.

## Credits and license

Derived from HariMultiThread and Async. Thanks to HariMT, Axalotl, Alchemy, Bliss, FurryMileon, Grider and jediminer543 for their upstream work; PaperMC / Folia informed thread-ownership design research. TickWeave is not a Folia server and does not imply upstream endorsement.

Licensed under [GPL-3.0](LICENSE). See [third-party notices](THIRD_PARTY_NOTICES.md) and the [changelog](CHANGELOG.md).
