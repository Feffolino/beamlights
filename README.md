# Beam Lights

Directional dynamic light for flashlights and lamps on NeoForge 1.21.1. The beam lights the block it hits and the
path along the way through Sodium Dynamic Lights. Native Omega Flashlight support.

Build: `JAVA_HOME="/c/Program Files/Java/jdk-25" ./gradlew build` (jar in `build/libs/`).
Debug: `/beamlights status`, `/beamlights debug overlay on`, `/beamlights debug render on`, `/beamlights debug dump`.

Mod and pack developers: see [DEVELOPERS.md](DEVELOPERS.md) (Java API, `BeamCollectEvent`, the `beamlights:beam` item
component, KubeJS, Maven artifacts).

## Performance (0.8.0)

The expensive part of dynamic light is not the beam tracing but Sodium re-meshing chunk sections every time a light
source moves or changes brightness. The client config section `[performance]` limits those changes: `snapToBlock`
(lights at block centers, default on), `maxMovesPerTick` (budget of light changes per tick, default 12, local
player's beam first), `remoteUpdateInterval` (other emitters re-traced every N ticks, default 2), `lodDistance` (far
emitters use only the central ray, default 24 blocks) and `mergeSameSection` (coarser midpoints, default off).
`/beamlights perf` shows the settings and live counters (also in the debug overlay);
`/beamlights perf preset quality|balanced|performance` switches all of them at once.

Since 0.8.2 lights also lag a little to save changes: `moveHysteresis` (1.5 blocks; `centralHysteresis` 0.75 for the
spot you look at), `luminanceHysteresis`, and fewer smoothing steps (`glideMinTicks`, `fadeSteps`). The debug overlay
shows `moves/s` and the resulting `rebuilds/s ~N`. To measure the FPS cost, toggle the mod with `/beamlights off` and
`/beamlights on` (saved, all lights cleared while off) and compare. Any `/beamlights perf|smoothing|layout <key>`
without a value prints the current value.

Since 0.9.0 updates adapt to motion (`[motion]`, `/beamlights motion ...`): while the beam turns fast (90 deg/s, or
the emitter moves at 8 blocks/s) only the spot you look at follows and side rays and midpoints keep their lights;
while moving slower they update every 3 ticks; once still everything snaps to its exact place. A fast camera sweep
then costs about one light change per tick instead of 7. The overlay shows `motion: FAST 240°/s ...`. Default budget
`maxMovesPerTick` is now 6 (re-apply `/beamlights perf preset balanced` to update a saved config).

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

### Java API, item component and KubeJS

Other mods register a `BeamProvider` with `BeamLightsApi.register(provider)` in common setup, on both sides (the client
uses beams for light, the server for spawn blocking and mob attraction), or edit beams in `BeamCollectEvent`. A single
stack gets a beam with the vanilla `custom_data` component under the key `beamlights:beam` (same fields as above, e.g.
`/give @s minecraft:lantern[minecraft:custom_data={"beamlights:beam":{luminance:12,range:16,cone:35}}]`, the loot
function `set_custom_data`, or `BeamLights.setBeam(item, {luminance: 12})` in KubeJS). With KubeJS installed, scripts
also get `BeamLights.isBeamActive(entity)`, `BeamLights.beamCount(entity)`, `BeamLights.beams(entity)` (beams of the
entity right now, on the entity's side), `BeamLights.getBeam(item)` and `BeamLights.clearBeam(item)`.

Beam Lights is optional on both sides: it registers nothing in a synced registry, so a client with the mod can join a
server without it and the other way round. A client-only install gives dynamic light for items the client already
knows (e.g. Omega flashlights, `custom_data` items); server features (spawn blocking, mob attraction, datapack
definitions) need the mod on the server. Details in [DEVELOPERS.md](DEVELOPERS.md).
