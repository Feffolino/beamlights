package it.ratlab.beamlights.core;

import it.ratlab.beamlights.core.math.V3;

import java.util.List;

/** Ray directions inside a beam cone: the axis, plus 3 side rays (down, up-right, up-left). */
public final class ConeRays {
    private static final double[] SIDE_ROLL_DEG = {270, 30, 150};

    private ConeRays() {
    }

    /** rays == 4 gives [central, down, up-right, up-left]; any other value gives the central ray only. */
    public static List<V3> directions(V3 dir, double halfAngleDeg, double spread, int rays) {
        V3 axis = dir.normalize();
        if (rays != 4) return List.of(axis);

        V3 cross = cross(axis, new V3(0, 1, 0));
        V3 right = cross.length() < 1e-6 ? new V3(1, 0, 0) : cross.normalize();
        V3 up = cross(right, axis);

        double a = Math.toRadians(halfAngleDeg * spread);
        double ca = Math.cos(a), sa = Math.sin(a);
        V3[] out = new V3[4];
        out[0] = axis;
        for (int i = 0; i < 3; i++) {
            double phi = Math.toRadians(SIDE_ROLL_DEG[i]);
            V3 radial = right.scale(Math.cos(phi)).add(up.scale(Math.sin(phi)));
            out[i + 1] = axis.scale(ca).add(radial.scale(sa)).normalize();
        }
        return List.of(out);
    }

    private static V3 cross(V3 a, V3 b) {
        return new V3(a.y() * b.z() - a.z() * b.y(), a.z() * b.x() - a.x() * b.z(), a.x() * b.y() - a.y() * b.x());
    }
}
