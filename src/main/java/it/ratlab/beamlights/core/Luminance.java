package it.ratlab.beamlights.core;

/** Light level math shared by providers. */
public final class Luminance {
    private Luminance() {
    }

    /**
     * @param base            luminance for the bulb tier (0..15)
     * @param charge01        remaining charge 0..1 (NaN = unknown, treated as full)
     * @param minFactor       floor of the battery factor
     * @param flicker         multiplier from the flicker effect (clamped to 0..1)
     */
    public static int compute(int base, boolean scaleWithBattery, double charge01, double minFactor, float flicker) {
        double v = base;
        if (scaleWithBattery) {
            double c = Double.isNaN(charge01) ? 1.0 : clamp(charge01, 0, 1);
            v *= Math.max(minFactor, c);
        }
        v *= clamp(flicker, 0, 1);
        return (int) Math.round(clamp(v, 0, 15));
    }

    /** Bulb tier (1..3) whose multiplier is closest to mul. */
    public static int tierFromMultiplier(float mul, float improved, float highQuality) {
        float d1 = Math.abs(mul - 1f), d2 = Math.abs(mul - improved), d3 = Math.abs(mul - highQuality);
        if (d3 <= d2 && d3 <= d1) return 3;
        if (d2 <= d1) return 2;
        return 1;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
