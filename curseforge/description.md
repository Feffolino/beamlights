# Beam Lights

**Flashlights that actually light what you point at.**

Most dynamic light mods give a held light a round glow around the player. Beam Lights turns a flashlight, a helmet
lamp or a headlight into a **directional beam**: the spot you aim at is lit, and so is the path along the way.

<!-- SCREENSHOT: dark cave, beam on a wall -->

## Features

- **Real beam light** – the hit point becomes a dynamic light; points along the ray and side rays inside the cone are
  optional.
- **Cone light** (with LambDynamicLights) – a soft cone of light along the beam when you are indoors.
- **Indoor / outdoor layouts** – indoors you get the cone and one ray layout; outdoors (far hit, open sky) a lighter
  layout. Switching uses hysteresis, so the light never flickers at the edge.
- **Ray patterns** – single ray, triangle, cross, ring, double ring, horizontal/vertical fan, or your own custom rays.
- **Smooth and light on FPS** – lights glide and cross-fade, update less while you turn fast, and a per-tick change
  budget keeps chunk rebuilds low. Presets: quality, balanced, performance.
- **Mobs** (server game rules) – no natural monster spawns inside a lit beam (on by default); optional weak attraction
  of idle mobs to the lit spot (off by default).

<!-- SCREENSHOT: indoor cone vs outdoor triangle -->

## Compatibility

| Mod | |
|---|---|
| **LambDynamicLights** *(recommended)* | full support, including the cone light |
| **Sodium Dynamic Lights** | supported (point lights, no cone) |
| **Omega Flashlight** | native support: hand-held and placed flashlights, bulb tiers, flicker |
| **Curios** | beam items in Curios slots |
| **KubeJS** | script bindings and layout files from `kubejs/assets` |

You need **one** dynamic lights mod (LambDynamicLights or Sodium Dynamic Lights) to see the light. Without it, spawn
blocking still works on the server.

**Client / server** – install on the client for the light, on the server for spawn blocking, mob attraction and
datapack beams. Each side works on its own: players without the mod can join a server that has it.

## Make any item a light beam

Add a JSON file to a datapack, `data/<namespace>/beamlights/beams/<name>.json`:

```json
{
  "items": ["minecraft:lantern", "minecraft:soul_lantern"],
  "slots": ["mainhand", "offhand"],
  "luminance": 12,
  "range": 12,
  "cone": 40,
  "color": "#FFD08A",
  "origin": { "forward": 0.3, "down": 0.3 }
}
```

Hands, armor and Curios slots, item tags and conditions on item components are supported.

## Per-source layouts (resource pack or KubeJS)

`assets/<namespace>/beamlights/layouts/<name>.json` picks the layout and cone for each beam source:

```json
{ "providers": ["omegaflashlight"], "indoor": "CENTER_ONLY", "outdoor": "TRIANGLE", "cone": true }
```

Press F3+T to reload.

## Commands

Everything is client-side under **`/beamlights`** and saved to the config at once:

- `/beamlights` – status and all settings
- `/beamlights on|off` – quick toggle
- `/beamlights layout ...` – ray pattern and presets
- `/beamlights cone ...` – cone light, outdoor pattern, indoor/outdoor switching
- `/beamlights perf preset quality|balanced|performance`
- `/beamlights spawns|attract on|off` – server game rules (operator)
- `/beamlights debug overlay on` – live numbers (light changes per second, state, layout)

## For developers

Java API (`BeamLightsApi`, `BeamProvider`, `BeamCollectEvent`), per-stack beam data in `custom_data`, KubeJS bindings
and Maven artifacts: see the GitHub repository.

## Links

- Source and issues: https://github.com/Feffolino/beamlights
- License: MIT
