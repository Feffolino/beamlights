# Changelog

## 0.8.0 (unreleased)
- Public API (package `it.ratlab.beamlights.api`, `API_VERSION = 1`): `BeamLightsApi.register/unregister`,
  `isBeamActive(entity)`, `getBeams(entity)` (snapshot), `providerNames()`, `beamComponent()`; Javadoc on every API
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
