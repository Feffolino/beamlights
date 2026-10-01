package it.ratlab.beamlights.core;

import it.ratlab.beamlights.api.math.V3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SourceMotionTest {
    @Test
    void newSourceAlwaysMoves() {
        assertTrue(SourceMotion.shouldMove(null, new V3(0, 0, 0), 0, 10, 0.25));
    }

    @Test
    void smallMoveIgnored() {
        assertFalse(SourceMotion.shouldMove(new V3(0, 0, 0), new V3(0.2, 0, 0), 10, 10, 0.25));
    }

    @Test
    void bigMoveApplied() {
        assertTrue(SourceMotion.shouldMove(new V3(0, 0, 0), new V3(0.25, 0, 0), 10, 10, 0.25));
    }

    @Test
    void luminanceChangeApplied() {
        assertTrue(SourceMotion.shouldMove(new V3(0, 0, 0), new V3(0, 0, 0), 10, 9, 0.25));
    }
}
