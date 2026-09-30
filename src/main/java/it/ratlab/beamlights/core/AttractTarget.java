package it.ratlab.beamlights.core;

import it.ratlab.beamlights.core.math.V3;

/** Pure rules for server mob attraction. */
public final class AttractTarget {
    private static final double BACK_OFF = 0.5;

    private AttractTarget() {
    }

    /** Point a mob should walk to: the beam hit point backed off along -dir, so it lies in front of the block face. */
    public static V3 litPoint(V3 hit, V3 dir) {
        V3 d = dir.normalize();
        if (d.lengthSq() == 0) return hit;
        return hit.sub(d.scale(BACK_OFF));
    }

    /** True when there is no previous path target or the lit point moved at least minDist away from it. */
    public static boolean shouldRepath(V3 last, V3 now, double minDist) {
        return last == null || last.distSq(now) >= minDist * minDist;
    }

    /** True when the mob is already close enough to the point to leave it alone. */
    public static boolean arrived(V3 mob, V3 point, double radius) {
        return mob.distSq(point) <= radius * radius;
    }
}
