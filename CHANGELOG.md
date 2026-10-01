# Changelog

## 1.1.1 (unreleased)
- `openAreaPattern` ([cone], default CENTER_ONLY): with `ldlConeLight` on, beams that get no cone (open area: hit
  farther than `coneMaxDistanceForCone`, or no hit) use this side ray pattern instead of `rayPattern`, e.g.
  `rayPattern = CENTER_ONLY` + `openAreaPattern = RING` = cone indoors, ring outdoors.
  `/beamlights cone openArea <pattern>`.

## 1.1.0 (unreleased)
Cone light performance (LDL `ldlConeLight`). Measured outdoors in a Lost Cities city (LambDynamicLights 4.8.11,
CENTER_ONLY): 50 FPS with the cone vs 100 without; LDL went from 2.1 % to 10.4 % of the render thread (per
entity/particle light lookups, spatial lookup rebuild) and Sodium from 10.0 % to 14.6 % (section rebuilds), because the
cone's bounding box was huge (range 22, half-angle 42, loose cube).
- Smaller cone: half-angle capped by `coneMaxAngle` (25; the visual beam is unchanged), length by `coneMaxLength` (16);
  beams shorter than `coneMinLength` (3) get no cone; hits farther than `coneMaxDistanceForCone` (20) or no hit (open
  sky) get no cone, it fades out and the point lights remain.
- Tight bounding box computed from apex, axis, length and radius: a typical outdoor beam hitting at 16.5 blocks now
  covers 48 LDL cells / 27 sections instead of 216 / 80 (open sky: none instead of 343 / 125).
- Cheaper `lightAtPos`: immutable `ConeLight.Prepared` snapshot with precomputed axis, tan, slope and reach; box, axial
  and lateral rejects before any square root; no allocation (the bounding box record is built once per update).
- Fewer updates: cone frozen while the emitter is FAST (`coneFreezeWhenFast`), hysteresis on length
  (`coneLengthHysteresis` 2.0), apex (`coneApexHysteresis` 1.0) and direction (`coneAngleHysteresis` 4 degrees); still
  through ConeSmoother, the central-ray gate and the move budget.
- Look: `coneEndFactor` (0.5) level at the end, `coneEdgeSoftness` (0.25) soft rim.
- New client config section `[cone]`; `ldlConeLight` and `ldlConeLuminanceOffset` keep their names and place.
- `/beamlights cone` (show all), `/beamlights cone <key> [value]`, `/beamlights cone preset light|balanced|wide`.
- Tests: `ConeLightTest` (tight bounds, cell count, fast path vs reference on random points, soft edge),
  `ConePolicyTest` (caps, max distance, min length, hysteresis, freeze). 144 tests.

## 1.0.1 (unreleased)
- Cone light smoothing (LDL `ldlConeLight`, needs `smoothing`): a cone length jump (near face to far face,
  >= `jumpDistance`) is walked in `coneSteps` (new, default 3, `/beamlights smoothing conesteps <n>`) equal steps,
  one every `glideMinTicks` ticks; smaller changes lerp by `smoothFactor`; a vanished cone fades out over
  `fadeSteps` instead of vanishing. Apex, direction and luminance still follow at once; every change goes
  through the existing change gate and move budget. Hit point lights already crossfaded in LDL mode (they pass
  through LightSmoother like with SDL).

## 1.0.0 (unreleased)
All five phases of the design are done: 1 single-beam dynamic light (SDL), 2 multi-ray cone, 3 spawn blocking (and
optional mob attraction), 4 data-driven beams (datapack, Java API, item component, KubeJS), 5 LambDynamicLights
backend. Version bumped to 1.0.0.
- LambDynamicLights backend (`lambdynamiclights`): used when LambDynamicLights (mod id `lambdynlights`, 4.8+) is
  installed instead of Sodium Dynamic Lights (the two mods exclude each other; SDL wins if both were present). Every
  light point is a custom `DynamicLightBehavior` added through LDL's `DynamicLightBehaviorManager`; LDL evaluates
  `lightAtPos` per block (sphere, level - 15/7.75 x distance, the same 7.75-block reach as SDL) and rebuilds the
  sections of the old and new bounding box itself. LDL mode OFF removes every beam light.
- Optional `ldlConeLight` (client config, default false; LDL only): one extra cone-shaped behavior per central beam,
  apex at the emitter, length up to the hit point, cone half-angle from the beam, full level at the apex fading to half
  at the end, falloff outside the cone surface; `ldlConeLuminanceOffset` (-3). No occlusion (like point lights).
- Change gating shared by both backends (`GatedLightBackend`: SourceMotion gate, snap, exact resync, move budget);
  cone changes go through the same gate and budget. `SdlBackend` is now a thin subclass, behaviour unchanged.
- No dynamic lights mod: `/beamlights status` says "no dynamic lights mod installed (Sodium Dynamic Lights or
  LambDynamicLights)".
- Tests: `ConeLightTest` (axis fade, cone radius, apex sphere, past-the-end falloff, bounds cover every lit block).

## 0.9.0 (unreleased)
- Motion-adaptive light updates (client config section `[motion]`, per emitter: the local player and every other
  emitter have their own state). Each trace measures the angular speed of the central beam direction (deg/s) and the
  origin speed (blocks/s), EMA-smoothed (alpha 0.5), and picks a state:
  - FAST (>= `fastTurnDegPerSec` 90 or >= `fastMoveBlocksPerSec` 8; left only below 0.7x): only the central hit
    point follows; side rays and midpoints keep their lights (`fastSideMode` FREEZE, default), fade out (FADE) or
    follow as before (OFF).
  - MOVING (from `slowTurnDegPerSec` 20 / fastMove / 4 up to fast): side rays and midpoints change at most every
    `sideUpdateTicks` (3) ticks, the central hit point every tick.
  - STILL (slow for `settleTicks` 6 ticks in a row): everything follows; for settleTicks ticks the lights go to their
    exact target ignoring `moveHysteresis`/`luminanceHysteresis` (fixes the hysteresis leftover after a sweep).
  - Implemented as a target filter before the smoother (held targets are fed again, so smoothing and ghosts are
    unchanged) plus an `exact` flag on `LightBackend.put` for the resync.
  - Simulated (9 sources, 180 deg/s for 40 ticks): 37 changes instead of 282 (0.9 per tick, central only), then one
    settling burst of 35.
- `maxMovesPerTick` default 12 -> 6; presets quality 16, balanced 6, performance 4 (presets also set the motion
  keys). A saved `maxMovesPerTick` stays until `/beamlights perf preset balanced` is re-applied.
- `/beamlights motion [fastTurn|slowTurn|fastMove|sideTicks|settle|mode] [value]` (saved; no value = current value).
  `/beamlights perf` and `/beamlights status` show the motion keys.
- Overlay: `motion: FAST 240°/s, 0.0 b/s, suppressed N` (local player's state, side/midpoint changes held back by the
  governor this tick).
- Fix: with `/beamlights off` the overlay kept the last beams/points/moves numbers; stats are reset while disabled.

## 0.8.2 (unreleased)
- Fewer chunk rebuilds (in-game: 150 moves/s with 8 sources = about 1200 section rebuilds/s, -30 FPS):
  - `moveHysteresis` (default 1.5 blocks) and `centralHysteresis` (0.75, central hit point): a light moves only when
    its target is that far from the shown position (with snapToBlock: another block and that distance).
  - `luminanceHysteresis` (default 1): 1-level luminance changes ignored for midpoints and side rays.
  - Smoothing: `glideMinTicks` (default 2) caps glide changes per light; `fadeSteps` (default 2) limits a fade to
    half then full/off whatever `fadeTicks` is, so a crossfade costs 4 changes instead of 8.
  - `maxMovesPerTick` default 24 -> 12; presets: quality 32, balanced 12, performance 6 (presets set the new keys
    too). A saved `maxMovesPerTick = 24` stays until `/beamlights perf preset balanced` is re-applied.
  - Simulated pipeline (8 sources): slow sweep 91 -> 48 changes in 40 ticks, jumps -50%.
- `/beamlights on|off` (saved, clears all lights) for quick A/B FPS tests; `/beamlights` alone shows the status,
  which now includes the enabled state.
- Perf, smoothing (`steps`, `glide` added) and layout subcommands without a value print the current value. New
  `/beamlights perf hysteresis|centralhysteresis|lumhysteresis`.
- Overlay: `rebuilds/s ~N` (applied changes per second x 8) next to `moves/s`.
- Debug dump: `/beamlights debug dump [ticks]` waits 40 ticks by default. The view is frozen while the chat is open,
  so the immediate dump always caught a still beam and printed `moved=false` for every point; it now also logs the
  applied/deferred change counts and ghost changes.

## 0.8.1 (unreleased)
- Beam item data moved from the registered data component `beamlights:beam` to the vanilla `minecraft:custom_data`
  component, key `"beamlights:beam"` (same fields). The mod registers nothing in a synced registry again, so it is
  optional on both sides. API: `beamComponent()` replaced by `getBeam/setBeam/clearBeam(ItemStack)`; KubeJS
  `BeamLights.setBeam/getBeam/clearBeam(item)`; `BeamItemData.STREAM_CODEC` removed. Decoded data is cached per
  CustomData instance; invalid data is ignored and logged once per message.

## 0.8.0 (unreleased)
- Public API (package `it.ratlab.beamlights.api`, `API_VERSION = 1`): `BeamLightsApi.register/unregister`,
  `isBeamActive(entity)`, `getBeams(entity)` (snapshot), `providerNames()`, `getBeam/setBeam/clearBeam(stack)`; Javadoc on every API
  type. `V3` moved from `core.math` to `api.math` (the API jar is self-contained). `Beam.of(Vec3, ...)` and
  `withLuminance/withRange/withRgb` helpers.
- `BeamCollectEvent` (`api.event`, NeoForge game bus, both sides): posted after the providers ran for an entity, with
  a mutable beam list; listener errors are logged once.
- Item data component `beamlights:beam` (persistent, synced; luminance, range, cone, color, origin, slots), checked
  before datapack definitions. Settable from Java, KubeJS (`item.set('beamlights:beam', {...})`) and `/give`. Because
  the component registry is synced, clients now need Beam Lights to join a server that has it.
- KubeJS binding `BeamLights.beams(entity)`.
- `maven-publish`: `./gradlew publish` writes `it.ratlab:beamlights:<version>` (main, `api`, `sources`, `javadoc`
  jars) to `build/repo` and `~/.m2`. New `DEVELOPERS.md`.
- Performance (client config section `[performance]`), aimed at Sodium chunk-section rebuilds caused by light source
  changes: `snapToBlock` (default true, sources at block centers, no change for sub-block motion),
  `maxMovesPerTick` (default 24, global budget of moves/luminance changes/creations/removals per tick, pure
  `core/MoveScheduler`: local central ray first, then aged, nearest, largest displacement), `remoteUpdateInterval`
  (default 2, remote emitters re-traced every N ticks staggered by entity id, last points reused), `lodDistance`
  (default 24, far emitters central ray only), `mergeSameSection` (default false); points in the same block are
  always merged. Fewer per-tick allocations (reused lists, stream-free plan counts, reused smoother sets).
- Overlay line `perf:` (moves applied/deferred, snap, LOD and reused emitters); `/beamlights perf [show]`,
  `/beamlights perf snap|budget|interval|lod|sectionmerge <v>`, `/beamlights perf preset quality|balanced|performance`.

## 0.7.0 (unreleased)
- Data-driven beams: datapack files `data/<ns>/beamlights/beams/*.json` (items or item tags, slots
  mainhand/offhand/head/curios, luminance, range, cone, colour, optional data component condition `present`/`equals`,
  origin offset, priority). Loaded by a server reload listener, invalid files logged and skipped; synced to clients
  with an optional payload (`beamlights:beam_definitions`) on join and `/reload`, client copy cleared on logout.
- New provider `data` (`core/DataBeamProvider`, both sides): players use hands, head and Curios, other living entities
  hands and head; at most two beams per entity; Omega Flashlight items skipped (native provider).
- Optional Curios support (`compat/curios`), public `api/BeamLightsApi.register(BeamProvider)`, KubeJS binding
  `BeamLights.isBeamActive(entity)` / `beamCount(entity)`.
- `/beamlights status` shows the number of synced data definitions; `/beamlights debug dump` prints the provider of
  each ray. Examples in `examples/datapack` (not shipped in the jar).

## 0.6.0 (unreleased)
- Smooth light movement (`core/LightSmoother`, between ticker and backend): small moves glide
  (`smoothFactor`, default 0.5), jumps of at least `jumpDistance` (default 3) blocks crossfade (a ghost fades out at the
  old spot while the light fades in at the new one), new lights fade in and vanished lights fade out over `fadeTicks`
  (default 4). Master switch `smoothing` (default true). Ghost keys set slot bit 7; planner slots stay below 128.
- `/beamlights smoothing [on|off]|factor <v>|jump <v>|fade <ticks>` (saved at once); `/beamlights status` shows the
  smoothing settings and the overlay the ghost count. `maxSources` now caps the smoothed output (ghosts first to go).

## 0.5.0 (unreleased)
- On/off switches are now game rules (per world): `beamlightsBlockSpawns` (SPAWNING, default true) and
  `beamlightsAttractMobs` (MOBS, default false). Config keys `blockSpawnsInBeam` and `beamAttractsMobs` removed.
- Op commands `/beamlightsspawns` and `/beamlightsattract` removed. `/beamlights spawns|attract [on|off]` forwards
  `gamerule <rule> [true|false]` to the server (op permission checked there). Blocked-spawn and attraction-step
  totals are printed in the rate-limited `debugLog` lines.
- Weaker mob attraction: only mobs with idle navigation and no target are nudged, with chance `attractChance`
  (default 0.35) per check, in steps of at most `attractStepDistance` (default 5) blocks toward the lit point;
  `attractSpeed` default lowered to 0.7. Vanilla targeting and other mods' attractors are never overridden.
- `/beamlights layout show|pattern|rays|spread|roll|inner|range|budget|luminance|midpoints|custom add|remove|clear|list
  |preset <default|wide|performance|cliff|floodlight>` edits the ray layout in the client config (saved at once,
  applied on the next frame). `/beamlights status` names the two game rules.

## 0.4.0 (unreleased)
- Configurable ray layout: `rays` replaced by `rayPattern` (CENTER_ONLY, TRIANGLE default = old layout, CROSS, RING,
  DOUBLE_RING, FAN_HORIZONTAL, FAN_VERTICAL, CUSTOM) with `sideRays`, `rayRollOffset`, `innerRays`, `innerSpread`,
  `customRays` ("spread,roll[,lumOffset[,midpoints[,rangeFactor]]]", invalid entries skipped and logged) and
  `sideRangeFactor`; at most 24 side rays. New per-emitter budget `maxSourcesPerEntity` (central rays placed first).
  Key ray index is now beamIndex * 32 + subRay. Overlay and /beamlights status show the layout.

## 0.3.0 (unreleased)
- Phase 3: server-side spawn blocking. Natural monster spawns inside the cone of a lit beam (same range, angle and
  transparency rule as the light) are cancelled via FinalizeSpawnEvent. Server config beamlights-server.toml
  (blockSpawnsInBeam, spawnCheckRange, debugLog), op command /beamlightsspawns. Omega provider is now server-safe and
  registered in common setup; LevelOcclusion moved to the common world package.
- Optional mob attraction (server, off by default): hostile mobs without a target walk to the point a lit beam hits
  (central ray only, checked every attractInterval ticks). Config keys beamAttractsMobs, attractRadius,
  attractInterval, attractMaxMobs, attractSpeed, attractRepathDistance; op command /beamlightsattract.

## 0.2.0 (unreleased)
- Phase 2: multi-ray cone (central ray + 3 side rays: down, up-right, up-left) with cross-ray merge; config keys
  rays, coneSpread, sideLuminanceOffset, sideMidpoints; side rays drawn grey in the debug render.

## 0.1.0 (unreleased)
- Phase 1: one ray per beam, hit light + midpoints, Sodium Dynamic Lights backend, Omega Flashlight (held and placed),
  debug overlay/render/commands.
