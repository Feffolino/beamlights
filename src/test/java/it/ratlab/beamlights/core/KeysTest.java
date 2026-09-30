package it.ratlab.beamlights.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeysTest {
    @Test
    void roundTrip() {
        long k = Keys.of(123456, 3, 7);
        assertEquals(123456, Keys.entityId(k));
        assertEquals(3, Keys.ray(k));
        assertEquals(7, Keys.slot(k));
    }

    @Test
    void distinct() {
        assertNotEquals(Keys.of(1, 0, 1), Keys.of(1, 1, 0));
        assertNotEquals(Keys.of(1, 0, 0), Keys.of(2, 0, 0));
    }
}
