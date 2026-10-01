package it.ratlab.beamlights.core;

import it.ratlab.beamlights.api.math.V3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BeamConeTest {
    private static final V3 O = new V3(0.5, 0.5, 0.5);
    private static final V3 X = new V3(1, 0, 0);

    @Test
    void insideCone() {
        assertTrue(BeamCone.inCone(O, X, 20, 32, new V3(10.5, 0.5, 0.5)));
        // ~11.3 degrees off axis
        assertTrue(BeamCone.inCone(O, X, 20, 32, new V3(10.5, 2.5, 0.5)));
    }

    @Test
    void outsideAngle() {
        // 45 degrees off axis
        assertFalse(BeamCone.inCone(O, X, 20, 32, new V3(10.5, 10.5, 0.5)));
    }

    @Test
    void beyondRange() {
        assertFalse(BeamCone.inCone(O, X, 20, 32, new V3(40.5, 0.5, 0.5)));
        assertTrue(BeamCone.inCone(O, X, 20, 32, new V3(32.5, 0.5, 0.5)));
    }

    @Test
    void behind() {
        assertFalse(BeamCone.inCone(O, X, 20, 32, new V3(-5.5, 0.5, 0.5)));
    }

    @Test
    void originItself() {
        assertTrue(BeamCone.inCone(O, X, 20, 32, O));
    }

    @Test
    void unnormalizedDirection() {
        assertTrue(BeamCone.inCone(O, new V3(5, 0, 0), 20, 32, new V3(10.5, 2.5, 0.5)));
    }

    @Test
    void visibleInOpenAir() {
        assertTrue(BeamCone.visible(O, new V3(10.5, 0.5, 0.5), (x, y, z) -> false));
    }

    @Test
    void wallBetweenBlocks() {
        assertFalse(BeamCone.visible(O, new V3(10.5, 0.5, 0.5), (x, y, z) -> x == 5));
    }

    @Test
    void wallBehindTargetDoesNotBlock() {
        assertTrue(BeamCone.visible(O, new V3(10.5, 0.5, 0.5), (x, y, z) -> x == 12));
    }

    @Test
    void targetBlockItselfDoesNotBlock() {
        assertTrue(BeamCone.visible(O, new V3(10.5, 0.5, 0.5), (x, y, z) -> x == 10));
    }

    @Test
    void samePointVisible() {
        assertTrue(BeamCone.visible(O, O, (x, y, z) -> true));
    }
}
