package it.ratlab.beamlights.core;

import it.ratlab.beamlights.core.math.V3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConeRaysTest {
    private static final V3 H = new V3(1, 0, 0);

    @Test
    void singleRayUnchanged() {
        List<V3> rays = ConeRays.directions(new V3(2, 0, 0), 30, 0.6, 1);
        assertEquals(1, rays.size());
        assertEquals(new V3(1, 0, 0), rays.get(0));
    }

    @Test
    void otherCountsActAsOne() {
        assertEquals(1, ConeRays.directions(H, 30, 0.6, 3).size());
        assertEquals(1, ConeRays.directions(H, 30, 0.6, 0).size());
    }

    @Test
    void fourRaysAllUnitLength() {
        List<V3> rays = ConeRays.directions(new V3(1, 0.3, -2), 30, 0.6, 4);
        assertEquals(4, rays.size());
        for (V3 r : rays) assertEquals(1.0, r.length(), 1e-9);
    }

    @Test
    void sideRaysTiltedByHalfAngleTimesSpread() {
        V3 dir = new V3(1, 0.3, -2).normalize();
        List<V3> rays = ConeRays.directions(dir, 30, 0.6, 4);
        double expected = Math.toRadians(30 * 0.6);
        assertEquals(dir, rays.get(0));
        for (int i = 1; i < 4; i++) {
            assertEquals(expected, Math.acos(rays.get(i).dot(dir)), 1e-6);
        }
    }

    @Test
    void secondRayPointsDownForHorizontalBeam() {
        List<V3> rays = ConeRays.directions(H, 30, 0.6, 4);
        assertTrue(rays.get(1).y() < 0);
        assertTrue(rays.get(2).y() > 0);
        assertTrue(rays.get(3).y() > 0);
    }

    @Test
    void specsGiveSideRaysInOrder() {
        List<RaySpec> specs = List.of(new RaySpec(0.5, 90, 0, false, 1), new RaySpec(1.0, 0, 0, false, 1),
                new RaySpec(0, 180, 0, false, 1));
        List<V3> rays = ConeRays.directions(H, 40, specs);
        assertEquals(3, rays.size());
        assertTrue(rays.get(0).y() > 0);
        assertEquals(Math.toRadians(20), Math.acos(rays.get(0).dot(H)), 1e-6);
        assertEquals(Math.toRadians(40), Math.acos(rays.get(1).dot(H)), 1e-6);
        assertEquals(0, rays.get(1).y(), 1e-9);
        assertEquals(0, rays.get(2).sub(H).length(), 1e-9);
    }

    @Test
    void specsMatchLegacyTriangle() {
        List<RaySpec> specs = RayLayout.build(RayLayout.Pattern.TRIANGLE, 6, 0.6, 270, 3, 0.3, List.of(), 0, false, 1);
        V3 dir = new V3(1, 0.3, -2);
        List<V3> legacy = ConeRays.directions(dir, 30, 0.6, 4);
        List<V3> side = ConeRays.directions(dir, 30, specs);
        for (int i = 0; i < 3; i++) assertEquals(0, side.get(i).sub(legacy.get(i + 1)).length(), 1e-9);
    }

    @Test
    void emptySpecsGiveNoRays() {
        assertTrue(ConeRays.directions(H, 30, List.<RaySpec>of()).isEmpty());
    }

    @Test
    void verticalBeamHasNoNaN() {
        List<V3> rays = ConeRays.directions(new V3(0, -1, 0), 30, 0.6, 4);
        assertEquals(4, rays.size());
        for (V3 r : rays) {
            assertFalse(Double.isNaN(r.x()) || Double.isNaN(r.y()) || Double.isNaN(r.z()));
            assertEquals(1.0, r.length(), 1e-9);
        }
    }
}
