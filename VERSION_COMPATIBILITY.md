# GiaiTriBetterModel version compatibility

This matrix separates **server implementation compatibility**, **client
protocol compatibility**, and **resource-pack compatibility**. A source adapter
is not marked `SUPPORTED` until its build and runtime checks have completed.
The current container could not download Gradle/plugin artifacts, so the rows
below deliberately use `IMPLEMENTED_NOT_VERIFIED` rather than claiming a pass.

## Server matrix

| Server version | Status | Java | Model rendering | Animation | MythicMobs | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| Paper/Purpur 1.21.0–1.21.3 | **UNSUPPORTED** | Not tested | Not tested | Not tested | Not tested | No bundled NMS adapter; plugin now stops with a clear message rather than loading latest NMS classes. |
| Paper/Purpur 1.21.4 | **IMPLEMENTED_NOT_VERIFIED** | 25 | Adapter present | Core present | Integration present | `nms:v1_21_R3`, Paper dev bundle 1.21.4, pack format 46. CI matrix build pending. |
| Paper/Purpur 1.21.5 | **IMPLEMENTED_NOT_VERIFIED** | 25 | Adapter present | Core present | Integration present | `nms:v1_21_R4`, Paper dev bundle 1.21.5. CI matrix build pending. |
| Paper/Purpur 1.21.6–1.21.8 | **IMPLEMENTED_NOT_VERIFIED** | 25 | Adapter present | Core present | Integration present | `nms:v1_21_R5`; 1.21.8 is the production target. CI matrix and runtime test pending. |
| Paper/Purpur 1.21.9–1.21.10 | **IMPLEMENTED_NOT_VERIFIED** | 25 | Adapter present | Core present | Integration present | `nms:v1_21_R6`, compiled against Paper 1.21.10. CI matrix build pending. |
| Paper/Purpur 1.21.11 | **IMPLEMENTED_NOT_VERIFIED** | 25 | Adapter present | Core present | Integration present | `nms:v1_21_R7`, Paper dev bundle 1.21.11. CI matrix build pending. |
| Paper/Purpur 26.1.x | **IMPLEMENTED_NOT_VERIFIED** | 25 | Adapter present | Core present | Integration present | `nms:v26_R1`, compiled against a 26.1.2 build range. CI/full build pending. |
| Paper/Purpur 26.2.x | **IMPLEMENTED_NOT_VERIFIED** | 25 | Adapter present | Core present | Integration present | `nms:v26_R2`, current upstream/latest target. CI matrix build pending. |
| Versions newer than 26.2.x | **UNSUPPORTED** | Not tested | Not tested | Not tested | Not tested | Never falls through to latest NMS. A new verified adapter must be added first. |

The GitHub Actions compatibility matrix builds the concrete 1.21.4, 1.21.8,
1.21.11, and latest adapter tasks. Its 1.21.1 and 1.21.3 entries verify that
these versions remain explicitly rejected. If a historical Paper dev bundle is
removed from configured repositories, that matrix entry must remain failed and
the repository/download limitation must be recorded here; it must not be
reported as a pass.

## Why 1.21.0–1.21.3 are not enabled

The oldest real adapter is compiled against the Paper 1.21.4 development
bundle, and both Paper and Spigot descriptors declare API version 1.21.4. Its
implementation directly uses revision-specific CraftBukkit/Mojang classes,
packet constructors and entity metadata serializers. It also creates item
models with the modern `DataComponents.ITEM_MODEL` and structured custom-model
data, while generated pack metadata starts at resource-pack format 46.

Consequently, changing only plugin metadata would not provide compatibility.
A real 1.21.0–1.21.3 implementation requires dedicated paperweight modules and
verified adaptations for at least:

- CraftBukkit/Mojang mappings and packet constructor signatures;
- ItemDisplay metadata indices and serializers;
- entity spawn, teleport, passenger, equipment, and interaction packets;
- pre-1.21.4 item/custom-model data components;
- resource-pack format and item-model JSON understood by those clients.

No such adapter is included in this phase, because marking it supported without
building and exercising those paths would be unsafe.

## Runtime adapter selection

Bukkit startup performs one centralized selection before constructing any NMS
implementation. Supported servers log the detected Minecraft version,
Paper/Purpur/Folia/Bukkit platform, selected compatibility range, and
`Supported` status. Unknown or older servers log `Unsupported Minecraft server
version: X` and disable the plugin before version-specific classes are created.
Version branches remain in this selector and the single NMS factory rather than
being distributed through model, animation, or tracker code.

## Client compatibility target: Paper 1.21.8

ViaVersion and ViaBackwards own wire-protocol translation. GiaiTriBetterModel
does not implement or intercept protocol translation. A client being able to
join does **not** prove that it can consume the generated resource pack or
render its item-model components.

The production compatibility target is one fixed **Paper 1.21.8** server. The
plugin constructs 1.21.8-native `Clientbound*` packets through `v1_21_R5` and
sends them through each player's normal server connection. It does not branch
on, reject, or rewrite a player's negotiated client protocol. ViaVersion and
ViaBackwards must therefore remain the only protocol translation layer. This
source-level separation avoids a second translator, but it is not evidence that
every translated metadata field renders correctly on every client.

The generated pack advertises formats **46 through 88**, matching the bundled
server adapters (Minecraft 1.21.4 through 26.2). Clients older than 1.21.4 are
outside that pack range and must not be treated as model-render compatible just
because ViaBackwards permits login. BetterModel generates the pack but does not
itself select and send per-protocol packs, so administrators must not force the
modern generated pack onto an older client population without a separately
verified fallback pack. A future multi-target implementation should key the
generated-pack cache by pack-format family, not generate a pack per player.

### Client result matrix

No real Minecraft client was launched in the current environment. Every result
is therefore `UNVERIFIED`; in particular, no row below claims that an older
client renders models merely because ViaBackwards is expected to allow login.

| Client | Server | ViaVersion | ViaBackwards | Join | Model | Animation | Resource Pack | Status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1.21 / 1.21.1 | Paper 1.21.8 | Planned | Required | UNVERIFIED | UNVERIFIED | UNVERIFIED | Outside generated pack's advertised range; UNVERIFIED | **UNVERIFIED** |
| 1.21.2 | Paper 1.21.8 | Planned | Required | UNVERIFIED | UNVERIFIED | UNVERIFIED | Outside generated pack's advertised range; UNVERIFIED | **UNVERIFIED** |
| 1.21.3 | Paper 1.21.8 | Planned | Required | UNVERIFIED | UNVERIFIED | UNVERIFIED | Outside generated pack's advertised range; UNVERIFIED | **UNVERIFIED** |
| 1.21.4 | Paper 1.21.8 | Planned | Required | UNVERIFIED | UNVERIFIED | UNVERIFIED | Format 46 is advertised; acceptance UNVERIFIED | **UNVERIFIED** |
| 1.21.5–1.21.7 | Paper 1.21.8 | Planned | Required | UNVERIFIED | UNVERIFIED | UNVERIFIED | Within advertised range; acceptance UNVERIFIED | **UNVERIFIED** |
| 1.21.8 | Paper 1.21.8 | Planned | Not required for same protocol | UNVERIFIED | UNVERIFIED | UNVERIFIED | Native target; acceptance UNVERIFIED | **UNVERIFIED** |
| 1.21.11 | Paper 1.21.8 | Required | Planned if required by installed Via stack | UNVERIFIED | UNVERIFIED | UNVERIFIED | Within advertised range; acceptance UNVERIFIED | **UNVERIFIED** |
| Latest supported by installed ViaVersion | Paper 1.21.8 | Required | Planned if required by installed Via stack | UNVERIFIED | UNVERIFIED | UNVERIFIED | Depends on client pack format; UNVERIFIED | **UNVERIFIED** |

`Planned` means the plugin is intended to be tested with that dependency
installed; it does not mean the dependency combination or row has passed.

### Source audit by client-sensitive area

| Area | Paper 1.21.8 implementation boundary | Cross-client risk to test |
| --- | --- | --- |
| ItemDisplay/entity metadata | `v1_21_R5` creates native 1.21.8 ItemDisplay spawn and entity-data packets. | Via must map entity type, metadata indices, serializers, and bundled packet contents without dropping transformations. |
| Resource-pack format | One generated pack advertises formats 46–88. | 1.21–1.21.3 are outside the advertised minimum; joining through Via does not make this pack compatible. |
| Item model/custom rendering | Items use structured custom-model data and item-model components. | Older clients may not understand the component or modern item-model JSON even if the item packet itself is translated. |
| Textures | PNG assets are version-neutral, but their model references are not. | Verify textures resolve rather than producing missing-model or purple/black output. |
| Animation/interpolation/scale | The server emits repeated native display transformation metadata. | Verify Via preserves vectors, quaternions, interpolation delay/duration, teleport updates, and scale. |
| Mount/passenger | The server emits native passenger packets and model hitbox state. | Verify translated passenger IDs/order, dismount, reconnect while mounted, and no ghost passenger. |
| Hitbox interaction | The 1.21.8 adapter consumes native serverbound interaction packets. | Verify Via translates client interaction hand/target/position and that no duplicate damage or interaction occurs. |
| Reconnect | Player channel caches are keyed by UUID and closed on quit by the existing lifecycle. | Reconnect repeatedly and compare `/gbm stats`; no stale channel, display, or duplicated entity may remain. |
| MythicMobs | MythicMobs drives the same tracker, animation, display, hitbox, and mount paths. | Test a modeled boss through spawn, state changes, damage, skills, death, despawn, and reload for every client row. |

### Crash-safety conclusion

The audit found no code path that loads a different NMS implementation based on
the connecting client's version: the server selects `v1_21_R5` once, and all
clients receive server-native packets through their normal connections. Thus an
old client cannot change the plugin's server adapter or cause a startup
`ClassNotFoundException`/`NoSuchMethodError` by version selection alone.

That is **not** a runtime confirmation that Paper 1.21.8 plus a 1.21–1.21.3
client cannot disconnect, trigger a packet translation exception, reject the
pack, or render incorrectly. This remains **UNVERIFIED** until the exact
ViaVersion/ViaBackwards builds and real clients complete the following test.

## Client acceptance checklist — not yet executed

Test server: **Paper 1.21.8**, Java 25, ViaVersion and ViaBackwards installed.
Record the exact ViaVersion/ViaBackwards builds and never convert an unchecked
row into `SUPPORTED` based only on a successful login.

| Client | Join | Pack accepted | Model | Animation | Texture | Hitbox | MythicMobs boss | Mount | Reconnect | Reload |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1.21 / 1.21.1 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 1.21.2 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 1.21.3 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 1.21.4 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 1.21.5–1.21.7 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 1.21.8 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| 1.21.11 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| Latest adapter/client | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |

For each row also verify disconnect during pack download, repeated reconnects,
`/gbm reload`, and `/gbm stats` before/after the test. Registry, tracker, and
display counts must return to their expected baseline without duplicate packet
entities.
