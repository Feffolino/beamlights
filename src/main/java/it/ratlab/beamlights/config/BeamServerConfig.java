package it.ratlab.beamlights.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server config, file serverconfig/beamlights-server.toml (per world). */
public final class BeamServerConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    // Only natural monster spawns are blocked (MobSpawnType.NATURAL): spawners, eggs, commands, structures and
    // reinforcements are never touched.
    public static final ModConfigSpec.BooleanValue BLOCK_SPAWNS_IN_BEAM = B
            .comment("Cancel natural monster spawns inside the cone of a lit beam (same range, angle and transparency rule as the light).")
            .define("blockSpawnsInBeam", true);
    public static final ModConfigSpec.IntValue SPAWN_CHECK_RANGE = B
            .comment("Radius (blocks) around a spawn position searched for beam emitters. Should cover the longest beam range.")
            .defineInRange("spawnCheckRange", 64, 16, 128);
    public static final ModConfigSpec.BooleanValue DEBUG_LOG = B
            .comment("Log blocked spawns (at most one line per second).")
            .define("debugLog", false);

    public static final ModConfigSpec SPEC = B.build();

    private BeamServerConfig() {
    }
}
