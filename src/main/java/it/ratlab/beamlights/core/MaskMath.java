package it.ratlab.beamlights.core;

/**
 * Math of the gamma mask; the fragment shader assets/beamlights/shaders/core/gamma_mask.fsh mirrors mask() and lift().
 * Pure, no Minecraft classes.
 */
public final class MaskMath {
    private MaskMath() {
    }

    /** GLSL smoothstep. */
    public static double smoothstep(double e0, double e1, double x) {
        if (e1 == e0) return x < e0 ? 0 : 1;
        double t = Math.max(0, Math.min(1, (x - e0) / (e1 - e0)));
        return t * t * (3 - 2 * t);
    }

    /** Cosine of the outer half-angle (degrees, clamped to 1..89). */
    public static double cosOuter(double coneDeg, double angleScale) {
        return Math.cos(Math.toRadians(clampAngle(coneDeg * angleScale)));
    }

    /** Cosine of the inner (full strength) half-angle: outer * (1 - softness). */
    public static double cosInner(double coneDeg, double angleScale, double softness) {
        double outer = clampAngle(coneDeg * angleScale);
        return Math.cos(Math.toRadians(outer * (1 - Math.max(0, Math.min(1, softness)))));
    }

    private static double clampAngle(double deg) {
        return Math.max(1, Math.min(89, deg));
    }

    /**
     * Mask 0..1 for a point at distance dist whose direction makes cosA with the beam axis.
     * falloff = exponent of (1 - dist/range); 0 = flat up to range.
     */
    public static double mask(double cosA, double dist, double cosOuter, double cosInner, double range,
                              double falloff) {
        if (range <= 0 || dist >= range) return 0;
        double cone = smoothstep(cosOuter, cosInner, cosA);
        double d = Math.pow(1 - dist / range, falloff);
        return cone * d;
    }

    /** One channel: gamma lift then a small additive lift so black is lit too. */
    public static double lift(double c, double m, double gamma, double lift, double tint) {
        double out = Math.pow(Math.max(0, c), 1 / (1 + gamma * m)) + lift * m * tint;
        return Math.min(1, out);
    }

    /** Moves current toward target by at most dt/seconds (seconds <= 0 = jump). */
    public static double approach(double current, double target, double dt, double seconds) {
        if (seconds <= 0) return target;
        double step = dt / seconds;
        if (Math.abs(target - current) <= step) return target;
        return current + Math.signum(target - current) * step;
    }
}
