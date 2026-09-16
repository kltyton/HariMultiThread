# Attribution and implementation references

TickWeave derives from HariMultiThread and Async. Existing contributor attribution is retained in mod metadata: HariMT, Axalotl, Alchemy, Bliss, FurryMileon, Grider, and jediminer543. The repository LICENSE contains GPL version 3; metadata now agrees with that file.

- [HariMultiThread](https://github.com/JustHari01/HariMultiThread): existing project and compatibility work.
- [Async 1.20.1](https://github.com/Bliss-tbh/Async-1.20.1): GPL-3.0 source lineage; the 1.20.1 version `0.1.8_alpha.1` informed this port. Adaptive batching, last-chunk caching, random-tick batching and optional compatibility adaptations are based on this source lineage.
- [Async, revision 43725f705d226395d97a4842604331f1d7a47694](https://github.com/AxalotLDev/Async/blob/43725f705d226395d97a4842604331f1d7a47694/common/src/main/java/com/axalotl/async/common/ParallelProcessor.java): GPL-3.0. Reviewed adaptive cost measurement, spatial grouping, caller assistance, and completion waits. Adaptations retain Java 17 / Minecraft 1.20.1 APIs and add bounded submission and failure-safe phase completion.
- [MCMTFabric, revision 03e39d299dd02ed4eb8061341cd6e7f348cfb6ee](https://github.com/himekifee/MCMTFabric/blob/03e39d299dd02ed4eb8061341cd6e7f348cfb6ee/src/main/java/net/himeki/mcmtfabric/ParallelProcessor.java): GPL-3.0. Reviewed phase ordering and `finally` completion signalling. No region executor implementation copied.
- [Folia, revision 1f768cb641e00ee8710db5c42234893fdd0a9901](https://github.com/PaperMC/Folia/blob/1f768cb641e00ee8710db5c42234893fdd0a9901/folia-server/paper-patches/features/0001-Region-Threading-Base.patch): reviewed thread ownership checks and region boundaries. Design reference only; no Folia patch code copied. TickWeave is not a region-threaded server.
- [JMT-MCMT](https://github.com/jediminer543/JMT-MCMT): acknowledged upstream ancestor of MCMTFabric and Async.

The Async 1.20.1 port is the version-matched implementation reference. Other repositories inform design; their performance figures and platform compatibility are not TickWeave test results.
