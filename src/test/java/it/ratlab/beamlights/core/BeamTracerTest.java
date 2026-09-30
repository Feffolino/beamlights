package it.ratlab.beamlights.core;

import it.ratlab.beamlights.core.math.V3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BeamTracerTest {
    private static final V3 O = new V3(0.5, 0.5, 0.5);

    @Test
    void hitsWallAtEntryFace() {
        BeamTracer.Result r = BeamTracer.trace(O, new V3(1, 0, 0), 10, (x, y, z) -> x == 5);
        assertTrue(r.hit());
        assertEquals(4.5, r.distance(), 1e-9);
        assertEquals(5.0, r.point().x(), 1e-9);
        assertEquals(5, r.blockX());
    }

    @Test
    void startVoxelNeverTested() {
        List<String> visited = new ArrayList<>();
        BeamTracer.trace(O, new V3(1, 0, 0), 3, (x, y, z) -> {
            visited.add(x + "," + y + "," + z);
            return false;
        });
        assertFalse(visited.contains("0,0,0"));
        assertEquals(List.of("1,0,0", "2,0,0", "3,0,0"), visited);
    }

    @Test
    void missReturnsEndOfRange() {
        BeamTracer.Result r = BeamTracer.trace(O, new V3(1, 0, 0), 8, (x, y, z) -> false);
        assertFalse(r.hit());
        assertEquals(8.0, r.distance(), 1e-9);
        assertEquals(8.5, r.point().x(), 1e-9);
    }

    @Test
    void wallBeyondRangeIsMiss() {
        BeamTracer.Result r = BeamTracer.trace(O, new V3(1, 0, 0), 10, (x, y, z) -> x == 20);
        assertFalse(r.hit());
    }

    @Test
    void negativeDirection() {
        BeamTracer.Result r = BeamTracer.trace(O, new V3(-1, 0, 0), 10, (x, y, z) -> x == -3);
        assertTrue(r.hit());
        assertEquals(2.5, r.distance(), 1e-9);
        assertEquals(-2.0, r.point().x(), 1e-9);
    }

    @Test
    void diagonalHitsFloorFaceAtBoundary() {
        BeamTracer.Result r = BeamTracer.trace(O, new V3(1, 0.5, 0), 20, (x, y, z) -> y >= 2);
        assertTrue(r.hit());
        assertEquals(2, r.blockY());
        assertEquals(2.0, r.point().y(), 1e-9);
    }

    @Test
    void unnormalizedDirectionGivesSameDistance() {
        BeamTracer.Result r = BeamTracer.trace(O, new V3(10, 0, 0), 10, (x, y, z) -> x == 5);
        assertEquals(4.5, r.distance(), 1e-9);
    }

    @Test
    void zeroDirectionIsMissAtOrigin() {
        BeamTracer.Result r = BeamTracer.trace(O, V3.ZERO, 10, (x, y, z) -> true);
        assertFalse(r.hit());
        assertEquals(0.0, r.distance(), 1e-9);
        assertEquals(O, r.point());
    }
}
