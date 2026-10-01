package it.ratlab.beamlights.core;

import it.ratlab.beamlights.api.math.V3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConeSmootherTest {
    private static final ConeSmoother.Settings ON = new ConeSmoother.Settings(true, 0.5, 3.0, 3, 2, 4, 2);

    private static List<ConeSmoother.Target> cone(double len, int lum) {
        return List.of(new ConeSmoother.Target(1L,
                new ConeLight.Shape(new V3(0, 0, 0), new V3(1, 0, 0), len, 20, lum)));
    }

    private static double len(List<ConeSmoother.Target> t) {
        return t.get(0).shape().length();
    }

    @Test
    void nearToFarTakesConeSteps() {
        ConeSmoother s = new ConeSmoother();
        s.update(cone(2, 12), ON);
        double[] seen = new double[6];
        for (int i = 0; i < 6; i++) seen[i] = len(s.update(cone(14, 12), ON));
        // glideMinTicks 2: steps at ticks 0, 2, 4 of +4 each
        assertEquals(6, seen[0], 1e-9);
        assertEquals(6, seen[1], 1e-9);
        assertEquals(10, seen[2], 1e-9);
        assertEquals(10, seen[3], 1e-9);
        assertEquals(14, seen[4], 1e-9);
        assertEquals(14, seen[5], 1e-9);
    }

    @Test
    void farToNearShrinksInSteps() {
        ConeSmoother s = new ConeSmoother();
        s.update(cone(14, 12), ON);
        assertEquals(10, len(s.update(cone(2, 12), ON)), 1e-9);
    }

    @Test
    void smallChangeLerpsAndSnaps() {
        ConeSmoother s = new ConeSmoother();
        s.update(cone(5, 12), ON);
        assertEquals(6, len(s.update(cone(7, 12), ON)), 1e-9);
        assertEquals(6.5, len(s.update(cone(7, 12), ON)), 1e-9);
        assertEquals(6.75, len(s.update(cone(7, 12), ON)), 1e-9);
        assertEquals(6.875, len(s.update(cone(7, 12), ON)), 1e-9);
        assertEquals(7, len(s.update(cone(7, 12), ON)), 1e-9);
    }

    @Test
    void disabledIsImmediate() {
        ConeSmoother s = new ConeSmoother();
        ConeSmoother.Settings off = new ConeSmoother.Settings(false, 0.5, 3.0, 3, 2, 4, 2);
        s.update(cone(2, 12), off);
        assertEquals(14, len(s.update(cone(14, 12), off)), 1e-9);
        assertEquals(0, s.update(List.of(), off).size());
    }

    @Test
    void removalFadesThenDrops() {
        ConeSmoother s = new ConeSmoother();
        s.update(cone(5, 12), ON);
        List<ConeSmoother.Target> a = s.update(List.of(), ON);
        assertEquals(1, a.size());
        int lum1 = a.get(0).shape().luminance();
        assertTrue(lum1 > 0 && lum1 < 12);
        assertEquals(5, len(a), 1e-9);
        int ticks = 0;
        while (!s.update(List.of(), ON).isEmpty() && ticks++ < 10) { }
        assertTrue(ticks < 5);
        assertEquals(0, s.ghostCount());
    }

    @Test
    void returningConeCancelsFade() {
        ConeSmoother s = new ConeSmoother();
        s.update(cone(5, 12), ON);
        s.update(List.of(), ON);
        List<ConeSmoother.Target> r = s.update(cone(5, 12), ON);
        assertEquals(12, r.get(0).shape().luminance());
        assertEquals(0, s.ghostCount());
    }
}
