# Changelog

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
