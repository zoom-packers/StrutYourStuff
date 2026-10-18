# Porting notes: Strut Your Stuff (Create Fly, Fabric 26.2) → NeoForge 26.3

Written 2026-10-18. Checked against Minecraft 26.3, NeoForge 26.3.0.89, Java 25 and Zoompack 5's NeoForge port of
Create Fly (`create-fly-neoforge-6.0.9+neoforge-26.3`, ZP5 `mods/create-fly-neoforge`, commit `8da5b8c6`).

Branch `zp5/26.3` of [chaevsfe/StrutYourStuff](https://github.com/chaevsfe/StrutYourStuff), starting from
`fabric-26.2` at `304530b` (1.3.1+fly-mc26.2). Strut Your Stuff is the girder strut library that
[Bits 'n' Bobs](https://github.com/chaevsfe/Create-Bits-n-Bobs) needs; the Fabric jar nests it, this port ships it as
its own jar (`struts`, `StrutYourStuff-1.3.1+neoforge-26.3-zp5.N.jar`).

Licence: MIT (see `LICENSE` and `NOTICE`). Mod id `struts`, package `com.cake.struts`.

## Why a native port, not Sinytra Connector

The Fabric jar is built against Create Fly 26.2 (`com.zurrtum.create`), which only exists on 26.3 as ZP5's NeoForge
port, and against 26.2 vanilla. Running it under Connector would still need a recompile for 26.3 and Connector-side
fixes for Create's NeoForge-only entrypoints (the `create_plugin` entrypoint is a `ServiceLoader` service in the port).
The Fabric surface is small (22 imports, all events, networking and one reload listener), so a native port is less
work and needs no Connector or Forgified Fabric API at runtime.

## Build

- ModDevGradle 2.0.148, NeoForge 26.3.0.89, Gradle 9.2.1 (the wrapper ZP5 uses), Java 25.
- Create is a compile and dev-run dependency from `libs/` (gitignored), snapshotted from ZP5's Create port:
  - `libs/create-fly-neoforge-6.0.9+neoforge-26.3.jar` (`mods/create-fly-neoforge/build/libs`)
  - `libs/create-accesstransformer.cfg` (the jar's `META-INF/accesstransformer.cfg`)
  - `libs/create-interfaces.json` (`mods/create-fly-neoforge/interfaces.json`)
  Refresh all three together when the Create port changes.
- `./gradlew build` gives `build/libs/StrutYourStuff-<version>+neoforge-26.3-<port_build>.jar`. `port_build` in
  `gradle.properties` is bumped for every released jar.
- The Fabric files stay in the tree and are excluded in `build.gradle.kts` (`com/cake/struts/fabric/**`,
  `fabric.mod.json`), so rebases onto upstream don't conflict on them.

## Fabric → NeoForge

| Fabric | NeoForge |
|---|---|
| `main` / `client` entrypoints (`fabric/*`) | `@Mod` classes `neoforge/StrutsNeoForge` and `neoforge/client/StrutsNeoForgeClient` |
| `create_plugin` entrypoint | `META-INF/services/com.zurrtum.create.api.registry.CreateRegisterPlugin` → `neoforge/StrutsCreatePlugin` (Create calls it during bootstrap) |
| Data components registered at init | `RegisterEvent` for `DATA_COMPONENT_TYPE` (NeoForge only opens registries while it posts `RegisterEvent`) |
| `PayloadTypeRegistry` + `ServerPlayNetworking` | `RegisterPayloadHandlersEvent` (`StrutPackets`); `ClientPlayNetworking.send` → `ClientPacketDistributor.sendToServer` |
| `ServerLevelEvents.UNLOAD`, `ServerChunkEvents.CHUNK_UNLOAD`, `ServerTickEvents.END_LEVEL_TICK` | `LevelEvent.Unload`, `ChunkEvent.Unload`, `LevelTickEvent.Post` (server levels only) |
| `PlayerBlockBreakEvents.BEFORE` | `BreakBlockEvent` (server side only, as before) |
| `UseBlockCallback` in an early phase | `PlayerInteractEvent.RightClickBlock` at `HIGHEST` priority |
| Client tick, disconnect, level change, chunk unload | `ClientTickEvent.Pre/Post`, `ClientPlayerNetworkEvent.LoggingOut`, client `LevelEvent.Load`, `ChunkEvent.Unload` |
| `LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN` | `SubmitCustomGeometryEvent`, through a `LevelRenderContext` record with the accessors Fabric's had (`neoforge/client/LevelRenderContext`) |
| `LevelRenderEvents.BEFORE_BLOCK_OUTLINE` returning false | cancel `ExtractBlockOutlineRenderStateEvent` |
| `ClientPreAttackCallback` | `InputEvent.InteractionKeyMappingTriggered` (attack), cancelled without a swing |
| `IdentifiableResourceReloadListener` (client) | `AddClientReloadListenersEvent` |
| `@Environment(EnvType.CLIENT)` | removed (NeoForge does not strip classes; client code is only reached from the client entrypoint) |

## Minecraft 26.2 → 26.3

- `PushReaction`: `NORMAL` → `PUSH_PULL`, `DESTROY` → `POPPED`.
- `PoseStack#mulPose(Quaternionf)` → `rotate(Quaternionfc)`.
- `LivingEntity#swing(hand)` → `swing(hand, SwingAnimation.DEFAULT, false)`.
- `Block#spawnDestroyParticles(level, player, pos, state)` → `spawnDestroyByEntityParticles(level, entity, pos, state)`.
- `BakedQuad.MaterialInfo#shade()` → nullable `shadeDirectionOverride()`; shaded means no override.
- Create's access transformer makes `BlockBehaviour#rotate` public, so overrides must be public.

`tools/zp5/` holds the scripts used, copied from ZP5's Create port: `remap_moves.py` (26.3 class moves; nothing to do
here), `data_26_3.py` (data and model lint; 0 legacy constructs) and `check_mixins.py` (static mixin check).

## Status

Verified 2026-10-18:
- `./gradlew build` passes.
- `tools/zp5/check_mixins.py <patched merged MC jar> <NeoForge universal jar> libs/create-fly-neoforge-*.jar`:
  0 problems (the only mixin, `MinecraftMixin`, is client-side).
- Loaded as a mod in Bits 'n' Bobs' `runGameTestServer` (with the Create port): its Create plugin registers the strut
  structure block, the world loads, all 4 game tests pass, and the log has no warnings or errors.

Not verified: anything client-side (strut placement, meshes, outlines, Flywheel visual). That needs `runClient`, which
opens a game window, so it waits for the user's OK.
