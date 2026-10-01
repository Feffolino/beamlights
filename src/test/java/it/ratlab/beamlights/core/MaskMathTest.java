package it.ratlab.beamlights.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaskMathTest {
    private static final double EPS = 1e-9;

    @Test
    void policyFirstFailureWins() {
        assertEquals(MaskPolicy.Reason.DISABLED, MaskPolicy.decide(false, true, true, false, false));
        assertEquals(MaskPolicy.Reason.FAILED, MaskPolicy.decide(true, true, true, false, false));
        assertEquals(MaskPolicy.Reason.SHADER_PACK, MaskPolicy.decide(true, false, true, false, false));
        assertEquals(MaskPolicy.Reason.NOT_FIRST_PERSON, MaskPolicy.decide(true, false, false, false, false));
        assertEquals(MaskPolicy.Reason.NO_BEAM, MaskPolicy.decide(true, false, false, true, false));
        assertEquals(MaskPolicy.Reason.ACTIVE, MaskPolicy.decide(true, false, false, true, true));
    }

    @Test
    void maskFullOnAxisZeroOutside() {
        double outer = MaskMath.cosOuter(20, 1);
        double inner = MaskMath.cosInner(20, 1, 0.35);
        assertEquals(1, MaskMath.mask(1, 0, outer, inner, 16, 0.7), EPS);
        assertEquals(0, MaskMath.mask(Math.cos(Math.toRadians(25)), 2, outer, inner, 16, 0.7), EPS);
        assertEquals(0, MaskMath.mask(1, 16, outer, inner, 16, 0.7), EPS);
        double edge = MaskMath.mask(Math.cos(Math.toRadians(17)), 0, outer, inner, 16, 0.7);
        assertTrue(edge > 0 && edge < 1);
    }

    @Test
    void maskFadesWithDistance() {
        double outer = MaskMath.cosOuter(20, 1);
        double inner = MaskMath.cosInner(20, 1, 0.35);
        double near = MaskMath.mask(1, 2, outer, inner, 16, 0.7);
        double far = MaskMath.mask(1, 12, outer, inner, 16, 0.7);
        assertTrue(near > far && far > 0);
        assertEquals(1, MaskMath.mask(1, 12, outer, inner, 16, 0), EPS);
    }

    @Test
    void lightMultipliesDarkKeepsContrastAndBright() {
        assertEquals(0.05, MaskMath.light(0.05, 0.05, 0, 5, 0.65, 0.7, 0.015), EPS);
        double a = MaskMath.light(0.04, 0.04, 1, 5, 0.65, 0.7, 0);
        double b = MaskMath.light(0.08, 0.08, 1, 5, 0.65, 0.7, 0);
        assertEquals(0.24, a, 1e-6);
        assertEquals(2 * a, b, 1e-6); // contrast kept below the knee
        assertEquals(0.9, MaskMath.light(0.9, 0.9, 1, 5, 0.65, 0.7, 0), EPS); // bright pixel untouched
        assertTrue(MaskMath.light(0.5, 0.1, 1, 5, 0.65, 0.7, 0) < 1); // soft clip, never white
    }

    @Test
    void softClipContinuous() {
        assertEquals(0.7, MaskMath.softClip(0.7, 0.7), EPS);
        assertTrue(MaskMath.softClip(5, 0.7) < 1);
        assertTrue(MaskMath.softClip(0.71, 0.7) > 0.7);
    }

    @Test
    void approachSteps() {
        assertEquals(0.5, MaskMath.approach(0, 1, 0.05, 0.1), EPS);
        assertEquals(1, MaskMath.approach(0.9, 1, 0.05, 0.1), EPS);
        assertEquals(1, MaskMath.approach(0, 1, 0.05, 0), EPS);
        assertEquals(0.5, MaskMath.approach(1, 0, 0.05, 0.1), EPS);
    }
}
