package it.ratlab.beamlights.core;

import java.util.List;
import java.util.Locale;

/** Value ranges of the beamlights:beam item component (same as datapack definitions). */
public final class BeamValues {
    public static final int MAX_LUMINANCE = 15;
    public static final double MIN_RANGE = 1, MAX_RANGE = 128;
    public static final double MIN_CONE = 1, MAX_CONE = 89;
    public static final double MAX_OFFSET = 4;

    private BeamValues() {
    }

    /** Null when valid, else a readable reason. Slots are slot ids (mainhand, offhand, head, curios). */
    public static String check(int luminance, double range, double cone, double forward, double down,
                               List<String> slots) {
        if (luminance < 0 || luminance > MAX_LUMINANCE) return "luminance " + luminance + " out of range 0..15";
        // Negated comparisons also reject NaN.
        if (!(range >= MIN_RANGE && range <= MAX_RANGE)) return "range " + range + " out of range 1..128";
        if (!(cone >= MIN_CONE && cone <= MAX_CONE)) return "cone " + cone + " out of range 1..89";
        if (!(Math.abs(forward) <= MAX_OFFSET)) return "origin.forward " + forward + " out of range -4..4";
        if (!(Math.abs(down) <= MAX_OFFSET)) return "origin.down " + down + " out of range -4..4";
        if (slots == null || slots.isEmpty()) return "slots is empty";
        for (String s : slots) {
            try {
                BeamDefinition.Slot.parse(s.toLowerCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return e.getMessage();
            }
        }
        return null;
    }
}
