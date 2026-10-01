package it.ratlab.beamlights.core;

import it.ratlab.beamlights.api.math.V3;

import java.util.ArrayList;
import java.util.List;

/** Ray directions inside a beam cone: the axis plus side rays described by {@link RaySpec}. */
public final class ConeRays {
    private static final double[] SIDE_ROLL_DEG = {270, 30, 150};

    private ConeRays() {
    }

    /** Legacy form: rays == 4 gives [central, down, up-right, up-left]; any other value gives the central ray only. */
    public static List<V3> directions(V3 dir, double halfAngleDeg, double spread, int rays) {
        V3 axis = dir.normalize();
        if (rays != 4) return List.of(axis);
        List<RaySpec> specs = new ArrayList<>();
        for (double roll : SIDE_ROLL_DEG) specs.add(new RaySpec(spread, roll, 0, false, 1.0));
        List<V3> out = new ArrayList<>();
        out.add(axis);
        out.addAll(directions(axis, halfAngleDeg, specs));
        return List.copyOf(out);
    }

    /** Side ray directions in spec order (no central ray). */
    public static List<V3> directions(V3 dir, double halfAngleDeg, List<RaySpec> specs) {
        V3 axis = dir.normalize();
        V3 cross = cross(axis, new V3(0, 1, 0));
        V3 right = cross.length() < 1e-6 ? new V3(1, 0, 0) : cross.normalize();
        V3 up = cross(right, axis);

        V3[] out = new V3[specs.size()];
        for (int i = 0; i < out.length; i++) {
            RaySpec s = specs.get(i);
            double a = Math.toRadians(halfAngleDeg * s.spread());
            double phi = Math.toRadians(s.rollDeg());
            V3 radial = right.scale(Math.cos(phi)).add(up.scale(Math.sin(phi)));
            out[i] = axis.scale(Math.cos(a)).add(radial.scale(Math.sin(a))).normalize();
        }
        return List.of(out);
    }

    private static V3 cross(V3 a, V3 b) {
        return new V3(a.y() * b.z() - a.z() * b.y(), a.z() * b.x() - a.x() * b.z(), a.x() * b.y() - a.y() * b.x());
    }
}
