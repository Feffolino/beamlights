package it.ratlab.beamlights.config;

import it.ratlab.beamlights.core.ConeLight;
import it.ratlab.beamlights.core.ConePolicy;
import it.ratlab.beamlights.core.MotionGovernor;
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
    public static final ModConfigSpec.IntValue FADE_STEPS = B
            .comment("Distinct light levels of a fade in or out, whatever fadeTicks is (2 = half, then full or off).",
                    "Each level is one chunk rebuild per light. 0 = one level per tick.")
            .defineInRange("fadeSteps", 2, 0, 20);
    public static final ModConfigSpec.IntValue GLIDE_MIN_TICKS = B
            .comment("A gliding light changes at most once every N ticks (with snapToBlock only block changes count).")
            .defineInRange("glideMinTicks", 2, 1, 20);
    public static final ModConfigSpec.IntValue CONE_STEPS = B
            .comment("LDL cone light: a length jump (near face to far face, >= jumpDistance) is walked in this many equal",
                    "steps, one every glideMinTicks ticks. Smaller changes lerp by smoothFactor. Needs smoothing on.")
            .defineInRange("coneSteps", 3, 1, 8);

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
            .defineInRange("maxMovesPerTick", 6, 0, 512);
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
    public static final ModConfigSpec.DoubleValue MOVE_HYSTERESIS = B
            .comment("A light (midpoint, side ray) moves only when its target is at least this far (blocks) from the",
                    "shown position; with snapToBlock: another block AND this distance. A light reaches 7.75 blocks,",
                    "so a lag of 1-1.5 blocks is barely visible and cuts most chunk rebuilds of a sweeping beam.")
            .defineInRange("moveHysteresis", 1.5, 0.0, 8.0);
    public static final ModConfigSpec.DoubleValue CENTRAL_HYSTERESIS = B
            .comment("Same as moveHysteresis for the hit point of the central ray (the spot you look at).")
            .defineInRange("centralHysteresis", 0.75, 0.0, 8.0);
    public static final ModConfigSpec.IntValue LUMINANCE_HYSTERESIS = B
            .comment("Luminance changes up to this many levels are ignored for midpoints and side rays (the central",
                    "hit point is always exact); applied together with the next move.")
            .defineInRange("luminanceHysteresis", 1, 0, 4);
    public static final ModConfigSpec.BooleanValue LDL_CONE_LIGHT = B
            .comment("LambDynamicLights only: also light a cone along each central beam (fades to coneEndFactor at the",
                    "hit point, no occlusion; shape and update rules in [cone]). Ignored with Sodium Dynamic Lights.",
                    "More section rebuilds while turning.")
            .define("ldlConeLight", false);
    public static final ModConfigSpec.IntValue LDL_CONE_LUMINANCE_OFFSET = B
            .comment("Luminance of the ldlConeLight cone relative to the beam (at the emitter).")
            .defineInRange("ldlConeLuminanceOffset", -3, -15, 0);

    static {
        B.pop();
    }

    // Motion: per emitter, side rays and midpoints update less while the beam turns or moves fast.
    static {
        B.comment("Motion-adaptive updates, per emitter (local player and every other emitter on its own). While the",
                "beam turns or moves fast only the central hit point follows; side rays and midpoints keep their",
                "lights. Presets: /beamlights perf preset ...; keys: /beamlights motion ...").push("motion");
    }

    public static final ModConfigSpec.DoubleValue FAST_TURN = B
            .comment("FAST at this angular speed of the beam (degrees/s) or more; left below 0.7 x this value.")
            .defineInRange("fastTurnDegPerSec", 90.0, 1.0, 3600.0);
    public static final ModConfigSpec.DoubleValue SLOW_TURN = B
            .comment("Below this angular speed (degrees/s) and fastMoveBlocksPerSec / 4 the emitter counts as slow;",
                    "settleTicks slow ticks in a row = STILL. Between slow and fast = MOVING.")
            .defineInRange("slowTurnDegPerSec", 20.0, 0.0, 3600.0);
    public static final ModConfigSpec.DoubleValue FAST_MOVE = B
            .comment("FAST at this origin speed (blocks/s) or more (sprinting is about 5.6).")
            .defineInRange("fastMoveBlocksPerSec", 8.0, 0.5, 200.0);
    public static final ModConfigSpec.IntValue SIDE_UPDATE_TICKS = B
            .comment("While MOVING, side rays and midpoints change at most every N ticks (central hit point every tick).")
            .defineInRange("sideUpdateTicks", 3, 1, 40);
    public static final ModConfigSpec.IntValue SETTLE_TICKS = B
            .comment("Slow ticks in a row before STILL; on reaching STILL every light goes to its exact target",
                    "(no hysteresis) for this many ticks.")
            .defineInRange("settleTicks", 6, 1, 40);
    public static final ModConfigSpec.EnumValue<MotionGovernor.SideMode> FAST_SIDE_MODE = B
            .comment("Side rays and midpoints while FAST: FREEZE = keep their lights; FADE = fade them out;",
                    "OFF = no motion adaptation (they follow like the central ray).")
            .defineEnum("fastSideMode", MotionGovernor.SideMode.FREEZE);

    static {
        B.pop();
    }

    // Cone light (LambDynamicLights): size caps, falloff look and update rules of the ldlConeLight cone.
    static {
        B.comment("Cone light (LambDynamicLights). Shape and update rules of the ldlConeLight cone (ldlConeLight and",
                "ldlConeLuminanceOffset stay in [performance]). A big cone covers a big bounding box: LDL evaluates",
                "it for every entity and particle inside and rebuilds every section of the box on each change.",
                "Keys: /beamlights cone ...; presets: /beamlights cone preset light|balanced|wide.").push("cone");
    }

    public static final ModConfigSpec.DoubleValue CONE_MAX_ANGLE = B
            .comment("Half-angle cap (degrees) of the light cone; the visual beam keeps its own angle.")
            .defineInRange("coneMaxAngle", 25.0, 5.0, 60.0);
    public static final ModConfigSpec.DoubleValue CONE_MAX_LENGTH = B
            .comment("Length cap (blocks) of the light cone.")
            .defineInRange("coneMaxLength", 16.0, 4.0, 48.0);
    public static final ModConfigSpec.DoubleValue CONE_MIN_LENGTH = B
            .comment("Beams shorter than this (blocks, to the hit point) get no cone, only the point lights.")
            .defineInRange("coneMinLength", 3.0, 0.0, 16.0);
    public static final ModConfigSpec.DoubleValue CONE_MAX_DISTANCE = B
            .comment("Hit points farther than this (blocks), or no hit (open sky), get no cone: it fades out and only",
                    "the point lights remain.")
            .defineInRange("coneMaxDistanceForCone", 20.0, 4.0, 64.0);
    public static final ModConfigSpec.DoubleValue CONE_END_FACTOR = B
            .comment("Cone level at the end relative to the apex.")
            .defineInRange("coneEndFactor", 0.5, 0.1, 1.0);
    public static final ModConfigSpec.DoubleValue CONE_EDGE_SOFTNESS = B
            .comment("Soft edge: the outer fraction of the cone radius fades to half the level at the rim (0 = hard).")
            .defineInRange("coneEdgeSoftness", 0.25, 0.0, 1.0);
    public static final ModConfigSpec.BooleanValue CONE_FREEZE_WHEN_FAST = B
            .comment("Keep the cone where it is while the emitter turns or moves fast (motion state FAST).")
            .define("coneFreezeWhenFast", true);
    public static final ModConfigSpec.DoubleValue CONE_LENGTH_HYSTERESIS = B
            .comment("The cone changes only when its length moved at least this much (blocks), or apex / direction",
                    "moved past their hysteresis, or its luminance changed.")
            .defineInRange("coneLengthHysteresis", 2.0, 0.0, 8.0);
    public static final ModConfigSpec.DoubleValue CONE_APEX_HYSTERESIS = B
            .comment("Apex move (blocks) that changes the cone.")
            .defineInRange("coneApexHysteresis", 1.0, 0.0, 8.0);
    public static final ModConfigSpec.DoubleValue CONE_ANGLE_HYSTERESIS = B
            .comment("Direction change (degrees) that changes the cone.")
            .defineInRange("coneAngleHysteresis", 4.0, 0.0, 45.0);

    static {
        B.pop();
    }

    // Gamma mask: screen-space beam for the local player (vanilla rendering only).
    static {
        B.comment("Gamma mask: the local player's beam is drawn as a screen-space brightening of the cone (depth",
                "buffer, no chunk rebuilds). Visual only; the physical light keeps just the central hit point. Off",
                "automatically with an Iris shader pack, in third person or if the pass fails. Keys: /beamlights mask ...")
                .push("mask");
    }

    public static final ModConfigSpec.BooleanValue GAMMA_MASK = B
            .comment("Draw the local player's beam with the gamma mask.")
            .define("gammaMask", true);
    public static final ModConfigSpec.DoubleValue MASK_STRENGTH = B
            .comment("Mask strength at full beam luminance (scaled by luminance/15).")
            .defineInRange("maskStrength", 1.0, 0.0, 3.0);
    public static final ModConfigSpec.DoubleValue MASK_GAMMA = B
            .comment("Gamma lift: colour^(1/(1+maskGamma*mask)). Higher = dark surfaces brighter.")
            .defineInRange("maskGamma", 2.5, 0.0, 8.0);
    public static final ModConfigSpec.DoubleValue MASK_LIFT = B
            .comment("Additive lift so pure black is lit too.")
            .defineInRange("maskLift", 0.04, 0.0, 0.3);
    public static final ModConfigSpec.DoubleValue MASK_ANGLE_SCALE = B
            .comment("Mask half-angle relative to the beam's cone half-angle.")
            .defineInRange("maskAngleScale", 1.0, 0.2, 3.0);
    public static final ModConfigSpec.DoubleValue MASK_SOFTNESS = B
            .comment("Soft edge: fraction of the half-angle that fades out (0 = hard edge).")
            .defineInRange("maskSoftness", 0.35, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue MASK_RANGE_SCALE = B
            .comment("Mask reach relative to the beam range.")
            .defineInRange("maskRangeScale", 1.0, 0.1, 2.0);
    public static final ModConfigSpec.DoubleValue MASK_FALLOFF = B
            .comment("Distance falloff exponent of (1 - dist/range); 0 = flat.")
            .defineInRange("maskFalloff", 0.7, 0.0, 4.0);
    public static final ModConfigSpec.DoubleValue MASK_TINT = B
            .comment("How much the beam colour tints the additive lift (0 = white).")
            .defineInRange("maskTint", 0.25, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue MASK_FADE_SECONDS = B
            .comment("Seconds to fade the mask fully in or out (toggle, flicker).")
            .defineInRange("maskFadeSeconds", 0.15, 0.0, 2.0);
    public static final ModConfigSpec.BooleanValue MASK_MIDPOINTS = B
            .comment("While the mask is active, keep midpoints of the central ray as physical lights.")
            .define("maskMidpoints", false);

    static {
        B.pop();
    }

    public static final ModConfigSpec SPEC = B.build();

    private BeamClientConfig() {
    }

    public static MotionGovernor.Settings motionSettings() {
        return new MotionGovernor.Settings(FAST_TURN.get(), SLOW_TURN.get(), FAST_MOVE.get(), SIDE_UPDATE_TICKS.get(),
                SETTLE_TICKS.get(), FAST_SIDE_MODE.get());
    }

    public static ConePolicy.Settings coneSettings() {
        return new ConePolicy.Settings(CONE_MAX_ANGLE.get(), CONE_MAX_LENGTH.get(), CONE_MIN_LENGTH.get(),
                CONE_MAX_DISTANCE.get(), CONE_LENGTH_HYSTERESIS.get(), CONE_APEX_HYSTERESIS.get(),
                CONE_ANGLE_HYSTERESIS.get(), CONE_FREEZE_WHEN_FAST.get());
    }

    public static ConeLight.Look coneLook() {
        return new ConeLight.Look(CONE_END_FACTOR.get(), CONE_EDGE_SOFTNESS.get());
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
