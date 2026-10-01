package it.ratlab.beamlights.core;

import it.ratlab.beamlights.api.math.V3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SourceMotionPerfTest {
    @Test
    void snapsToBlockCenter() {
        assertEquals(new V3(1.5, 64.5, -0.5), SourceMotion.snapToBlock(new V3(1.01, 64.99, -0.2)));
        assertEquals(new V3(-1.5, 0.5, 2.5), SourceMotion.snapToBlock(new V3(-1.0001, 0, 2.999)));
    }

    @Test
    void subBlockMotionIsNoMoveWhenSnapped() {
        V3 a = SourceMotion.snapToBlock(new V3(3.1, 70.2, 5.3));
        V3 b = SourceMotion.snapToBlock(new V3(3.9, 70.8, 5.7));
        assertFalse(SourceMotion.shouldMove(a, b, 12, 12, 0.0001));
        V3 c = SourceMotion.snapToBlock(new V3(4.05, 70.8, 5.7));
        assertTrue(SourceMotion.shouldMove(a, c, 12, 12, 0.25));
    }

    @Test
    void precomputedDistanceVariant() {
        assertFalse(SourceMotion.shouldMove(0.01, 10, 10, 0.25));
        assertTrue(SourceMotion.shouldMove(0.0625, 10, 10, 0.25));
        assertTrue(SourceMotion.shouldMove(0, 10, 11, 0.25));
    }

    @Test
    void remoteDueStaggersByEntityId() {
        assertTrue(SourceMotion.remoteDue(5, 123, 1));
        int due = 0;
        for (long t = 0; t < 8; t++) if (SourceMotion.remoteDue(7, t, 4)) due++;
        assertEquals(2, due);
        // Two consecutive ids are never due on the same tick with interval 2.
        for (long t = 0; t < 4; t++) {
            assertNotEquals(SourceMotion.remoteDue(10, t, 2), SourceMotion.remoteDue(11, t, 2));
        }
        assertTrue(SourceMotion.remoteDue(-3, 3, 2) || SourceMotion.remoteDue(-3, 4, 2));
    }
}
