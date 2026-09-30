package it.ratlab.beamlights.core;

import it.ratlab.beamlights.core.RayLayout.Pattern;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RayLayoutTest {
    private static List<RaySpec> build(Pattern p, int side, double roll) {
        return RayLayout.build(p, side, 0.6, roll, 3, 0.3, List.of(), -2, false, 1.0);
    }

    private static double[] rolls(List<RaySpec> specs) {
        return specs.stream().mapToDouble(RaySpec::rollDeg).toArray();
    }

    @Test
    void centerOnlyHasNoSideRays() {
        assertTrue(build(Pattern.CENTER_ONLY, 6, 270).isEmpty());
    }

    @Test
    void triangleDefaultMatchesOldLayout() {
        List<RaySpec> s = build(Pattern.TRIANGLE, 6, 270);
        assertArrayEquals(new double[]{270, 30, 150}, rolls(s), 1e-9);
        for (RaySpec r : s) {
            assertEquals(0.6, r.spread(), 1e-9);
            assertEquals(-2, r.luminanceOffset());
            assertFalse(r.midpoints());
            assertEquals(1.0, r.rangeFactor(), 1e-9);
        }
    }

    @Test
    void crossHasFourRaysNinetyApart() {
        assertArrayEquals(new double[]{45, 135, 225, 315}, rolls(build(Pattern.CROSS, 6, 45)), 1e-9);
    }

    @Test
    void ringUsesSideRaysEvenly() {
        assertArrayEquals(new double[]{270, 330, 30, 90, 150, 210}, rolls(build(Pattern.RING, 6, 270)), 1e-9);
    }

    @Test
    void doubleRingInnerOffsetByHalfStep() {
        List<RaySpec> s = build(Pattern.DOUBLE_RING, 4, 0);
        assertEquals(7, s.size());
        assertArrayEquals(new double[]{60, 180, 300, 0, 90, 180, 270}, rolls(s), 1e-9);
        for (int i = 0; i < 3; i++) assertEquals(0.3, s.get(i).spread(), 1e-9);
        for (int i = 3; i < 7; i++) assertEquals(0.6, s.get(i).spread(), 1e-9);
    }

    @Test
    void fanHorizontalAlternatesSidesWithGrowingSpread() {
        List<RaySpec> s = build(Pattern.FAN_HORIZONTAL, 4, 270);
        assertArrayEquals(new double[]{0, 180, 0, 180}, rolls(s), 1e-9);
        assertArrayEquals(new double[]{0.15, 0.3, 0.45, 0.6},
                s.stream().mapToDouble(RaySpec::spread).toArray(), 1e-9);
    }

    @Test
    void fanVerticalUsesUpAndDown() {
        assertArrayEquals(new double[]{90, 270, 90}, rolls(build(Pattern.FAN_VERTICAL, 3, 0)), 1e-9);
    }

    @Test
    void customParsesAllFieldsAndDefaults() {
        List<RaySpec> s = RayLayout.build(Pattern.CUSTOM, 6, 0.6, 270, 3, 0.3,
                List.of("0.6,270", " 0.9 , -90 , -4 , true , 0.8 "), -2, false, 1.0);
        assertEquals(2, s.size());
        assertEquals(new RaySpec(0.6, 270, -2, false, 1.0), s.get(0));
        assertEquals(new RaySpec(0.9, 270, -4, true, 0.8), s.get(1));
    }

    @Test
    void customSkipsInvalidEntries() {
        List<String> custom = List.of("0.6,270", "abc", "0.6", "1.5,0", "0.5,0,-4,maybe", "0.5,0,-4,true,2",
                "0.5,0,20", "0.5,0,0,true,1,9", "0.3,90");
        List<RaySpec> s = RayLayout.build(Pattern.CUSTOM, 6, 0.6, 270, 3, 0.3, custom, -2, false, 1.0);
        assertEquals(2, s.size());
        assertEquals(7, RayLayout.invalidCount(custom));
    }

    @Test
    void capsAtTwentyFourSideRays() {
        assertEquals(24, build(Pattern.RING, 40, 0).size());
        List<RaySpec> dr = RayLayout.build(Pattern.DOUBLE_RING, 24, 0.6, 0, 12, 0.3, List.of(), 0, false, 1);
        assertEquals(24, dr.size());
        assertEquals(0.3, dr.get(0).spread(), 1e-9);
        List<String> many = new ArrayList<>();
        for (int i = 0; i < 30; i++) many.add("0.5," + i * 12);
        assertEquals(24, RayLayout.build(Pattern.CUSTOM, 6, 0.6, 0, 3, 0.3, many, 0, false, 1).size());
    }
}
