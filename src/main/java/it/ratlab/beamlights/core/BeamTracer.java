package it.ratlab.beamlights.core;

import it.ratlab.beamlights.core.math.V3;

/** Voxel ray march (Amanatides-Woo). Stops on the first block the test rejects; the start voxel is never tested. */
public final class BeamTracer {
    @FunctionalInterface
    public interface BlockTest {
        /** True when the block at these coordinates stops the beam (opaque). */
        boolean stops(int x, int y, int z);
    }

    /**
     * hit = true: point is the entry point on the stopping block's face, distance the ray length to it.
     * hit = false: point is the end of the range (block coordinates are meaningless).
     */
    public record Result(boolean hit, V3 point, double distance, int blockX, int blockY, int blockZ) {
    }

    private BeamTracer() {
    }

    public static Result trace(V3 origin, V3 direction, double range, BlockTest test) {
        V3 dir = direction.normalize();
        if (dir.lengthSq() == 0 || range <= 0) return new Result(false, origin, 0, 0, 0, 0);

        int x = (int) Math.floor(origin.x());
        int y = (int) Math.floor(origin.y());
        int z = (int) Math.floor(origin.z());
        int stepX = (int) Math.signum(dir.x());
        int stepY = (int) Math.signum(dir.y());
        int stepZ = (int) Math.signum(dir.z());
        double tDeltaX = stepX == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dir.x());
        double tDeltaY = stepY == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dir.y());
        double tDeltaZ = stepZ == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dir.z());
        double tMaxX = firstBoundary(origin.x(), x, stepX, dir.x());
        double tMaxY = firstBoundary(origin.y(), y, stepY, dir.y());
        double tMaxZ = firstBoundary(origin.z(), z, stepZ, dir.z());

        while (true) {
            double t;
            if (tMaxX <= tMaxY && tMaxX <= tMaxZ) {
                t = tMaxX;
                x += stepX;
                tMaxX += tDeltaX;
            } else if (tMaxY <= tMaxZ) {
                t = tMaxY;
                y += stepY;
                tMaxY += tDeltaY;
            } else {
                t = tMaxZ;
                z += stepZ;
                tMaxZ += tDeltaZ;
            }
            if (t > range) return new Result(false, origin.add(dir.scale(range)), range, x, y, z);
            if (test.stops(x, y, z)) return new Result(true, origin.add(dir.scale(t)), t, x, y, z);
        }
    }

    private static double firstBoundary(double o, int cell, int step, double d) {
        if (step == 0) return Double.POSITIVE_INFINITY;
        double boundary = step > 0 ? cell + 1 - o : o - cell;
        return boundary / Math.abs(d);
    }
}
