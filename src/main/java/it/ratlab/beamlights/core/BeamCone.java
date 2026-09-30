package it.ratlab.beamlights.core;

import it.ratlab.beamlights.core.math.V3;

/** Point-in-beam tests used by server spawn blocking. */
public final class BeamCone {
    private static final double TARGET_SLACK = 0.5;

    private BeamCone() {
    }

    /** True when point is within range and at most halfAngleDeg off the beam axis (origin itself counts as inside). */
    public static boolean inCone(V3 origin, V3 dir, double halfAngleDeg, double range, V3 point) {
        V3 d = point.sub(origin);
        double distSq = d.lengthSq();
        if (distSq < 1e-12) return true;
        if (distSq > range * range) return false;
        V3 axis = dir.normalize();
        if (axis.lengthSq() == 0) return false;
        double cos = axis.dot(d) / Math.sqrt(distSq);
        return cos >= Math.cos(Math.toRadians(halfAngleDeg));
    }

    /** True when no stopping block lies between origin and point; the block containing point does not count. */
    public static boolean visible(V3 origin, V3 point, BeamTracer.BlockTest test) {
        V3 d = point.sub(origin);
        double dist = d.length();
        if (dist < 1e-6) return true;
        BeamTracer.Result r = BeamTracer.trace(origin, d, dist, test);
        return !r.hit() || r.distance() >= dist - TARGET_SLACK;
    }
}
