package it.ratlab.beamlights.core;

import it.ratlab.beamlights.api.math.V3;

import java.util.ArrayList;
import java.util.List;

/** Turns one traced beam into light points: the hit point (slot 0) plus midpoints along the ray (slots 1..n). */
public final class LightPointPlanner {
    public enum Status { ACCEPTED, MERGED, SKIPPED_AIR, CAPPED }

    /**
     * @param sectionMerge also drop a midpoint that shares its 16x16x16 section with an accepted point (coarser,
     *                     fewer lights; points in the same block are always merged)
     */
    public record Settings(boolean midpoints, double midSpacing, int midLuminanceOffset, int maxPerBeam,
                           double mergeDistance, double hitBackoff, boolean sectionMerge) {
        public Settings(boolean midpoints, double midSpacing, int midLuminanceOffset, int maxPerBeam,
                        double mergeDistance, double hitBackoff) {
            this(midpoints, midSpacing, midLuminanceOffset, maxPerBeam, mergeDistance, hitBackoff, false);
        }

        public Settings withMidpoints(boolean on) {
            return on == midpoints ? this
                    : new Settings(on, midSpacing, midLuminanceOffset, maxPerBeam, mergeDistance, hitBackoff, sectionMerge);
        }
    }

    public record PlannedPoint(int slot, V3 pos, int luminance, Status status) {
    }

    public record Plan(List<PlannedPoint> points) {
        public List<PlannedPoint> accepted() {
            return points.stream().filter(p -> p.status() == Status.ACCEPTED).toList();
        }

        // Loop, not a stream: called four times per ray per tick for the stats.
        public int count(Status status) {
            int n = 0;
            for (PlannedPoint p : points) {
                if (p.status() == status) n++;
            }
            return n;
        }
    }

    @FunctionalInterface
    public interface OccluderProbe {
        /** True when an opaque block is close enough for a light here to matter. */
        boolean nearOccluder(V3 pos);
    }

    private LightPointPlanner() {
    }

    public static Plan plan(V3 origin, V3 direction, BeamTracer.Result trace, int luminance, Settings s,
                            OccluderProbe probe) {
        return plan(origin, direction, trace, luminance, s, probe, new ArrayList<>(), false);
    }

    /**
     * Cross-ray variant: points are merge-checked against and appended to {@code sharedAccepted}. With
     * {@code mergeHit} the hit point is merge-checked too. maxPerBeam counts only this ray's points.
     */
    public static Plan plan(V3 origin, V3 direction, BeamTracer.Result trace, int luminance, Settings s,
                            OccluderProbe probe, List<V3> sharedAccepted, boolean mergeHit) {
        List<PlannedPoint> out = new ArrayList<>();
        if (luminance <= 0) return new Plan(out);
        V3 dir = direction.normalize();
        double mergeSq = s.mergeDistance() * s.mergeDistance();
        int own = 0;

        if (trace.hit()) {
            V3 p = origin.add(dir.scale(Math.max(0, trace.distance() - s.hitBackoff())));
            Status st;
            if (mergeHit && isNear(sharedAccepted, p, mergeSq, false)) st = Status.MERGED;
            else if (s.maxPerBeam() < 1) st = Status.CAPPED;
            else {
                st = Status.ACCEPTED;
                sharedAccepted.add(p);
                own++;
            }
            out.add(new PlannedPoint(0, p, luminance, st));
        }

        if (s.midpoints() && s.midSpacing() > 0) {
            int midLum = Math.max(1, Math.min(15, luminance + s.midLuminanceOffset()));
            int slot = 1;
            // Slots stay below the ghost bit of Keys.
            for (double d = s.midSpacing(); d < trace.distance() && slot < Keys.GHOST_BIT;
                 d += s.midSpacing(), slot++) {
                V3 p = origin.add(dir.scale(d));
                Status st;
                if (isNear(sharedAccepted, p, mergeSq, s.sectionMerge())) st = Status.MERGED;
                else if (!probe.nearOccluder(p)) st = Status.SKIPPED_AIR;
                else if (own >= s.maxPerBeam()) st = Status.CAPPED;
                else {
                    st = Status.ACCEPTED;
                    sharedAccepted.add(p);
                    own++;
                }
                out.add(new PlannedPoint(slot, p, midLum, st));
            }
        }
        return new Plan(out);
    }

    // Within mergeDistance, in the same block (SDL lights per block anyway) or, with section, in the same section.
    private static boolean isNear(List<V3> accepted, V3 p, double mergeSq, boolean section) {
        int bx = floor(p.x()), by = floor(p.y()), bz = floor(p.z());
        for (V3 a : accepted) {
            if (a.distSq(p) < mergeSq) return true;
            int ax = floor(a.x()), ay = floor(a.y()), az = floor(a.z());
            if (ax == bx && ay == by && az == bz) return true;
            if (section && ax >> 4 == bx >> 4 && ay >> 4 == by >> 4 && az >> 4 == bz >> 4) return true;
        }
        return false;
    }

    private static int floor(double v) {
        return (int) Math.floor(v);
    }
}
