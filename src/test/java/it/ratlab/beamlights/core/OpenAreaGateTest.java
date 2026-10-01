package it.ratlab.beamlights.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAreaGateTest {
    private static final OpenAreaGate.Settings S = new OpenAreaGate.Settings(20, 2, 10, true, 15);

    @Test
    void firstUpdateDecidesAtOnce() {
        assertTrue(new OpenAreaGate().update(false, 0, 0, 1, S));
        assertTrue(new OpenAreaGate().update(true, 21, 0, 1, S));
        assertFalse(new OpenAreaGate().update(true, 19, 0, 1, S));
        assertTrue(new OpenAreaGate().update(true, 5, 15, 1, S));
    }

    @Test
    void hysteresisBandHoldsState() {
        OpenAreaGate g = new OpenAreaGate();
        g.update(true, 19, 0, 1, S);
        for (int i = 0; i < 20; i++) assertFalse(g.update(true, 21.5, 0, 1, S)); // inside 18..22
        assertTrue(g.update(true, 22.5, 0, 1, S));
        for (int i = 0; i < 20; i++) assertTrue(g.update(true, 18.5, 0, 1, S));
        assertFalse(g.update(true, 17.5, 0, 1, S));
    }

    @Test
    void minTicksHoldsAfterSwitch() {
        OpenAreaGate g = new OpenAreaGate();
        g.update(true, 10, 0, 1, S);
        assertTrue(g.update(false, 0, 0, 10, S));
        for (int i = 0; i < 9; i++) assertTrue(g.update(true, 5, 0, 1, S)); // held for 10 ticks
        assertFalse(g.update(true, 5, 0, 1, S));
    }

    @Test
    void skyLightOpensNearHit() {
        OpenAreaGate g = new OpenAreaGate();
        g.update(true, 5, 0, 1, S);
        assertTrue(g.update(true, 5, 15, 10, S));
        OpenAreaGate.Settings noSky = new OpenAreaGate.Settings(20, 2, 10, false, 15);
        assertFalse(new OpenAreaGate().update(true, 5, 15, 1, noSky));
    }

    @Test
    void fadeOffsetTwoSteps() {
        assertEquals(-4, OpenAreaGate.fadeOffset(0, 6, 4));
        assertEquals(-4, OpenAreaGate.fadeOffset(5, 6, 4));
        assertEquals(0, OpenAreaGate.fadeOffset(6, 6, 4));
        assertEquals(0, OpenAreaGate.fadeOffset(0, 0, 4));
    }
}
