package it.ratlab.beamlights.core;

import it.ratlab.beamlights.api.math.V3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConeLightTest {
    private static final double K = ConeLight.LDL_FALLOFF;
    private static final ConeLight.Shape CONE =
            new ConeLight.Shape(new V3(0, 0, 0), new V3(2, 0, 0), 20, 15, 14);

    @Test
    void axisIsNormalizedAndValuesClamped() {
        assertEquals(1.0, CONE.axis().length(), 1e-9);
        ConeLight.Shape s = new ConeLight.Shape(V3.ZERO, V3.ZERO, -3, 170, 40);
        assertEquals(0, s.length());
        assertEquals(ConeLight.MAX_HALF_ANGLE_DEG, s.halfAngleDeg());
        assertEquals(15, s.luminance());
        assertEquals(1.0, s.axis().length(), 1e-9);
    }

    @Test
    void onAxisFadesToHalfAtTheEnd() {
        assertEquals(14, ConeLight.lightAt(CONE, 0, 0, 0, K), 1e-9);
        assertEquals(10.5, ConeLight.lightAt(CONE, 10, 0, 0, K), 1e-9);
        assertEquals(7, ConeLight.lightAt(CONE, 20, 0, 0, K), 1e-9);
    }

    @Test
    void insideTheConeRadiusIsUnattenuated() {
        double r = 10 * Math.tan(Math.toRadians(15));
        assertEquals(10.5, ConeLight.lightAt(CONE, 10, r * 0.9, 0, K), 1e-9);
        assertEquals(10.5 - K * 1.0, ConeLight.lightAt(CONE, 10, r + 1.0, 0, K), 1e-9);
    }

    @Test
    void behindTheApexIsASphere() {
        assertEquals(14 - K * 3, ConeLight.lightAt(CONE, -3, 0, 0, K), 1e-9);
        assertEquals(14 - K * 5, ConeLight.lightAt(CONE, -3, 4, 0, K), 1e-9);
    }

    @Test
    void pastTheEndFallsOff() {
        assertEquals(7 - K * 2, ConeLight.lightAt(CONE, 22, 0, 0, K), 1e-9);
        assertTrue(ConeLight.lightAt(CONE, 30, 0, 0, K) < 0);
    }

    @Test
    void zeroLengthIsAPointLight() {
        ConeLight.Shape p = new ConeLight.Shape(new V3(1, 1, 1), new V3(0, 0, 1), 0, 10, 15);
        assertEquals(15 - K * 2, ConeLight.lightAt(p, 1, 3, 1, K), 1e-9);
    }

    @Test
    void boundsContainEveryLitBlock() {
        ConeLight.Shape s = new ConeLight.Shape(new V3(3.2, 64.7, -5.1), new V3(1, -0.4, 0.7), 18, 20, 15);
        ConeLight.Box b = ConeLight.bounds(s, K);
        for (int x = b.minX() - 6; x <= b.maxX() + 6; x++)
            for (int y = b.minY() - 6; y <= b.maxY() + 6; y++)
                for (int z = b.minZ() - 6; z <= b.maxZ() + 6; z++)
                    if (ConeLight.lightAtBlock(s, x, y, z, K) > 0) assertTrue(b.contains(x, y, z), x + "," + y + "," + z);
    }

    @Test
    void displacementIsTheLargerEndMove() {
        ConeLight.Shape moved = new ConeLight.Shape(new V3(1, 0, 0), new V3(1, 0, 0), 23, 15, 14);
        assertEquals(16.0, ConeLight.displacementSq(CONE, moved), 1e-9);
    }
}
