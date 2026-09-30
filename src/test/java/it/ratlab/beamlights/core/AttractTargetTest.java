package it.ratlab.beamlights.core;

import it.ratlab.beamlights.core.math.V3;
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
}
