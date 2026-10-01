package it.ratlab.beamlights.core;

import it.ratlab.beamlights.api.math.V3;

/**
 * Cone-shaped light along a central beam (LambDynamicLights backend, config ldlConeLight). Same falloff convention as
 * point lights: light = level - falloffRatio * distance, with LDL's ratio 15 / 7.75 (light 15 reaches 7.75 blocks).
 * Inside the cone the level fades from the full luminance at the apex to half at the end; outside it, light falls off
 * with the distance from the cone surface (behind the apex: from the apex, past the end: from the end disc's axis point).
 * Pure math, no occlusion: the length should stop at the beam's first hit.
 */
public final class ConeLight {
    /** LambDynamicLights' falloff ratio (blocks per light level): 15 / 7.75. */
    public static final double LDL_FALLOFF = 15.0 / 7.75;
    /** Keys slot of a cone light (planner slots stay far below; bit 7 = ghost stays clear). */
    public static final int SLOT = 0x7F;
    /** Level at the cone end relative to the apex. */
    static final double END_FACTOR = 0.5;
    /** Half angles are clamped to this, so the cone radius stays bounded. */
    static final double MAX_HALF_ANGLE_DEG = 60.0;

    /** One cone: apex, unit axis, length (blocks), half angle (degrees), luminance at the apex. */
    public record Shape(V3 origin, V3 axis, double length, double halfAngleDeg, int luminance) {
        public Shape {
            double l = axis.length();
            axis = l > 1e-9 ? axis.scale(1.0 / l) : new V3(0, 1, 0);
            length = Math.max(0, length);
            halfAngleDeg = Math.max(0, Math.min(MAX_HALF_ANGLE_DEG, halfAngleDeg));
            luminance = Math.max(0, Math.min(15, luminance));
        }

        public V3 end() {
            return origin.add(axis.scale(length));
        }

        public double endRadius() {
            return length * Math.tan(Math.toRadians(halfAngleDeg));
        }
    }

    /** Integer block bounds (inclusive) of every block the cone can light. */
    public record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public boolean contains(int x, int y, int z) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }
    }

    private ConeLight() {
    }

    /** Light level (may be negative = none) at point (px, py, pz). */
    public static double lightAt(Shape s, double px, double py, double pz, double falloffRatio) {
        double vx = px - s.origin().x(), vy = py - s.origin().y(), vz = pz - s.origin().z();
        double t = vx * s.axis().x() + vy * s.axis().y() + vz * s.axis().z();
        double vSq = vx * vx + vy * vy + vz * vz;
        int lum = s.luminance();
        double len = s.length();
        if (t <= 0 || len <= 0) return lum - falloffRatio * Math.sqrt(vSq);
        double tc = Math.min(t, len);
        double level = lum * (1.0 - (1.0 - END_FACTOR) * tc / Math.max(len, 1e-9));
        double perp = Math.sqrt(Math.max(0, vSq - t * t));
        double radius = tc * Math.tan(Math.toRadians(s.halfAngleDeg()));
        double outside = Math.max(0, perp - radius);
        double beyond = t - tc;
        double dist = Math.sqrt(outside * outside + beyond * beyond);
        return level - falloffRatio * dist;
    }

    /** Light at the center of block (x, y, z). */
    public static double lightAtBlock(Shape s, int x, int y, int z, double falloffRatio) {
        return lightAt(s, x + 0.5, y + 0.5, z + 0.5, falloffRatio);
    }

    /** Block bounds covering the cone plus its falloff reach. */
    public static Box bounds(Shape s, double falloffRatio) {
        double reach = s.luminance() / falloffRatio;
        double grow = s.endRadius() + reach;
        V3 o = s.origin();
        V3 e = s.end();
        return new Box(
                (int) Math.floor(Math.min(o.x() - reach, e.x() - grow)),
                (int) Math.floor(Math.min(o.y() - reach, e.y() - grow)),
                (int) Math.floor(Math.min(o.z() - reach, e.z() - grow)),
                (int) Math.floor(Math.max(o.x() + reach, e.x() + grow)),
                (int) Math.floor(Math.max(o.y() + reach, e.y() + grow)),
                (int) Math.floor(Math.max(o.z() + reach, e.z() + grow)));
    }

    /**
     * Squared distance used by the change gate: the larger of the apex and end displacements between two cones.
     */
    public static double displacementSq(Shape a, Shape b) {
        return Math.max(a.origin().distSq(b.origin()), a.end().distSq(b.end()));
    }
}
