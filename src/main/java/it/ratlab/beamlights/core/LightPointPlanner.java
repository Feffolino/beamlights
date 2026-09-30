package it.ratlab.beamlights.core;

import it.ratlab.beamlights.core.math.V3;

import java.util.ArrayList;
import java.util.List;

/** Turns one traced beam into light points: the hit point (slot 0) plus midpoints along the ray (slots 1..n). */
public final class LightPointPlanner {
    public enum Status { ACCEPTED, MERGED, SKIPPED_AIR, CAPPED }

    public record Settings(boolean midpoints, double midSpacing, int midLuminanceOffset, int maxPerBeam,
                           double mergeDistance, double hitBackoff) {
    }

    public record PlannedPoint(int slot, V3 pos, int luminance, Status status) {
    }

    public record Plan(List<PlannedPoint> points) {
        public List<PlannedPoint> accepted() {
            return points.stream().filter(p -> p.status() == Status.ACCEPTED).toList();
        }

        public int count(Status status) {
            return (int) points.stream().filter(p -> p.status() == status).count();
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
        List<PlannedPoint> out = new ArrayList<>();
        if (luminance <= 0) return new Plan(out);
        V3 dir = direction.normalize();
        List<V3> accepted = new ArrayList<>();

        if (trace.hit()) {
            V3 p = origin.add(dir.scale(Math.max(0, trace.distance() - s.hitBackoff())));
            Status st = s.maxPerBeam() < 1 ? Status.CAPPED : Status.ACCEPTED;
            if (st == Status.ACCEPTED) accepted.add(p);
            out.add(new PlannedPoint(0, p, luminance, st));
        }

        if (s.midpoints() && s.midSpacing() > 0) {
            int midLum = Math.max(1, Math.min(15, luminance + s.midLuminanceOffset()));
            double mergeSq = s.mergeDistance() * s.mergeDistance();
            int slot = 1;
            for (double d = s.midSpacing(); d < trace.distance() && slot < 256; d += s.midSpacing(), slot++) {
                V3 p = origin.add(dir.scale(d));
                Status st;
                if (isNear(accepted, p, mergeSq)) st = Status.MERGED;
                else if (!probe.nearOccluder(p)) st = Status.SKIPPED_AIR;
                else if (accepted.size() >= s.maxPerBeam()) st = Status.CAPPED;
                else {
                    st = Status.ACCEPTED;
                    accepted.add(p);
                }
                out.add(new PlannedPoint(slot, p, midLum, st));
            }
        }
        return new Plan(out);
    }

    private static boolean isNear(List<V3> accepted, V3 p, double mergeSq) {
        for (V3 a : accepted) {
            if (a.distSq(p) < mergeSq) return true;
        }
        return false;
    }
}
