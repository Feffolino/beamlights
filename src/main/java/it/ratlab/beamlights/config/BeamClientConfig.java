package it.ratlab.beamlights.config;

import it.ratlab.beamlights.core.RayLayout;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

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

    // Ray layout: the central ray is always traced; side rays come from rayPattern (max 24).
    public static final ModConfigSpec.EnumValue<RayLayout.Pattern> RAY_PATTERN = B
            .comment("Ray layout. The central ray is always traced; this picks the side rays inside the cone:",
                    "CENTER_ONLY = none; TRIANGLE = 3 (down, up-right, up-left with rayRollOffset 270);",
                    "CROSS = 4; RING = sideRays evenly spaced; DOUBLE_RING = innerRays at innerSpread + sideRays",
                    "at coneSpread; FAN_HORIZONTAL / FAN_VERTICAL = sideRays along a line (wide sweep);",
                    "CUSTOM = customRays. At most 24 side rays. More rays cost more chunk rebuilds while moving",
                    "(see moves/s in the debug overlay).")
            .defineEnum("rayPattern", RayLayout.Pattern.TRIANGLE);
    public static final ModConfigSpec.IntValue SIDE_RAYS = B
            .comment("Side ray count for RING, the outer ring of DOUBLE_RING and the FAN patterns.")
            .defineInRange("sideRays", 6, 1, RayLayout.MAX_SIDE_RAYS);
    public static final ModConfigSpec.DoubleValue RAY_ROLL_OFFSET = B
            .comment("Roll (degrees) of the first side ray around the beam axis: 0 = right, 90 = up, 270 = down,",
                    "counter-clockwise. Ignored by the FAN patterns.")
            .defineInRange("rayRollOffset", 270.0, 0.0, 360.0);
    public static final ModConfigSpec.IntValue INNER_RAYS = B
            .comment("Inner ring ray count for DOUBLE_RING (offset by half a step from the outer ring).")
            .defineInRange("innerRays", 3, 1, 12);
    public static final ModConfigSpec.DoubleValue INNER_SPREAD = B
            .comment("Inner ring tilt for DOUBLE_RING as a fraction of the beam half-angle.")
            .defineInRange("innerSpread", 0.3, 0.0, 1.0);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CUSTOM_RAYS = B
            .comment("Side rays for CUSTOM, one entry per ray: \"spread,roll[,lumOffset[,midpoints[,rangeFactor]]]\".",
                    "spread 0..1 (fraction of the half-angle), roll in degrees (0 = right, 90 = up, 270 = down),",
                    "lumOffset -15..15, midpoints true|false, rangeFactor 0.1..1. Missing fields use",
                    "sideLuminanceOffset, sideMidpoints and sideRangeFactor. Invalid entries are skipped (logged).",
                    "Example: \"0.9,0,-4,false,0.8\".")
            .defineListAllowEmpty("customRays", List.of("0.6,270", "0.6,30", "0.6,150"), () -> "0.6,270",
                    o -> o instanceof String);
    public static final ModConfigSpec.DoubleValue CONE_SPREAD = B
            .comment("Side ray tilt as a fraction of the beam half-angle (outer ring and fan maximum).")
            .defineInRange("coneSpread", 0.6, 0.0, 1.0);
    public static final ModConfigSpec.IntValue SIDE_LUMINANCE_OFFSET = B
            .comment("Side ray light level relative to the beam (negative = dimmer).")
            .defineInRange("sideLuminanceOffset", -2, -15, 0);
    public static final ModConfigSpec.BooleanValue SIDE_MIDPOINTS = B
            .comment("Add midpoints along the side rays too.")
            .define("sideMidpoints", false);
    public static final ModConfigSpec.DoubleValue SIDE_RANGE_FACTOR = B
            .comment("Side ray range as a fraction of the beam range.")
            .defineInRange("sideRangeFactor", 1.0, 0.1, 1.0);
    public static final ModConfigSpec.IntValue MAX_SOURCES_PER_ENTITY = B
            .comment("Max lights for all rays of one emitter; central ray points are placed first.")
            .defineInRange("maxSourcesPerEntity", 16, 1, 64);

    public static final ModConfigSpec.DoubleValue MOVE_THRESHOLD = B
            .comment("A light moves only when its target is at least this far (blocks). Each move rebuilds chunks.")
            .defineInRange("moveThreshold", 0.25, 0.0, 4.0);
    public static final ModConfigSpec.DoubleValue MERGE_DISTANCE = B
            .comment("Lights closer than this (blocks) to an accepted light are dropped.")
            .defineInRange("mergeDistance", 3.0, 0.0, 16.0);

    // Smoothing: glide small moves, crossfade jumps. Fades add a few chunk rebuilds only at the moment of a jump.
    public static final ModConfigSpec.BooleanValue SMOOTHING = B
            .comment("Smooth light movement: small moves glide, jumps crossfade (old spot fades out, new one fades in),",
                    "new lights fade in and vanished lights fade out. Fades add a few chunk rebuilds only at the",
                    "moment of a jump.")
            .define("smoothing", true);
    public static final ModConfigSpec.DoubleValue SMOOTH_FACTOR = B
            .comment("Fraction of the remaining distance a light glides per tick (1 = no glide).")
            .defineInRange("smoothFactor", 0.5, 0.1, 1.0);
    public static final ModConfigSpec.DoubleValue JUMP_DISTANCE = B
            .comment("A target this far (blocks) from the shown light is a jump: crossfade instead of glide.")
            .defineInRange("jumpDistance", 3.0, 1.0, 16.0);
    public static final ModConfigSpec.IntValue FADE_TICKS = B
            .comment("Ticks for a light to fade in or out.")
            .defineInRange("fadeTicks", 4, 1, 20);

    public static final ModConfigSpec.BooleanValue DEBUG_OVERLAY = B
            .comment("Show the debug text overlay (also /beamlights debug overlay on|off).")
            .define("debugOverlay", false);
    public static final ModConfigSpec.BooleanValue DEBUG_RENDER = B
            .comment("Draw rays and light points in the world (also /beamlights debug render on|off).")
            .define("debugRender", false);

    // Performance: the main cost is Sodium chunk-section rebuilds, one batch per light source change.
    static {
        B.comment("Performance. Every light source move or luminance change makes Sodium rebuild up to 8 chunk",
                "sections; these keys cut the number of changes. Presets: /beamlights perf preset ...").push("performance");
    }

    public static final ModConfigSpec.BooleanValue SNAP_TO_BLOCK = B
            .comment("Place lights at block centers: a light only moves when it enters another block (Sodium Dynamic",
                    "Lights lights whole blocks anyway). Off = free positions with moveThreshold.")
            .define("snapToBlock", true);
    public static final ModConfigSpec.IntValue MAX_MOVES_PER_TICK = B
            .comment("Max light source changes (move, luminance change, new, removed) applied per tick; the rest waits",
                    "for the next tick. Local player's central ray first, then nearest. 0 = unlimited.")
            .defineInRange("maxMovesPerTick", 24, 0, 512);
    public static final ModConfigSpec.IntValue REMOTE_UPDATE_INTERVAL = B
            .comment("Beams of other players and entities are re-traced every N ticks (staggered by entity id); in",
                    "between their last light points are reused. 1 = every tick.")
            .defineInRange("remoteUpdateInterval", 2, 1, 20);
    public static final ModConfigSpec.IntValue LOD_DISTANCE = B
            .comment("Emitters farther than this (blocks) use the central ray only (no side rays, no midpoints).",
                    "The local player is never reduced. 0 = off.")
            .defineInRange("lodDistance", 24, 0, 256);
    public static final ModConfigSpec.BooleanValue MERGE_SAME_SECTION = B
            .comment("Drop a midpoint that shares its 16x16x16 chunk section with an accepted light (fewer lights,",
                    "coarser look). Points in the same block are always merged.")
            .define("mergeSameSection", false);

    static {
        B.pop();
    }

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
