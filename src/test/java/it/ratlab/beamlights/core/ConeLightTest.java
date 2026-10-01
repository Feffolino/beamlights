package it.ratlab.beamlights.core;

import it.ratlab.beamlights.api.math.V3;
import org.junit.jupiter.api.Test;

import java.util.Random;

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
    void softEdgeFadesToHalfAtTheRimAndEndFactorScales() {
        ConeLight.Look look = new ConeLight.Look(0.25, 0.5);
        double r = 10 * Math.tan(Math.toRadians(15));
        double axis = 14 * (1 - 0.75 * 10 / 20.0);
        assertEquals(axis, ConeLight.lightAt(CONE, look, 10, r * 0.4, 0, K), 1e-9);
        assertEquals(axis * 0.75, ConeLight.lightAt(CONE, look, 10, r * 0.75, 0, K), 1e-9);
        assertEquals(axis * 0.5, ConeLight.lightAt(CONE, look, 10, r, 0, K), 1e-9);
        assertEquals(14 * 0.25, ConeLight.lightAt(CONE, look, 20, 0, 0, K), 1e-9);
    }

    @Test
    void boundsContainEveryLitBlock() {
        ConeLight.Shape s = new ConeLight.Shape(new V3(3.2, 64.7, -5.1), new V3(1, -0.4, 0.7), 18, 20, 15);
        for (ConeLight.Look look : new ConeLight.Look[]{ConeLight.Look.HARD, ConeLight.Look.DEFAULT,
                new ConeLight.Look(1.0, 0.0), new ConeLight.Look(0.1, 1.0)}) {
            assertAllLitInside(s, look, ConeLight.bounds(s, look, K));
        }
    }

    @Test
    void boundsAreTightForAxisAlignedAndPointCones() {
        ConeLight.Shape axis = new ConeLight.Shape(new V3(0.5, 0.5, 0.5), new V3(0, 0, 1), 10, 25, 12);
        ConeLight.Box b = ConeLight.bounds(axis, ConeLight.Look.DEFAULT, K);
        assertAllLitInside(axis, ConeLight.Look.DEFAULT, b);
        // Along z: end 10.5 + endLevel 6 / K (3.1) = 13.6 -> block 13; apex 0.5 - 12 / K (6.2) = -5.7 -> block -6.
        assertEquals(13, b.maxZ());
        assertEquals(-6, b.minZ());
        ConeLight.Shape point = new ConeLight.Shape(new V3(0.5, 0.5, 0.5), new V3(1, 0, 0), 0, 25, 15);
        ConeLight.Box p = ConeLight.bounds(point, ConeLight.Look.DEFAULT, K);
        assertEquals(new ConeLight.Box(-7, -7, -7, 7, 7, 7), p);
        assertAllLitInside(point, ConeLight.Look.DEFAULT, p);
    }

    /** Typical outdoor beam (eye height, slightly down, diagonal) before and after the 1.1 cone limits. */
    @Test
    void defaultConeCoversFarFewerCellsThanTheOldLooseBox() {
        V3 eye = new V3(0.3, 65.62, 0.7);
        V3 dir = new V3(1, -0.3, 0.6);
        // 1.0: open sky -> range 22, beam half-angle 42, luminance 15 - 3.
        ConeLight.Shape oldSky = new ConeLight.Shape(eye, dir, 22, 42, 12);
        // 1.0 with a hit at 16.5 blocks; 1.1 default for the same hit: half-angle capped at 25.
        ConeLight.Shape oldHit = new ConeLight.Shape(eye, dir, 16, 42, 12);
        ConeLight.Shape now = ConePolicy.target(eye, dir, true, 16.5, 0.5, 42, 12, ConePolicy.Settings.BALANCED);
        assertNotNull(now);
        assertEquals(25, now.halfAngleDeg(), 1e-9);
        // 1.1 open sky: no cone at all.
        assertNull(ConePolicy.target(eye, dir, false, 0, 0.5, 42, 12, ConePolicy.Settings.BALANCED));

        ConeLight.Box looseSky = ConeLight.looseBounds(oldSky, K);
        ConeLight.Box looseHit = ConeLight.looseBounds(oldHit, K);
        ConeLight.Box tightHit = ConeLight.bounds(oldHit, ConeLight.Look.DEFAULT, K);
        ConeLight.Box tight = ConeLight.bounds(now, ConeLight.Look.DEFAULT, K);
        assertAllLitInside(now, ConeLight.Look.DEFAULT, tight);
        System.out.println("CONE_BOX old sky: " + describe(looseSky) + " | old hit16: " + describe(looseHit)
                + " | tight box same 42deg cone: " + describe(tightHit) + " | new default hit16: " + describe(tight)
                + " | new sky: none");
        assertTrue(tight.volume() * 3 < looseHit.volume(), describe(tight) + " vs " + describe(looseHit));
        assertTrue(tight.cells(3) * 2 < looseHit.cells(3), describe(tight) + " vs " + describe(looseHit));
        assertTrue(tight.sections() < looseHit.sections());
        assertTrue(tight.cells(3) * 4 < looseSky.cells(3));
    }

    /** The prepared fast path returns the reference value at every lit point (and no light elsewhere). */
    @Test
    void preparedMatchesReferenceOnRandomPoints() {
        Random r = new Random(42);
        ConeLight.Look[] looks = {ConeLight.Look.HARD, ConeLight.Look.DEFAULT, new ConeLight.Look(1.0, 1.0),
                new ConeLight.Look(0.1, 0.6)};
        for (int n = 0; n < 60; n++) {
            V3 o = new V3(r.nextDouble() * 40 - 20, 60 + r.nextDouble() * 10, r.nextDouble() * 40 - 20);
            V3 a = new V3(r.nextGaussian(), r.nextGaussian(), r.nextGaussian());
            double len = n % 10 == 0 ? 0 : r.nextDouble() * 30;
            ConeLight.Shape s = new ConeLight.Shape(o, a, len, r.nextDouble() * 60, 4 + r.nextInt(12));
            ConeLight.Look look = looks[n % looks.length];
            ConeLight.Prepared p = new ConeLight.Prepared(s, look, K);
            int lit = 0;
            for (int i = 0; i < 4000; i++) {
                double range = i % 2 == 0 ? 40 : 12;
                int x = (int) Math.floor(o.x() + (r.nextDouble() * 2 - 1) * range);
                int y = (int) Math.floor(o.y() + (r.nextDouble() * 2 - 1) * range);
                int z = (int) Math.floor(o.z() + (r.nextDouble() * 2 - 1) * range);
                double ref = ConeLight.lightAt(s, look, x + 0.5, y + 0.5, z + 0.5, K);
                double fast = p.lightAtBlock(x, y, z, K);
                if (ref > 0) lit++;
                assertEquals(Math.max(0, ref), Math.max(0, fast), 1e-9, s + " " + look + " at " + x + "," + y + "," + z);
            }
            assertTrue(lit > 0, "no lit sample for " + s);
            // Another falloff ratio takes the reference path.
            assertEquals(ConeLight.lightAt(s, look, 0.5, 64.5, 0.5, 1.0), p.lightAtBlock(0, 64, 0, 1.0), 1e-12);
        }
    }

    @Test
    void displacementIsTheLargerEndMove() {
        ConeLight.Shape moved = new ConeLight.Shape(new V3(1, 0, 0), new V3(1, 0, 0), 23, 15, 14);
        assertEquals(16.0, ConeLight.displacementSq(CONE, moved), 1e-9);
    }

    private static void assertAllLitInside(ConeLight.Shape s, ConeLight.Look look, ConeLight.Box b) {
        for (int x = b.minX() - 6; x <= b.maxX() + 6; x++)
            for (int y = b.minY() - 6; y <= b.maxY() + 6; y++)
                for (int z = b.minZ() - 6; z <= b.maxZ() + 6; z++)
                    if (ConeLight.lightAt(s, look, x + 0.5, y + 0.5, z + 0.5, K) > 0)
                        assertTrue(b.contains(x, y, z), x + "," + y + "," + z);
    }

    private static String describe(ConeLight.Box b) {
        return (b.maxX() - b.minX() + 1) + "x" + (b.maxY() - b.minY() + 1) + "x" + (b.maxZ() - b.minZ() + 1)
                + " = " + b.volume() + " blocks, " + b.cells(3) + " cells, " + b.sections() + " sections";
    }
}
