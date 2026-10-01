package it.ratlab.beamlights.core;

import it.ratlab.beamlights.api.math.V3;

/**
 * Cone-shaped light along a central beam (LambDynamicLights backend, config ldlConeLight). Same falloff convention as
 * point lights: light = level - falloffRatio * distance, with LDL's ratio 15 / 7.75 (light 15 reaches 7.75 blocks).
 * Inside the cone the level fades from the full luminance at the apex to endFactor x luminance at the end; the outer
 * edgeSoftness fraction of the cone radius fades to half that level at the rim; outside the cone, light falls off with
 * the distance from the cone surface (behind the apex: from the apex, past the end: from the end disc).
 * Pure math, no occlusion: the length should stop at the beam's first hit.
 * <p>
 * Two evaluators: {@link #lightAt(Shape, Look, double, double, double, double)} is the plain reference;
 * {@link Prepared} is the immutable snapshot LDL queries per block (precomputed axis, tan, bounds; AABB and axial
 * early rejects before any square root; no allocation). Both return the same value wherever it is positive.
 */
public final class ConeLight {
    /** LambDynamicLights' falloff ratio (blocks per light level): 15 / 7.75. */
    public static final double LDL_FALLOFF = 15.0 / 7.75;
    /** Keys slot of a cone light (planner slots stay far below; bit 7 = ghost stays clear). */
    public static final int SLOT = 0x7F;
    /** Half angles are clamped to this, so the cone radius stays bounded. */
    static final double MAX_HALF_ANGLE_DEG = 60.0;
    /** Level at the rim relative to the axis level, reached across the soft edge. */
    static final double RIM_FACTOR = 0.5;
    /** Value returned for rejected points (no light). */
    static final double NONE = -1.0;

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

    /** Falloff look: level at the end relative to the apex (0.1..1), soft edge as a fraction of the radius (0..1). */
    public record Look(double endFactor, double edgeSoftness) {
        /** The 1.0 look: half at the end, hard edge. */
        public static final Look HARD = new Look(0.5, 0.0);
        public static final Look DEFAULT = new Look(0.5, 0.25);

        public Look {
            endFactor = Math.max(0.1, Math.min(1.0, endFactor));
            edgeSoftness = Math.max(0.0, Math.min(1.0, edgeSoftness));
        }
    }

    /** Integer block bounds (inclusive) of every block the cone can light. */
    public record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public boolean contains(int x, int y, int z) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }

        public long volume() {
            return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        }

        /** Grid cells of size 2^shift touched by the box (LDL spatial lookup: shift 3 = 8 blocks). */
        public long cells(int shift) {
            return (long) ((maxX >> shift) - (minX >> shift) + 1) * ((maxY >> shift) - (minY >> shift) + 1)
                    * ((maxZ >> shift) - (minZ >> shift) + 1);
        }

        /** Chunk sections LDL rebuilds for this box (it widens the box by 8 blocks on every side). */
        public long sections() {
            return (long) (((maxX + 8) >> 4) - ((minX - 8) >> 4) + 1) * (((maxY + 8) >> 4) - ((minY - 8) >> 4) + 1)
                    * (((maxZ + 8) >> 4) - ((minZ - 8) >> 4) + 1);
        }
    }

    private ConeLight() {
    }

    /** Reference light level (may be negative = none) at point (px, py, pz) with the 1.0 look (half, hard edge). */
    public static double lightAt(Shape s, double px, double py, double pz, double falloffRatio) {
        return lightAt(s, Look.HARD, px, py, pz, falloffRatio);
    }

    /** Reference light level (may be negative = none) at point (px, py, pz). */
    public static double lightAt(Shape s, Look look, double px, double py, double pz, double falloffRatio) {
        double vx = px - s.origin().x(), vy = py - s.origin().y(), vz = pz - s.origin().z();
        double t = vx * s.axis().x() + vy * s.axis().y() + vz * s.axis().z();
        double vSq = vx * vx + vy * vy + vz * vz;
        int lum = s.luminance();
        double len = s.length();
        if (t <= 0 || len <= 0) return lum - falloffRatio * Math.sqrt(vSq);
        double tc = Math.min(t, len);
        double level = lum * (1.0 - (1.0 - look.endFactor()) * tc / len);
        double perp = Math.sqrt(Math.max(0, vSq - t * t));
        double radius = tc * Math.tan(Math.toRadians(s.halfAngleDeg()));
        level *= edgeFactor(perp, radius, look.edgeSoftness());
        double outside = Math.max(0, perp - radius);
        double beyond = t - tc;
        double dist = Math.sqrt(outside * outside + beyond * beyond);
        return level - falloffRatio * dist;
    }

    /** Soft edge: 1 inside (1 - soft) x radius, then linear down to RIM_FACTOR at the rim and beyond. */
    static double edgeFactor(double perp, double radius, double soft) {
        if (soft <= 0) return 1.0;
        double inner = radius * (1.0 - soft);
        if (perp <= inner) return 1.0;
        double band = radius - inner;
        double f = band > 1e-12 ? Math.min(1.0, (perp - inner) / band) : 1.0;
        return 1.0 - (1.0 - RIM_FACTOR) * f;
    }

    /** Reference light at the center of block (x, y, z), 1.0 look. */
    public static double lightAtBlock(Shape s, int x, int y, int z, double falloffRatio) {
        return lightAt(s, x + 0.5, y + 0.5, z + 0.5, falloffRatio);
    }

    /**
     * Tight block bounds of every block the cone can light: the lit set lies in the convex hull of the apex ball
     * (radius lum / falloff) and the end disc (radius R, normal = axis) grown by endLevel / falloff. A disc's AABB
     * half-extent along world axis i is R * sqrt(1 - axis_i^2); a block is lit when its center is inside.
     */
    public static Box bounds(Shape s, Look look, double falloffRatio) {
        double reach = s.luminance() / falloffRatio;
        double endReach = s.luminance() * look.endFactor() / falloffRatio;
        double r = s.length() > 0 ? s.endRadius() : 0;
        V3 o = s.origin();
        V3 e = s.end();
        V3 a = s.axis();
        double gx = r * Math.sqrt(Math.max(0, 1 - a.x() * a.x())) + endReach;
        double gy = r * Math.sqrt(Math.max(0, 1 - a.y() * a.y())) + endReach;
        double gz = r * Math.sqrt(Math.max(0, 1 - a.z() * a.z())) + endReach;
        return new Box(
                lo(Math.min(o.x() - reach, e.x() - gx)), lo(Math.min(o.y() - reach, e.y() - gy)),
                lo(Math.min(o.z() - reach, e.z() - gz)), hi(Math.max(o.x() + reach, e.x() + gx)),
                hi(Math.max(o.y() + reach, e.y() + gy)), hi(Math.max(o.z() + reach, e.z() + gz)));
    }

    // Block x is inside when its center x + 0.5 is inside [min, max].
    private static int lo(double min) {
        return (int) Math.ceil(min - 0.5);
    }

    private static int hi(double max) {
        return (int) Math.floor(max - 0.5);
    }

    /** The 1.0 loose cube (end grown by the full radius plus the full reach); kept for comparisons in tests. */
    static Box looseBounds(Shape s, double falloffRatio) {
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

    /**
     * Immutable evaluation snapshot of one cone for a given look and falloff ratio: every per-cone constant is
     * precomputed, so {@link #lightAtBlock} does a box test, an axial test and a lateral test (all without sqrt)
     * before the exact formula. Safe to share between threads.
     */
    public static final class Prepared {
        private final Shape shape;
        private final Look look;
        private final Box box;
        private final double k;
        private final double ox, oy, oz, ax, ay, az;
        private final double len, tan, lum, levelSlope, soft, reach, reachSq, endReach;

        public Prepared(Shape shape, Look look, double falloffRatio) {
            this.shape = shape;
            this.look = look;
            this.k = falloffRatio;
            this.box = bounds(shape, look, falloffRatio);
            ox = shape.origin().x();
            oy = shape.origin().y();
            oz = shape.origin().z();
            ax = shape.axis().x();
            ay = shape.axis().y();
            az = shape.axis().z();
            len = shape.length();
            tan = Math.tan(Math.toRadians(shape.halfAngleDeg()));
            lum = shape.luminance();
            levelSlope = len > 0 ? lum * (1.0 - look.endFactor()) / len : 0;
            soft = look.edgeSoftness();
            reach = lum / falloffRatio;
            reachSq = reach * reach;
            endReach = lum * look.endFactor() / falloffRatio;
        }

        public Shape shape() {
            return shape;
        }

        public Look look() {
            return look;
        }

        public Box box() {
            return box;
        }

        /** Light at the center of block (x, y, z); NONE (negative) for rejected blocks. */
        public double lightAtBlock(int x, int y, int z, double falloffRatio) {
            if (falloffRatio != k) return lightAt(shape, look, x + 0.5, y + 0.5, z + 0.5, falloffRatio);
            if (!box.contains(x, y, z)) return NONE;
            double vx = x + 0.5 - ox, vy = y + 0.5 - oy, vz = z + 0.5 - oz;
            double t = vx * ax + vy * ay + vz * az;
            if (t <= -reach) return NONE;
            double vSq = vx * vx + vy * vy + vz * vz;
            if (t <= 0 || len <= 0) {
                if (vSq >= reachSq) return NONE;
                return lum - k * Math.sqrt(vSq);
            }
            double beyond = t - len;
            if (beyond >= endReach) return NONE;
            double tc = beyond > 0 ? len : t;
            double level = lum - levelSlope * tc;
            double radius = tc * tan;
            double perpSq = Math.max(0, vSq - t * t);
            double limit = radius + level / k;
            if (perpSq >= limit * limit) return NONE;
            if (beyond <= 0) {
                double inner = radius * (1.0 - soft);
                if (perpSq <= inner * inner) return level;
            }
            double perp = Math.sqrt(perpSq);
            level *= edgeFactor(perp, radius, soft);
            double outside = perp > radius ? perp - radius : 0;
            if (beyond <= 0) return level - k * outside;
            return level - k * Math.sqrt(outside * outside + beyond * beyond);
        }
    }
}
