# Beam Lights for mod developers

Beam Lights (NeoForge 1.21.1) turns "beams" emitted by entities into directional dynamic light on the client
(Sodium Dynamic Lights or LambDynamicLights backend, chosen at runtime; providers need not care which) and into spawn blocking / mob attraction on the server. This page covers the ways
another mod, a modpack or a KubeJS script can add beams. Datapack beams (no code) are described in the
[README](README.md#data-driven-beams-070).

The stable API is the package `it.ratlab.beamlights.api` and its subpackages (`api.event`, `api.math`); everything
else is internal and may change in any release. `BeamLightsApi.API_VERSION` is bumped on incompatible API changes
(1 = Beam Lights 0.8.0).

## Depending on Beam Lights

Build locally with `./gradlew publish`: the artifacts land in `build/repo` (and in `~/.m2`):

| Artifact | Use |
|---|---|
| `it.ratlab:beamlights:<version>:api` | API classes only, for `compileOnly` |
| `it.ratlab:beamlights:<version>` | full mod jar, for the dev runtime |
| `...:sources`, `...:javadoc` | IDE sources and API javadoc |

```groovy
repositories {
    maven { url = uri('/path/to/beamlights/build/repo') } // or mavenLocal()
}
dependencies {
    compileOnly 'it.ratlab:beamlights:0.8.0:api'
    // Only if you want the mod in your dev client/server:
    runtimeOnly 'it.ratlab:beamlights:0.8.0'
}
```

Optional dependency in `neoforge.mods.toml`:

```toml
[[dependencies.yourmod]]
    modId = "beamlights"
    type = "optional"
    versionRange = "[0.8,)"
    ordering = "NONE"
    side = "BOTH"
```

With an optional dependency, only touch Beam Lights classes after `ModList.get().isLoaded("beamlights")`, from a
class that is not loaded otherwise (the usual "compat class" pattern).

Beam Lights registers nothing in a synced registry (item beam data lives in `minecraft:custom_data`), so it is
optional on both sides: a client with the mod can join a server without it and the other way round.

## BeamProvider (code decides every tick)

```java
public final class LanternProvider implements BeamProvider {
    @Override public String name() { return "mymod:lantern"; }

    // Hot path: runs for every candidate entity every tick. Keep it to a few field reads.
    @Override public boolean mayEmit(Entity entity) {
        return entity instanceof LivingEntity l && l.getMainHandItem().is(MyItems.LANTERN.get());
    }

    // Only called when mayEmit returned true. Both sides.
    @Override public void collect(Entity entity, float partialTick, Consumer<Beam> out) {
        LivingEntity l = (LivingEntity) entity;
        if (!MyItems.LANTERN.get().isOn(l.getMainHandItem())) return;
        out.accept(Beam.of(l.getEyePosition(partialTick), l.getViewVector(partialTick),
                24f /* range */, 20f /* cone half-angle */, 13 /* luminance */, 0xFFE8B0));
    }
}

// FMLCommonSetupEvent, both sides:
event.enqueueWork(() -> BeamLightsApi.register(new LanternProvider()));
```

`BeamLightsApi.unregister(provider)` removes it again. Reading beams: `BeamLightsApi.isBeamActive(entity)` and
`BeamLightsApi.getBeams(entity)` (snapshot, runs providers and the event: fine for occasional checks, not for every
entity every tick).

## BeamCollectEvent (edit what providers produced)

Posted on `NeoForge.EVENT_BUS` after the providers ran for one entity, on both sides. The list is mutable; `Beam` is
an immutable record with `withLuminance`, `withRange`, `withRgb`. Only fired for entities that some provider accepts
in `mayEmit`.

```java
@SubscribeEvent
static void onBeams(BeamCollectEvent event) {
    // Dim every beam while the entity is underwater.
    if (event.getEntity().isUnderWater()) {
        event.getBeams().replaceAll(b -> b.withLuminance(b.luminance() - 4));
    }
}
```

## Item beam data `beamlights:beam` in `custom_data` (one stack, no datapack)

Same fields as a datapack definition (minus items, condition, priority). Only `luminance` is required. In the slots
it lists, it wins over datapack definitions; `luminance: 0` turns a stack's beam off.

| Field | Default | Range |
|---|---|---|
| `luminance` | required | 0..15 |
| `range` | 16 | 1..128 blocks |
| `cone` | 25 | 1..89 degrees (half-angle) |
| `color` | `"#FFFFFF"` | `"#RRGGBB"` string or int |
| `origin` | `{forward: 0, down: 0}` | -4..4 each |
| `slots` | `["mainhand", "offhand"]` | `mainhand`, `offhand`, `head`, `curios` |

Java:

```java
BeamLightsApi.setBeam(stack,
        new BeamItemData(12, 20f, 25f, 0xFFE8B0, 0.3, 0.2, List.of("mainhand", "head")));
BeamLightsApi.setBeam(stack, BeamItemData.of(10)); // defaults
Optional<BeamItemData> beam = BeamLightsApi.getBeam(stack);
BeamLightsApi.clearBeam(stack);
```

KubeJS:

```js
// e.g. in a recipe result or a player event
BeamLights.setBeam(item, { luminance: 12, range: 20, color: '#FFE8B0', slots: ['mainhand', 'head'] })
BeamLights.getBeam(item)   // BeamItemData or null
BeamLights.clearBeam(item)
```

Command:

```
/give @s minecraft:lantern[minecraft:custom_data={"beamlights:beam":{luminance:12,range:20f,color:"#FFE8B0",slots:["mainhand","offhand"]}}]
```

Loot tables: function `minecraft:set_custom_data` with `"tag": "{\"beamlights:beam\":{luminance:12}}"`.

`custom_data` is not validated on write by the game: invalid beam data is ignored on read (logged once per distinct
error). `setBeam` from Java/KubeJS validates and throws.

## Threading and sides

- Providers and `BeamCollectEvent` listeners run on the client thread (light) and on the server thread (spawn
  blocking, mob attraction). In single player both run, on different threads: keep providers stateless or
  thread-safe. Check `entity.level().isClientSide()` before using client-only classes.
- `BeamLightsApi.register` / `unregister` are thread-safe; call them during setup.
- `getBeams` / `isBeamActive` must be called on the thread that owns the entity's level.

## Performance guidance

- `mayEmit` is called for every rendered entity near the player every client tick: a held-item check, no
  allocation, no lookups by string.
- `collect` should add at most a few beams per entity (the client uses up to 8 beams per entity and caps the light
  sources per emitter and globally).
- Each beam is traced and turned into light sources; light sources that move cost chunk-section rebuilds in Sodium.
  The client batches and budgets those moves (see the README "Performance" section), but fewer, steadier beams are
  always cheaper.
