package it.ratlab.beamlights.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server config, file serverconfig/beamlights-server.toml (per world). */
public final class BeamServerConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    // The on/off switches are game rules (BeamGameRules): beamlightsBlockSpawns (default true) and
    // beamlightsAttractMobs (default false). Only natural monster spawns are blocked.
    public static final ModConfigSpec.IntValue SPAWN_CHECK_RANGE = B
            .comment("Radius (blocks) around a spawn position searched for beam emitters. Should cover the longest beam range.")
            .defineInRange("spawnCheckRange", 64, 16, 128);
    public static final ModConfigSpec.BooleanValue DEBUG_LOG = B
            .comment("Log blocked spawns and attraction steps with running totals (at most one line per second each).")
            .define("debugLog", false);

    // Mob attraction (game rule beamlightsAttractMobs): a weak nudge, idle mobs only, in short steps.
    public static final ModConfigSpec.DoubleValue ATTRACT_RADIUS = B
            .comment("Mobs within this distance (blocks) of the lit point are attracted.")
            .defineInRange("attractRadius", 12.0, 4.0, 32.0);
    public static final ModConfigSpec.IntValue ATTRACT_INTERVAL = B
            .comment("Ticks between attraction checks (per level).")
            .defineInRange("attractInterval", 10, 5, 100);
    public static final ModConfigSpec.IntValue ATTRACT_MAX_MOBS = B
            .comment("Max idle mobs considered per beam per check (nearest first).")
            .defineInRange("attractMaxMobs", 8, 1, 32);
    public static final ModConfigSpec.DoubleValue ATTRACT_SPEED = B
            .comment("Navigation speed modifier for attracted mobs.")
            .defineInRange("attractSpeed", 0.7, 0.5, 1.5);
    public static final ModConfigSpec.DoubleValue ATTRACT_CHANCE = B
            .comment("Chance per check that an idle mob in range takes a step toward the lit point.")
            .defineInRange("attractChance", 0.35, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue ATTRACT_STEP_DISTANCE = B
            .comment("Max length (blocks) of one step toward the lit point; the mob drifts in several short steps.")
            .defineInRange("attractStepDistance", 5.0, 1.0, 16.0);
    public static final ModConfigSpec.DoubleValue ATTRACT_REPATH_DISTANCE = B
            .comment("A mob gets a new step only when its step target is at least this far (blocks) from its previous one.")
            .defineInRange("attractRepathDistance", 2.0, 0.5, 8.0);

    public static final ModConfigSpec SPEC = B.build();

    private BeamServerConfig() {
    }
}
