package it.ratlab.beamlights.core;

import it.ratlab.beamlights.api.math.V3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConePolicyTest {
    private static final ConePolicy.Settings S = ConePolicy.Settings.BALANCED;
    private static final V3 O = new V3(0, 64, 0);
    private static final V3 X = new V3(1, 0, 0);

    private static ConeLight.Shape cone(V3 origin, V3 axis, double len, int lum) {
        return new ConeLight.Shape(origin, axis, len, 25, lum);
    }

    @Test
    void capsAngleAndLength() {
        ConeLight.Shape c = ConePolicy.target(O, X, true, 19.5, 0.5, 42, 12, S);
        assertEquals(16, c.length(), 1e-9);
        assertEquals(25, c.halfAngleDeg(), 1e-9);
        ConeLight.Shape narrow = ConePolicy.target(O, X, true, 8.5, 0.5, 10, 12, S);
        assertEquals(8, narrow.length(), 1e-9);
        assertEquals(10, narrow.halfAngleDeg(), 1e-9);
    }

    @Test
    void maxDistanceRule() {
        assertNotNull(ConePolicy.target(O, X, true, 20.0, 0.5, 30, 12, S));
        assertNull(ConePolicy.target(O, X, true, 20.01, 0.5, 30, 12, S));
        assertNull(ConePolicy.target(O, X, false, 0, 0.5, 30, 12, S));
        assertNotNull(ConePolicy.target(O, X, true, 27, 0.5, 30, 12, ConePolicy.Settings.WIDE));
        assertNull(ConePolicy.target(O, X, true, 15, 0.5, 30, 12, ConePolicy.Settings.LIGHT));
    }

    @Test
    void shortBeamsAndDarkConesGetNone() {
        assertNull(ConePolicy.target(O, X, true, 3.4, 0.5, 30, 12, S));
        assertNotNull(ConePolicy.target(O, X, true, 3.5, 0.5, 30, 12, S));
        assertNull(ConePolicy.target(O, X, true, 10, 0.5, 30, 0, S));
    }

    @Test
    void hysteresisHoldsSmallChanges() {
        ConeLight.Shape held = cone(O, X, 10, 12);
        assertSame(held, ConePolicy.gate(held, cone(O, X, 11.9, 12), false, S));
        assertSame(held, ConePolicy.gate(held, cone(new V3(0.9, 64, 0), X, 10, 12), false, S));
        V3 turned3 = new V3(Math.cos(Math.toRadians(3)), Math.sin(Math.toRadians(3)), 0);
        assertSame(held, ConePolicy.gate(held, cone(O, turned3, 10, 12), false, S));

        ConeLight.Shape longer = cone(O, X, 12, 12);
        assertSame(longer, ConePolicy.gate(held, longer, false, S));
        ConeLight.Shape moved = cone(new V3(1, 64, 0), X, 10, 12);
        assertSame(moved, ConePolicy.gate(held, moved, false, S));
        V3 turned5 = new V3(Math.cos(Math.toRadians(5)), Math.sin(Math.toRadians(5)), 0);
        ConeLight.Shape turned = cone(O, turned5, 10, 12);
        assertSame(turned, ConePolicy.gate(held, turned, false, S));
        ConeLight.Shape dimmer = cone(O, X, 10, 11);
        assertSame(dimmer, ConePolicy.gate(held, dimmer, false, S));
    }

    @Test
    void appearVanishAndFreeze() {
        ConeLight.Shape fresh = cone(O, X, 10, 12);
        assertSame(fresh, ConePolicy.gate(null, fresh, true, S));
        assertNull(ConePolicy.gate(fresh, null, false, S));
        ConeLight.Shape far = cone(new V3(5, 64, 5), new V3(0, 0, 1), 4, 12);
        assertSame(fresh, ConePolicy.gate(fresh, far, true, S));
        assertSame(fresh, ConePolicy.gate(fresh, null, true, S));
        ConePolicy.Settings noFreeze = new ConePolicy.Settings(25, 16, 3, 20, 2, 1, 4, false);
        assertSame(far, ConePolicy.gate(fresh, far, true, noFreeze));
    }
}
