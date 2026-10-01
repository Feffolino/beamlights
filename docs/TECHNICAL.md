# Beam Lights - technical notes

Config keys, backends and design notes, newest features first. Player overview: [README](../README.md).

Directional dynamic light for flashlights and lamps on NeoForge 1.21.1. The beam lights the block it hits and the
path along the way through Sodium Dynamic Lights or LambDynamicLights (whichever is installed; they exclude each
other). Native Omega Flashlight support.

Build: `JAVA_HOME="/c/Program Files/Java/jdk-25" ./gradlew build` (jar in `build/libs/`).
Debug: `/beamlights status`, `/beamlights debug overlay on`, `/beamlights debug render on`, `/beamlights debug dump`.

Mod and pack developers: see [DEVELOPERS.md](../DEVELOPERS.md) (Java API, `BeamCollectEvent`, the `beamlights:beam` item
component, KubeJS, Maven artifacts).


## Indoor / outdoor layouts and layout profiles (1.1.1 - 1.1.3)

Every central ray goes through an open area gate: **open** when there is no hit, the hit is farther than
`coneMaxDistanceForCone + openAreaHysteresis`, or (`openAreaSky`) the lit point has sky light >= `openAreaSkyLight`;
back to **indoor** below `coneMaxDistanceForCone - openAreaHysteresis`. Each state is held `openAreaMinTicks`.

| Key (`[cone]`) | Command `/beamlights cone ...` | Default | Meaning |
|---|---|---|---|
| `openAreaPattern` | `openArea <pattern>` | CENTER_ONLY | outdoor side rays (indoor = `rayPattern`) |
| `openAreaHysteresis` | `openHysteresis` | 2.0 | band in blocks around `coneMaxDistanceForCone` |
| `openAreaMinTicks` | `openMinTicks` | 10 | minimum ticks per state |
| `openAreaSky` | `openSky` | true | open sky at the lit point counts as open |
| `openAreaSkyLight` | `openSkyLight` | 15 | sky light level that counts as open sky |
| `openAreaFadeTicks` | `openFade` | 6 | outdoor side rays start 4 levels dimmer for this long |

Indoors the beam gets the cone (if `ldlConeLight`), outdoors never. The overlay line `area:` shows the state.

Layout profiles (client resources, F3+T reloads) override patterns and the cone per beam provider
(`omegaflashlight`, `data`, `event`):

```json
{ "providers": ["omegaflashlight"], "indoor": "CENTER_ONLY", "outdoor": "TRIANGLE", "cone": true, "priority": 0 }
```

File: `assets/<ns>/beamlights/layouts/<name>.json` (resource pack, or KubeJS `kubejs/assets/<ns>/...`). Missing fields
use the config; `cone: false` turns the cone off for that provider (it cannot turn it on when `ldlConeLight` is off);
the highest `priority` wins per provider.

## LambDynamicLights backend (1.0.0)

With LambDynamicLights 4.8+ (instead of Sodium Dynamic Lights) every light point becomes an LDL custom light behavior;
LDL computes the light and rebuilds chunk sections itself. Same gating and budget as with SDL. Optional
`ldlConeLight = true` (client config) adds a cone of light along each central beam (`ldlConeLuminanceOffset`, -3).

### Cone light (1.1.0)

The cone is meant for indoor and near hits: outdoors a long, wide cone made LDL evaluate it for every entity and
particle and rebuild many sections (50 vs 100 FPS measured). Client config section `[cone]`, all keys also editable
with `/beamlights cone <key> [value]` (no value = show; `/beamlights cone` shows all; saved at once):

| Key | Command | Default | Meaning |
|---|---|---|---|
| `ldlConeLight` | `enabled` | false | cone on/off (in `[performance]`, name unchanged) |
| `ldlConeLuminanceOffset` | `luminanceOffset` | -3 | cone level relative to the beam (in `[performance]`) |
| `coneMaxAngle` | `maxAngle` | 25 | half-angle cap in degrees, 5..60 (the visual beam keeps its angle) |
| `coneMaxLength` | `maxLength` | 16 | length cap in blocks, 4..48 |
| `coneMinLength` | `minLength` | 3 | shorter beams get no cone, only the point lights |
| `coneMaxDistanceForCone` | `maxDistance` | 20 | farther hits or no hit (sky): no cone, it fades out |
| `coneEndFactor` | `endFactor` | 0.5 | level at the end relative to the apex, 0.1..1 |
| `coneEdgeSoftness` | `edgeSoftness` | 0.25 | outer fraction of the radius fading to half at the rim, 0..1 |
| `coneFreezeWhenFast` | `freezeWhenFast` | true | keep the cone while the beam turns or moves fast |
| `coneLengthHysteresis` | `lengthHysteresis` | 2.0 | length change (blocks) that updates the cone |
| `coneApexHysteresis` | `apexHysteresis` | 1.0 | apex move (blocks) that updates the cone |
| `coneAngleHysteresis` | `angleHysteresis` | 4 | direction change (degrees) that updates the cone |

Presets: `/beamlights cone preset light` (angle 18, length 12, max distance 14), `balanced` (the defaults), `wide`
(angle 35, length 24, max distance 28); all keep `freezeWhenFast` on and reset min length and hysteresis to the
defaults.

Manual test (the dev run has neither mod):
1. In a test instance remove Sodium Dynamic Lights and install LambDynamicLights 4.8.x for 1.21.1 (with its
   dependencies), plus this jar.
2. `/beamlights status`: backend `lambdynamiclights`, status `LDL mode FANCY` (or the mode set in LDL's options).
3. Turn a flashlight on: hit point and midpoints lit, lights follow the beam; `/beamlights debug overlay on` shows
   sources and moves.
4. LDL options, mode OFF: beam lights disappear; back on: they return.
5. Set `ldlConeLight = true` in `beamlights-client.toml`, `/beamlights reload`: a soft cone along the beam, the status
   line shows `N cones`; turning fast stays within `maxMovesPerTick`.
6. Leave and rejoin the world / change dimension: no leftover lights.

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
