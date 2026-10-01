package it.ratlab.beamlights.core;

import it.ratlab.beamlights.api.math.V3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AttractTargetTest {
    @Test
    void repathWhenNoPreviousTarget() {
        assertTrue(AttractTarget.shouldRepath(null, new V3(1, 2, 3), 2.0));
    }

    @Test
    void repathOnlyAfterMovingFarEnough() {
        V3 last = new V3(0, 64, 0);
        assertFalse(AttractTarget.shouldRepath(last, new V3(1.9, 64, 0), 2.0));
        assertTrue(AttractTarget.shouldRepath(last, new V3(2.0, 64, 0), 2.0));
        assertTrue(AttractTarget.shouldRepath(last, new V3(0, 64, -5), 2.0));
    }

    @Test
    void litPointBacksOffAlongBeam() {
        V3 p = AttractTarget.litPoint(new V3(10, 64, 0), new V3(4, 0, 0));
        assertEquals(9.5, p.x(), 1e-9);
        assertEquals(64, p.y(), 1e-9);
        assertEquals(0, p.z(), 1e-9);
        assertEquals(new V3(1, 2, 3), AttractTarget.litPoint(new V3(1, 2, 3), V3.ZERO));
    }

    @Test
    void arrivedWithinRadius() {
        assertTrue(AttractTarget.arrived(new V3(1, 0, 1), new V3(0, 0, 0), 1.5));
        assertFalse(AttractTarget.arrived(new V3(2, 0, 0), new V3(0, 0, 0), 1.5));
    }

    @Test
    void stepReturnsPointWhenClose() {
        V3 p = new V3(3, 64, 4);
        assertEquals(p, AttractTarget.step(new V3(0, 64, 0), p, 5.0));
        assertEquals(p, AttractTarget.step(p, p, 5.0));
    }

    @Test
    void stepClampsToMaxDistance() {
        V3 s = AttractTarget.step(new V3(0, 64, 0), new V3(30, 64, 40), 5.0);
        assertEquals(3.0, s.x(), 1e-9);
        assertEquals(64.0, s.y(), 1e-9);
        assertEquals(4.0, s.z(), 1e-9);
        assertEquals(25.0, s.distSq(new V3(0, 64, 0)), 1e-9);
    }

    @Test
    void rollHonoursChance() {
        assertTrue(AttractTarget.roll(0.35, 0.1));
        assertFalse(AttractTarget.roll(0.35, 0.35));
        assertFalse(AttractTarget.roll(0.0, 0.0));
        assertTrue(AttractTarget.roll(1.0, 0.999));
    }
}
