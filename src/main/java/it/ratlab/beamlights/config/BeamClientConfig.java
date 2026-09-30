package it.ratlab.beamlights.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client config, file beamlights-client.toml. */
public final class BeamClientConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = B
            .comment("Master switch for beam dynamic light.")
            .define("enabled", true);
    public static final ModConfigSpec.BooleanValue OTHER_PLAYERS = B
            .comment("Light from other players' beams.")
            .define("otherPlayers", true);
    public static final ModConfigSpec.IntValue OTHER_PLAYERS_RANGE = B
            .comment("Max distance (blocks) of other players and placed lights whose beams are shown.")
            .defineInRange("otherPlayersRange", 48, 8, 256);
    public static final ModConfigSpec.IntValue MAX_SOURCES = B
            .comment("Global cap on light sources created by this mod.")
            .defineInRange("maxSources", 64, 1, 512);

    public static final ModConfigSpec.IntValue LUMINANCE_TIER1 = B
            .comment("Hit-point light level for bulb tier 1 (basic).")
            .defineInRange("luminanceTier1", 10, 0, 15);
    public static final ModConfigSpec.IntValue LUMINANCE_TIER2 = B
            .comment("Hit-point light level for bulb tier 2 (improved).")
            .defineInRange("luminanceTier2", 14, 0, 15);
    public static final ModConfigSpec.IntValue LUMINANCE_TIER3 = B
            .comment("Hit-point light level for bulb tier 3 (high quality).")
            .defineInRange("luminanceTier3", 15, 0, 15);
    public static final ModConfigSpec.BooleanValue SCALE_WITH_BATTERY = B
            .comment("Scale light level by remaining battery charge.")
            .define("scaleWithBattery", false);
    public static final ModConfigSpec.DoubleValue BATTERY_MIN_FACTOR = B
            .comment("Lowest battery factor, so the light does not fade to nothing before the battery is empty.")
            .defineInRange("batteryMinFactor", 0.3, 0.0, 1.0);

    public static final ModConfigSpec.BooleanValue MIDPOINTS = B
            .comment("Add lights along the ray, not only at the hit point.")
            .define("midpoints", true);
    public static final ModConfigSpec.DoubleValue MID_SPACING = B
            .comment("Distance (blocks) between midpoints. Sodium Dynamic Lights reaches 7.75 blocks per light.")
            .defineInRange("midSpacing", 6.0, 2.0, 16.0);
    public static final ModConfigSpec.IntValue MID_LUMINANCE_OFFSET = B
            .comment("Midpoint light level relative to the hit point (negative = dimmer).")
            .defineInRange("midLuminanceOffset", -4, -15, 0);
    public static final ModConfigSpec.IntValue MAX_SOURCES_PER_BEAM = B
            .comment("Max lights per ray (hit point + midpoints).")
            .defineInRange("maxSourcesPerBeam", 6, 1, 16);

    public static final ModConfigSpec.DoubleValue MOVE_THRESHOLD = B
            .comment("A light moves only when its target is at least this far (blocks). Each move rebuilds chunks.")
            .defineInRange("moveThreshold", 0.25, 0.0, 4.0);
    public static final ModConfigSpec.DoubleValue MERGE_DISTANCE = B
            .comment("Lights closer than this (blocks) to an accepted light are dropped.")
            .defineInRange("mergeDistance", 3.0, 0.0, 16.0);

    public static final ModConfigSpec.BooleanValue DEBUG_OVERLAY = B
            .comment("Show the debug text overlay (also /beamlights debug overlay on|off).")
            .define("debugOverlay", false);
    public static final ModConfigSpec.BooleanValue DEBUG_RENDER = B
            .comment("Draw rays and light points in the world (also /beamlights debug render on|off).")
            .define("debugRender", false);

    public static final ModConfigSpec SPEC = B.build();

    private BeamClientConfig() {
    }

    public static int luminanceForTier(int tier) {
        return switch (tier) {
            case 1 -> LUMINANCE_TIER1.get();
            case 2 -> LUMINANCE_TIER2.get();
            case 3 -> LUMINANCE_TIER3.get();
            default -> 0;
        };
    }
}
