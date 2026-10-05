# Beam Lights

Directional dynamic light for flashlights, helmet lamps and headlights on **NeoForge 1.21.1**.
A light beam lights the block it hits and the path along the way, instead of a round glow around the player.

## Features

- **Real beam light**: the hit point (and optionally points along the ray and side rays inside the cone) become
  dynamic lights, so the spot you point at is actually lit.
- **Cone light** (LambDynamicLights): a cone of light along the beam indoors.
- **Indoor / outdoor layouts**: indoors the beam can use the cone and one ray layout, outdoors (far hit, no hit or open
  sky) another one, with hysteresis so it never flickers at the threshold. Patterns: single ray, triangle, cross, ring,
  double ring, horizontal / vertical fan, custom.
- **Smooth**: lights glide and cross-fade, update less while you turn fast, and respect a per-tick change budget to
  keep chunk rebuilds (and FPS cost) low.
- **Mobs** (server, game rules): no natural monster spawns inside lit beams (`beamlightsBlockSpawns`, on by default);
  optional weak attraction of idle mobs to the lit spot (`beamlightsAttractMobs`, off by default).
- **Omega Flashlight**: native support (hand-held and placed flashlights, bulb tiers, flicker).
- **Any item**: datapack JSON beam definitions (items or tags, hands, armor, Curios slots, colour, range, cone,
  conditions on item components), a Java API, an event and KubeJS bindings.
- **Per-source layouts**: resource-pack JSON (`assets/<ns>/beamlights/layouts/*.json`) picks indoor / outdoor layout
  and cone per beam provider; works from KubeJS `kubejs/assets`.

## Requirements

| Mod | Needed for |
|---|---|
| NeoForge 21.1+ for Minecraft 1.21.1 | required |
| [Sodium Dynamic Lights](https://www.curseforge.com/minecraft/mc-mods/sodium-dynamic-lights) **or** [LambDynamicLights](https://www.curseforge.com/minecraft/mc-mods/lambdynamiclights) | the visible light (client); the cone needs LambDynamicLights |
| Omega Flashlight 1.6.6+ (optional) | flashlight beams out of the box |
| Curios (optional) | beam items in Curios slots |
| KubeJS (optional) | script bindings |

**Recommended: LambDynamicLights.** It is the tested setup and the only one with the cone light. Sodium Dynamic Lights
works too (point lights only). Without a dynamic lights mod nothing is drawn, but spawn blocking still works on the
server.

**Sides**: install on the client for the light, on the server for spawn blocking, mob attraction and datapack beam
definitions. Each side works alone; players without the mod can join a server that has it.

## Commands (client)

Everything is under `/beamlights` and saved to `config/beamlights-client.toml` at once:

| Command | What |
|---|---|
| `/beamlights` / `status` | backend, sources, layout and every setting group |
| `/beamlights on` / `off` | quick on/off (A/B test) |
| `/beamlights layout ...` | ray pattern (indoor), side rays, spread, presets `default`, `wide`, `performance`, `cliff`, `floodlight` |
| `/beamlights cone ...` | cone light, its size caps, outdoor pattern (`openArea`) and indoor/outdoor switching |
| `/beamlights perf preset quality\|balanced\|performance` | performance presets |
| `/beamlights smoothing ...`, `motion ...` | movement smoothing and motion-adaptive updates |
| `/beamlights spawns\|attract [on\|off]` | server game rules (needs operator) |
| `/beamlights debug overlay\|render on\|off`, `debug dump` | diagnostics |

## Performance tips

Each change of a dynamic light rebuilds nearby chunk sections. Fewer rays and a smaller cone cost less:
`/beamlights perf preset performance`, a single-ray layout outdoors, or `/beamlights cone preset light`.
The debug overlay shows moves/s and estimated rebuilds/s.

## For pack and mod developers

- **[Wiki](https://github.com/Feffolino/beamlights/wiki)**: comprehensive player, admin, and pack-maker guide (1.21.1 NeoForge, 1.20.1 Forge coming soon)
- Datapack beams: `data/<ns>/beamlights/beams/*.json`, examples in [`examples/datapack`](examples/datapack).
- Layout profiles: `assets/<ns>/beamlights/layouts/*.json`, example in [`examples/resourcepack`](examples/resourcepack).
- Java API, `BeamCollectEvent`, item data, KubeJS, Maven: [DEVELOPERS.md](DEVELOPERS.md).
- Every config key in detail: [docs/TECHNICAL.md](docs/TECHNICAL.md).

## Building

```bash
./gradlew build
```

Needs a JDK (the Gradle toolchain downloads Java 21). The jar is `build/libs/beamlights-<version>+1.21.1.jar`.
Optional mods are compile-only jars in `libs/` (not in the repository, see `build.gradle`).

## License

MIT, see [LICENSE](LICENSE).
