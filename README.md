# Beam Lights

Directional dynamic light for flashlights and lamps on NeoForge 1.21.1. The beam lights the block it hits and the
path along the way through Sodium Dynamic Lights. Native Omega Flashlight support.

Build: `JAVA_HOME="/c/Program Files/Java/jdk-25" ./gradlew build` (jar in `build/libs/`).
Debug: `/beamlights status`, `/beamlights debug overlay on`, `/beamlights debug render on`, `/beamlights debug dump`.

## Data-driven beams (0.7.0)

Any item can emit a beam through a datapack file `data/<namespace>/beamlights/beams/<name>.json` (KubeJS packs can use
`kubejs/data/<namespace>/beamlights/beams/`). Files are loaded on the server and synced to clients on join and on
`/reload`; invalid files are logged with their id and skipped.

```json
{
  "items": ["minecraft:lantern", "#c:flashlights"],
  "slots": ["mainhand", "offhand", "head", "curios"],
  "luminance": 12,
  "range": 20.0,
  "cone": 25.0,
  "color": "#FFE8B0",
  "condition": { "component": "mymod:enabled", "equals": true },
  "origin": { "forward": 0.3, "down": 0.2 },
  "priority": 0
}
```

| Key | Required | Default | Meaning |
|---|---|---|---|
| `items` | yes | | item ids or `#` item tags; unknown items are ignored (optional mods) |
| `slots` | no | `["mainhand","offhand"]` | `mainhand`, `offhand`, `head`, `curios` (any Curios slot, players only, needs Curios) |
| `luminance` | yes | | light level 0..15 |
| `range` | no | 16 | beam length in blocks, 1..128 |
| `cone` | no | 25 | cone half-angle in degrees, 1..89 |
| `color` | no | `#FFFFFF` | `#RRGGBB` (kept for colour-capable backends) |
| `condition` | no | always on | `{"component": id, "present": true}` or `{"component": id, "equals": <json>}`; `equals` compares the component encoded with its codec |
| `origin` | no | 0 / 0 | offset from the eye: `forward` along the look direction, `down` world-down, -4..4 |
| `priority` | no | 0 | highest wins when several definitions match the same stack |

At most two data beams per entity. Players use every listed slot; other living entities (armor stands, mobs) only
hands and head. Omega Flashlight items are skipped (native support). Examples in `examples/datapack` (copy it into
`datapacks/`): a held lantern, and a renamed leather helmet that works as a helmet lamp.

### Java API and KubeJS

Other mods register a `BeamProvider` with `BeamLightsApi.register(provider)` in common setup, on both sides (the client
uses beams for light, the server for spawn blocking and mob attraction). With KubeJS installed, scripts get the binding
`BeamLights.isBeamActive(entity)` and `BeamLights.beamCount(entity)` (lit beams of the entity right now, on the
entity's side).
