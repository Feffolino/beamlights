package it.ratlab.beamlights.core.math;

/** Immutable double vector; keeps the core free of Minecraft classes. */
public record V3(double x, double y, double z) {
    public static final V3 ZERO = new V3(0, 0, 0);

    public V3 add(V3 o) {
        return new V3(x + o.x, y + o.y, z + o.z);
    }

    public V3 sub(V3 o) {
        return new V3(x - o.x, y - o.y, z - o.z);
    }

    public V3 scale(double s) {
        return new V3(x * s, y * s, z * s);
    }

    public double dot(V3 o) {
        return x * o.x + y * o.y + z * o.z;
    }

    public double lengthSq() {
        return x * x + y * y + z * z;
    }

    public double length() {
        return Math.sqrt(lengthSq());
    }

    public double distSq(V3 o) {
        double dx = x - o.x, dy = y - o.y, dz = z - o.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public V3 normalize() {
        double l = length();
        return l < 1e-9 ? ZERO : scale(1.0 / l);
    }
}
