First public release.

- Directional beam light through LambDynamicLights (recommended) or Sodium Dynamic Lights: hit point, optional
  midpoints and side rays in configurable layouts (single, triangle, cross, ring, double ring, fans, custom).
- Cone light indoors (LambDynamicLights); indoor / outdoor layouts with hysteresis and open-sky detection.
- Per-source layout profiles in resource packs or KubeJS (`assets/<ns>/beamlights/layouts`).
- Smoothing, motion-adaptive updates, change budget, distance LOD, performance presets.
- Server: no monster spawns inside lit beams, optional weak mob attraction (game rules).
- Native Omega Flashlight support; datapack beam definitions (items, tags, Curios, components); Java API, event,
  KubeJS bindings.
- `/beamlights` client command for status, every setting and diagnostics.
