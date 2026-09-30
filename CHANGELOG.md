# Changelog

## 0.3.0 (unreleased)
- Phase 3: server-side spawn blocking. Natural monster spawns inside the cone of a lit beam (same range, angle and
  transparency rule as the light) are cancelled via FinalizeSpawnEvent. Server config beamlights-server.toml
  (blockSpawnsInBeam, spawnCheckRange, debugLog), op command /beamlightsspawns. Omega provider is now server-safe and
  registered in common setup; LevelOcclusion moved to the common world package.

## 0.2.0 (unreleased)
- Phase 2: multi-ray cone (central ray + 3 side rays: down, up-right, up-left) with cross-ray merge; config keys
  rays, coneSpread, sideLuminanceOffset, sideMidpoints; side rays drawn grey in the debug render.

## 0.1.0 (unreleased)
- Phase 1: one ray per beam, hit light + midpoints, Sodium Dynamic Lights backend, Omega Flashlight (held and placed),
  debug overlay/render/commands.
