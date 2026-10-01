package it.ratlab.beamlights.core;

import it.ratlab.beamlights.api.math.V3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class V3Test {
    @Test
    void arithmetic() {
        V3 a = new V3(1, 2, 3);
        V3 b = new V3(4, 6, 8);
        assertEquals(new V3(5, 8, 11), a.add(b));
        assertEquals(new V3(3, 4, 5), b.sub(a));
        assertEquals(new V3(2, 4, 6), a.scale(2));
        assertEquals(4 + 12 + 24, a.dot(b), 1e-9);
        assertEquals(9 + 16 + 25, a.distSq(b), 1e-9);
    }

    @Test
    void normalize() {
        V3 n = new V3(3, 0, 4).normalize();
        assertEquals(1.0, n.length(), 1e-9);
        assertEquals(0.6, n.x(), 1e-9);
        assertEquals(V3.ZERO, V3.ZERO.normalize());
    }
}
