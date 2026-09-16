# TickWeave

<p align="center"><img src="common/src/main/resources/tickweave.png" alt="TickWeave" width="160"></p>

[English](README.md) | [简体中文](README.zh-CN.md)

TickWeave 将 Minecraft 的实体 tick 分配给多个 CPU 工作线程，旨在降低实体密集场景的服务器 tick 耗时。面向 **Minecraft 1.20.1**，提供 **Forge 与 Fabric** 两个版本，支持专用服务器和单人游戏的内置服务器。

## 安装

使用 Java 17，并选择与加载器匹配的文件：

| 加载器 | 运行环境 | 发行文件 |
| --- | --- | --- |
| Forge | Forge 47.x，构建版本为 47.4.16 | `tickweave-forge-1.20.1-2.1.1-all.jar` |
| Fabric | Fabric Loader 0.18.2+，以及对应 1.20.1 的 Fabric API | `tickweave-fabric-1.20.1-2.1.1.jar` |

将 JAR 放入 `mods`。专用服务器的玩家客户端不必安装；单人游戏需要在客户端安装。更换 tick 处理模组前备份存档。安装时移除旧版 TickWeave、HariMultiThread 或 Async，不要同时运行这些实现。

## 功能

- 根据实体实测耗时自适应调整任务大小，按空间位置分组，主线程参与处理。
- 有界线程池，各处理阶段等待本阶段任务完成。
- 工作线程缓存区块查询，缺失区块交给服务器执行器处理。
- 可选并行自然刷怪、同步实体名单和异常熔断。
- 实时查看服务器 tick 耗时、工作线程活动和实体开销。
- 实验性随机 tick 批处理默认关闭；方块和流体回调仍在服务器主线程执行。

玩家、乘客与载具组以及部分敏感实体保持同步。模组实体需要显式兼容注解才会进入异步 tick。并行处理会改变实体执行顺序，收益取决于世界、CPU 和整合包，不保证固定提升。实体调度不使用 GPU 计算。

## 配置

配置文件为 `config/tickweave.toml`。Forge 将以下键放在 `["Async Config"]` 下；Fabric 使用顶层键。从 `harimt.toml` 迁移时，将设置值复制到同一加载器新生成的配置中。两种加载器的配置文件不能直接互换。

| 配置项 | 默认值 | 用途 |
| --- | --- | --- |
| `disabled` | `false` | 关闭异步处理 |
| `paraMax` | `-1` | 工作线程数，-1 自动选择，上限为可用处理器数；修改后重启 |
| `enableAsyncSpawn` | `true` | 并行自然刷怪 |
| `enableAsyncRandomTicks` | `false` | 实验性随机 tick 准备阶段 |
| `enableAffinityRouting` | `true` | 将附近实体分到相邻批次 |
| `enableCircuitBreaker` | `true` | 将持续异常的实体类型退回同步处理 |
| `entitiesPerWorker` | `25` | 每个任务的实体数上限；Forge 范围为 5–200 |
| `staleTaskTimeoutMs` | `200` | 慢批次告警阈值，单位毫秒；不会取消正在运行的 tick |
| `synchronizedEntities` | 内置名单 | 必须同步的实体 ID 或 `namespace:*` 通配规则 |

管理命令：

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

`stats entity 10 100` 采集 100 个服务器 tick，列出开销最高的十种实体。实体耗时会在线程间重叠，不能当作墙钟时间节省量。`Completed Worker Entity Ticks` 表示工作线程实际完成的实体 tick 数；线程池初始化日志本身不能证明并行处理生效。

## 兼容性

不要与 Moonrise 或 Cupboard 同时使用。Carpet 的 `lagFreeSpawning` 规则与并行刷怪冲突，使用该规则时请关闭并行刷怪。兼容补丁只在对应模组安装时加载。2.1.1 更新了 Forge SophisticatedCore 的 `SlotValueMap` 兼容处理，对应 Core 1.3.21.1676 / Backpacks 3.24.35.1675。

请先用整合包和世界副本验证。发生实体相关问题时，可将其 ID 或命名空间加入 `synchronizedEntities`。通过 [GitHub Issues](https://github.com/kltyton/HariMultiThread/issues) 提交加载器、模组版本、`latest.log`、崩溃报告和复现步骤。

## 构建

使用 Java 17 和仓库内的 Gradle Wrapper：

```sh
./gradlew :forge:build :fabric:build
./gradlew :forge:runClient
./gradlew :fabric:runClient
```

Windows 使用 `gradlew.bat`。Forge 发行包位于 `forge/build/libs`，选择 `-all.jar`；Fabric 位于 `fabric/build/libs`，选择重映射后的 JAR，不使用源码包。现有 `common/libs/Harium-1.0.0.jar` 仅供可选集成编译，不打包进发行文件。

## 致谢与许可证

本项目衍生自 HariMultiThread 和 Async。感谢 HariMT、Axalotl、Alchemy、Bliss、FurryMileon、Grider、jediminer543 的上游工作，以及 PaperMC / Folia 的线程所有权设计参考。TickWeave 不是 Folia 服务器，也不代表上游团队背书。

采用 [GPL-3.0](LICENSE)。详见[第三方来源说明](THIRD_PARTY_NOTICES.md)和[更新记录](CHANGELOG.md)。
