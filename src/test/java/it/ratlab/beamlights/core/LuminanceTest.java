package it.ratlab.beamlights.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LuminanceTest {
    @Test
    void baseWithoutBattery() {
        assertEquals(14, Luminance.compute(14, false, 0.1, 0.3, 1f));
    }

    @Test
    void batteryScaling() {
        assertEquals(7, Luminance.compute(14, true, 0.5, 0.3, 1f));
    }

    @Test
    void batteryFloor() {
        assertEquals(3, Luminance.compute(10, true, 0.05, 0.3, 1f));
    }

    @Test
    void batteryUnknownTreatedAsFull() {
        assertEquals(10, Luminance.compute(10, true, Double.NaN, 0.3, 1f));
    }

    @Test
    void flickerMultipliesAndClamps() {
        assertEquals(5, Luminance.compute(10, false, 1, 0.3, 0.5f));
        assertEquals(10, Luminance.compute(10, false, 1, 0.3, 3f));
        assertEquals(0, Luminance.compute(10, false, 1, 0.3, -1f));
    }

    @Test
    void clampedTo15() {
        assertEquals(15, Luminance.compute(40, false, 1, 0.3, 1f));
    }

    @Test
    void tierFromMultiplier() {
        assertEquals(1, Luminance.tierFromMultiplier(1.0f, 1.5f, 2.0f));
        assertEquals(2, Luminance.tierFromMultiplier(1.45f, 1.5f, 2.0f));
        assertEquals(3, Luminance.tierFromMultiplier(2.2f, 1.5f, 2.0f));
    }
}
