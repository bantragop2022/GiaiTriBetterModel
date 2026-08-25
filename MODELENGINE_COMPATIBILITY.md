# GiaiTriBetterModel compatibility audit

This document records the Phase 1 audit of the open-source BetterModel `v3`
codebase. ModelEngine documentation and observable public behaviour are only a
terminology reference; no proprietary binaries or implementation were used.

## Runtime and architecture

| Area | Implementation |
| --- | --- |
| API | Java contracts, render data, animation, bones, trackers, events, NMS abstractions, packs, profiles, and domain utilities in `api`; Bukkit and mod adapters are separate API submodules. |
| Core | Kotlin-first import/build pipeline, resource-pack generation, scripts, managers, and reload orchestration in `core`. |
| NMS | Isolated packet/display/hitbox implementations for each supported server revision in `nms/*`. |
| Platform | Paper and Spigot plugin entry points plus a Fabric mod entry point in `platform/*`. |
| Purpur | Optional Purpur tick-loop hook in `purpur`; it does not contain model business logic. |
| Test plugin | API integration fixtures and manual animation/model exercises in `test-plugin`; Fabric has an additional testmod source set. |
| Resource-pack/model engine | `.bbmodel`/`.ajmodel` import, mesh and texture processing, modern item-model JSON, pack obfuscation, zipping, and optional pack hosting are implemented by the core model/pack pipeline. |
| Animation | Parsed keyframes, Molang evaluation, loop modes, interpolation, per-player animation, priorities, override rules, and lerp-in/lerp-out are in the API tracker/animation layer. |
| Entity tracking | A global UUID/entity-ID registry owns per-entity trackers, displays, hitboxes, viewers, persistence, reload, despawn, and close operations. Platform listeners handle unload, removal, death, world change, and player quit. |
| MythicMobs | Optional Bukkit integration registers mechanics, a condition, a targeter, and animation timeline skill dispatch without making MythicMobs a hard dependency. |

### Supported Minecraft and Java versions

The Bukkit runtime dispatch table has concrete NMS modules for Paper/Spigot
**1.21.4, 1.21.5, 1.21.6–1.21.8, 1.21.9–1.21.10, 1.21.11, 26.1.x, and
26.2**. Paper 1.21.8 therefore has an explicit `v1_21_R5` implementation. The
current upstream build uses the Java 25 toolchain and Minecraft 26.2; Phase 1
does not lower the bytecode/toolchain to Java 21 because the current source and
latest platform dependencies are Java-25 based. Keeping all upstream features
and supported revisions takes precedence over an incompatible toolchain
downgrade.

Build/runtime verification status, the explicit exclusion of 1.21.0–1.21.3,
client/ViaVersion scope, resource-pack format limits, and the cross-version
acceptance checklist are maintained in [`VERSION_COMPATIBILITY.md`](VERSION_COMPATIBILITY.md).

## Feature matrix

`FULL` means a first-class implementation exists in this codebase. `PARTIAL`
means the core behaviour exists but does not cover every similarly named
ModelEngine convention. `MISSING` means no matching implementation was found.
`NOT_APPLICABLE` is reserved for concepts that do not map to this server-side
renderer.

| Feature | Status | Source audit result |
| --- | --- | --- |
| model | **FULL** | Named general renderers are imported from `.bbmodel` and `.ajmodel`, packed, exposed through the API, and attachable to entities. |
| state | **FULL** | Tracker animation APIs and the MythicMobs `state`/`animation` mechanic play named animations. |
| defaultstate | **FULL** | Default animation state is stored by trackers and exposed through `defaultstate`/`defaultanimation`. |
| animation | **FULL** | Keyframes, loop types, speed, predicates, scripts, interpolation, and tracker playback are implemented. |
| animation priority | **FULL** | `AnimationModifier.priority` participates in animation selection. |
| animation blending/transition | **FULL** | Lerp-in and lerp-out tick values plus override state provide transitions between animation results. |
| runtime scale | **FULL** | `ModelScaler` supports entity/default scaling and multiplication at runtime. |
| bone scale/rotation/position | **FULL** | Bone transforms contain animated position, quaternion rotation, and scale and are sent to displays. |
| bone/part visibility | **FULL** | Part visibility scripts and the MythicMobs `partvisibility` mechanic update rendered bones. |
| tint | **FULL** | Tint scripts, damage tint, renderer tint data, and MythicMobs tint/color are present. |
| glow | **FULL** | Entity glow synchronization and MythicMobs glow/glowbone are present. |
| brightness | **FULL** | Display brightness scripts and MythicMobs brightness/light are present. |
| texture variants | **PARTIAL** | Runtime part/item remapping (`changepart`, `remapmodel`) is supported, but no separate ModelEngine-style variant registry or variant command was found. |
| submodel | **PARTIAL** | Nested bone groups and multiple model trackers per entity are supported; no dedicated named submodel contract was found. |
| model attachment | **FULL** | Multiple named trackers can be attached to one entity and paired through the MythicMobs integration. |
| mount | **FULL** | Mountable hitboxes, mount controllers, mount/dismount events, and MythicMobs mechanics are implemented. |
| swap entity | **MISSING** | No atomic base-entity replacement operation preserving a tracker was found. |
| root motion | **MISSING** | Position keyframes transform model bones; no animation-driven movement of the source entity was found. |
| modelplayerskin | **FULL** | Player models resolve Mojang/external profiles, slim skin geometry, capes, caching, fallback data, and SkinsRestorer integration. |
| custom hitbox | **FULL** | Blueprint hitbox metadata creates interaction entities with damage, interaction, mount, and lifecycle handling. |
| MythicMobs mechanics | **FULL** | Model, animation/default animation, limb animation, visibility, hitbox binding, part changes, tint, brightness, enchant, billboard, glow, mount/dismount, rotation, remap, and pair mechanics are registered, with a passenger condition and model-part targeter. |

## Phase 1 operational compatibility

Phase 1 adds `/gbm` as a stable command alias on Bukkit and Fabric. The new
`/gbm models` command reports every loaded model key, and `/gbm stats` reports
loaded general/player models, entity registries, active trackers, and packet
displays. These are live snapshots and are useful for finding failed imports,
duplicate trackers, or displays left behind after reloads.

The importer derives concurrent-map capacity from the files discovered and
does not impose a model-count limit. It processes every uniquely named input;
duplicate names are reported and deterministically resolved rather than
silently creating duplicate renderers. The relevant acceptance set is **1,
12, 13, 30, and 50 models**, and the same path supports 100+ unique models when
server memory, build time, resource-pack size, and client resources permit.

Reload clears and rebuilds renderer maps, then reloads each entity registry.
Shutdown closes every registry after persistence; entity/chunk unload, removal,
death, world changes, and player quit despawn or close their owned state. This
preserves the existing ownership model and avoids a second global registry or
display cache that could leak or duplicate packet entities.

## Phase 1 verification checklist

- [x] Paper 1.21.8 remains explicitly implemented (`v1_21_R5`); runtime verification is still pending.
- [x] No fixed maximum model count was introduced.
- [x] Loader acceptance counts documented: 1, 12, 13, 30, and 50 unique models.
- [x] Existing model/state/animation and MythicMobs paths remain intact.
- [x] Live model and tracker diagnostics are available through `/gbm`.
- [x] CI builds the complete project and uploads Paper, Spigot, and Fabric JARs.
