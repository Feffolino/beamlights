package it.ratlab.beamlights.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeamValuesTest {
    private static final List<String> HANDS = List.of("mainhand", "offhand");

    @Test
    void acceptsDefaultsAndLimits() {
        assertNull(BeamValues.check(12, 16, 25, 0, 0, HANDS));
        assertNull(BeamValues.check(0, 1, 1, -4, 4, List.of("head")));
        assertNull(BeamValues.check(15, 128, 89, 4, -4, List.of("curios", "HEAD")));
    }

    @Test
    void rejectsOutOfRange() {
        assertTrue(BeamValues.check(16, 16, 25, 0, 0, HANDS).startsWith("luminance"));
        assertTrue(BeamValues.check(-1, 16, 25, 0, 0, HANDS).startsWith("luminance"));
        assertTrue(BeamValues.check(10, 0.5, 25, 0, 0, HANDS).startsWith("range"));
        assertTrue(BeamValues.check(10, 129, 25, 0, 0, HANDS).startsWith("range"));
        assertTrue(BeamValues.check(10, 16, 90, 0, 0, HANDS).startsWith("cone"));
        assertTrue(BeamValues.check(10, 16, 25, 4.5, 0, HANDS).startsWith("origin.forward"));
        assertTrue(BeamValues.check(10, 16, 25, 0, -5, HANDS).startsWith("origin.down"));
    }

    @Test
    void rejectsNaN() {
        assertNotNull(BeamValues.check(10, Double.NaN, 25, 0, 0, HANDS));
        assertNotNull(BeamValues.check(10, 16, Double.NaN, 0, 0, HANDS));
        assertNotNull(BeamValues.check(10, 16, 25, Double.NaN, 0, HANDS));
    }

    @Test
    void rejectsBadSlots() {
        assertNotNull(BeamValues.check(10, 16, 25, 0, 0, List.of()));
        assertNotNull(BeamValues.check(10, 16, 25, 0, 0, null));
        assertTrue(BeamValues.check(10, 16, 25, 0, 0, List.of("feet")).contains("unknown slot"));
    }

    @Test
    void slotIdsAreLowerCase() {
        assertTrue(BeamDefinition.Slot.MAINHAND.id().equals("mainhand"));
        assertTrue(BeamDefinition.Slot.parse("curios") == BeamDefinition.Slot.CURIOS);
    }
}
