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

    // Mob attraction: hostile mobs without a target walk to the point a beam lights up. Independent of spawn blocking.
    public static final ModConfigSpec.BooleanValue BEAM_ATTRACTS_MOBS = B
            .comment("Hostile mobs without a target investigate the block lit by a beam (they do not target the player directly).")
            .define("beamAttractsMobs", false);
    public static final ModConfigSpec.DoubleValue ATTRACT_RADIUS = B
            .comment("Mobs within this distance (blocks) of the lit point are attracted.")
            .defineInRange("attractRadius", 12.0, 4.0, 32.0);
    public static final ModConfigSpec.IntValue ATTRACT_INTERVAL = B
            .comment("Ticks between attraction checks (per level).")
            .defineInRange("attractInterval", 10, 5, 100);
    public static final ModConfigSpec.IntValue ATTRACT_MAX_MOBS = B
            .comment("Max mobs steered per beam per check (nearest first).")
            .defineInRange("attractMaxMobs", 8, 1, 32);
    public static final ModConfigSpec.DoubleValue ATTRACT_SPEED = B
            .comment("Navigation speed modifier for attracted mobs.")
            .defineInRange("attractSpeed", 1.0, 0.5, 1.5);
    public static final ModConfigSpec.DoubleValue ATTRACT_REPATH_DISTANCE = B
            .comment("A mob gets a new path only when the lit point moved at least this far (blocks) since its last path.")
            .defineInRange("attractRepathDistance", 2.0, 0.5, 8.0);

    public static final ModConfigSpec SPEC = B.build();

    private BeamServerConfig() {
    }
}
