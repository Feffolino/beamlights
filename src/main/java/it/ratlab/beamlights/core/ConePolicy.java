package it.ratlab.beamlights.core;

import it.ratlab.beamlights.api.math.V3;

/**
 * Which cone light a central beam gets (LDL ldlConeLight) and when it may change. Outdoors a long, wide cone covers a
 * huge bounding box: LDL evaluates it for every entity and particle nearby and rebuilds every section of the box on each
 * change. So the cone is capped (half-angle, length), skipped for far hits, open sky and short beams (the point lights
 * remain), held by hysteresis and frozen while the emitter turns fast. Pure logic, client thread only.
 */
public final class ConePolicy {
    /**
     * maxAngleDeg: half-angle cap of the light cone (independent of the visual beam); maxLength / minLength: length cap
     * and the shortest beam that gets a cone; maxDistance: farther hits (or no hit) get no cone; length / apex /
     * angle hysteresis: smaller changes keep the shown cone; freezeWhenFast: keep the cone while MotionGovernor is FAST.
     */
    public record Settings(double maxAngleDeg, double maxLength, double minLength, double maxDistance,
                           double lengthHysteresis, double apexHysteresis, double angleHysteresisDeg,
                           boolean freezeWhenFast) {
        public static final Settings BALANCED = new Settings(25, 16, 3, 20, 2.0, 1.0, 4.0, true);
        public static final Settings LIGHT = new Settings(18, 12, 3, 14, 2.0, 1.0, 4.0, true);
        public static final Settings WIDE = new Settings(35, 24, 3, 28, 2.0, 1.0, 4.0, true);
    }

    private ConePolicy() {
    }

    /**
     * The cone of a central beam, or null for none: no hit, hit farther than maxDistance, or length (hit distance minus
     * backoff, capped at maxLength) below minLength.
     */
    public static ConeLight.Shape target(V3 origin, V3 dir, boolean hit, double hitDistance, double backoff,
                                         double beamHalfAngleDeg, int luminance, Settings s) {
        if (luminance <= 0 || !hit || hitDistance > s.maxDistance()) return null;
        double len = Math.min(Math.max(0, hitDistance - backoff), s.maxLength());
        if (len < s.minLength()) return null;
        return new ConeLight.Shape(origin, dir, len, Math.min(beamHalfAngleDeg, s.maxAngleDeg()), luminance);
    }

    /**
     * Hysteresis: the cone to show given the one shown (held, may be null) and the fresh target (may be null). Frozen
     * (held kept) while fast and freezeWhenFast; otherwise the fresh cone replaces the held one only when its length,
     * apex or direction moved at least the hysteresis, or its luminance or half-angle changed. Null = no cone.
     */
    public static ConeLight.Shape gate(ConeLight.Shape held, ConeLight.Shape fresh, boolean fast, Settings s) {
        if (held == null) return fresh;
        if (fast && s.freezeWhenFast()) return held;
        if (fresh == null) return null;
        if (fresh.luminance() != held.luminance()) return fresh;
        if (Math.abs(fresh.halfAngleDeg() - held.halfAngleDeg()) >= 1.0) return fresh;
        if (Math.abs(fresh.length() - held.length()) >= s.lengthHysteresis()) return fresh;
        double ah = s.apexHysteresis();
        if (fresh.origin().distSq(held.origin()) >= ah * ah) return fresh;
        if (MotionGovernor.angleDeg(fresh.axis(), held.axis()) >= s.angleHysteresisDeg()) return fresh;
        return held;
    }
}
