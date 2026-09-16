# Changelog

## 2.1.1 — Minecraft 1.20.1

- Fix startup with SophisticatedCore 1.3.21.1676 by adapting inventory hooks to `SlotValueMap`. Protect its paired indexes and return detached slot snapshots.
- Enable the shared scheduling improvements on Fabric, including platform service registration and dedicated-server dependencies.
- Align Fabric scheduling configuration with Forge and reload entity rules when a server starts. Preserve existing configuration files on load errors.
- Package the Forge Mixin configurations and generated refmap in release JARs.
- Introduce adaptive entity batches, bounded submission, main-thread assistance and phase completion barriers.
- Cache worker chunk lookups and commit entity tracking callbacks on the main thread.
- Keep random-tick batching experimental and disabled by default.
- Rename the mod to TickWeave (`tickweave`), use `/tickweave` and `config/tickweave.toml`. Old configurations require manual migration; remove the old mod JAR before upgrading.
