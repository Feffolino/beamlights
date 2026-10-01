package it.ratlab.beamlights.core;

import it.ratlab.beamlights.core.LightSmoother.Output;
import it.ratlab.beamlights.core.LightSmoother.Settings;
import it.ratlab.beamlights.core.LightSmoother.Target;
import it.ratlab.beamlights.api.math.V3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LightSmootherTest {
    private static final long K = Keys.of(5, 0, 0);
    private static final long G = Keys.ghost(K);
    private static final Settings ON = new Settings(true, 0.5, 3.0, 4);

    private static Output find(List<Output> out, long key) {
        return out.stream().filter(o -> o.key() == key).findFirst().orElse(null);
    }

    private static List<Output> tick(LightSmoother s, V3 pos, int lum) {
        return s.update(List.of(new Target(K, pos, lum)), ON);
    }

    private static void settle(LightSmoother s, V3 pos, int lum) {
        for (int i = 0; i < 10; i++) tick(s, pos, lum);
    }

    @Test
    void newKeyRampsUpOverFadeTicks() {
        LightSmoother s = new LightSmoother();
        V3 p = new V3(1, 2, 3);
        int[] expected = {3, 6, 9, 12, 12};
        for (int e : expected) {
            Output o = find(tick(s, p, 12), K);
            assertNotNull(o);
            assertEquals(e, o.luminance());
            assertEquals(p, o.pos());
            assertFalse(o.ghost());
        }
    }

    @Test
    void rampIsAtLeastOne() {
        LightSmoother s = new LightSmoother();
        assertEquals(1, find(tick(s, V3.ZERO, 1), K).luminance());
    }

    @Test
    void smallMoveGlides() {
        LightSmoother s = new LightSmoother();
        settle(s, V3.ZERO, 12);
        List<Output> out = tick(s, new V3(2, 0, 0), 12);
        assertEquals(1, out.size());
        assertEquals(1.0, find(out, K).pos().x(), 1e-9);
        assertEquals(1.5, find(tick(s, new V3(2, 0, 0), 12), K).pos().x(), 1e-9);
        // Snaps once closer than 0.05.
        settle(s, new V3(2, 0, 0), 12);
        assertEquals(new V3(2, 0, 0), find(tick(s, new V3(2, 0, 0), 12), K).pos());
    }

    @Test
    void luminanceChangeIsRateLimited() {
        LightSmoother s = new LightSmoother();
        settle(s, V3.ZERO, 15);
        assertEquals(11, find(tick(s, V3.ZERO, 2), K).luminance());
        assertEquals(7, find(tick(s, V3.ZERO, 2), K).luminance());
    }

    @Test
    void jumpCrossfades() {
        LightSmoother s = new LightSmoother();
        V3 a = V3.ZERO;
        V3 b = new V3(10, 0, 0);
        settle(s, a, 12);
        int[] ghost = {9, 6, 3};
        int[] ramp = {3, 6, 9};
        for (int i = 0; i < 3; i++) {
            List<Output> out = tick(s, b, 12);
            Output g = find(out, G);
            Output n = find(out, K);
            assertNotNull(g);
            assertTrue(g.ghost());
            assertEquals(a, g.pos());
            assertEquals(ghost[i], g.luminance());
            assertEquals(b, n.pos());
            assertEquals(ramp[i], n.luminance());
        }
        List<Output> out = tick(s, b, 12);
        assertNull(find(out, G));
        assertEquals(12, find(out, K).luminance());
        assertEquals(0, s.ghostCount());
    }

    @Test
    void untargetedKeyFadesOutThenIsDropped() {
        LightSmoother s = new LightSmoother();
        V3 p = new V3(4, 5, 6);
        settle(s, p, 12);
        int[] expected = {9, 6, 3};
        for (int e : expected) {
            List<Output> out = s.update(List.of(), ON);
            assertEquals(1, out.size());
            assertEquals(G, out.get(0).key());
            assertEquals(p, out.get(0).pos());
            assertEquals(e, out.get(0).luminance());
        }
        assertTrue(s.update(List.of(), ON).isEmpty());
        assertEquals(0, s.ghostCount());
        // Coming back is a new key: ramps up again.
        assertEquals(3, find(tick(s, p, 12), K).luminance());
    }

    @Test
    void newerJumpReplacesGhost() {
        LightSmoother s = new LightSmoother();
        settle(s, V3.ZERO, 12);
        tick(s, new V3(10, 0, 0), 12);
        tick(s, new V3(10, 0, 0), 12); // lum 6 at x=10
        List<Output> out = tick(s, new V3(20, 0, 0), 12);
        assertEquals(1, out.stream().filter(Output::ghost).count());
        Output g = find(out, G);
        assertEquals(new V3(10, 0, 0), g.pos());
        assertEquals(5, g.luminance()); // round(6 * 3/4)
    }

    @Test
    void disabledPassesThrough() {
        LightSmoother s = new LightSmoother();
        Settings off = new Settings(false, 0.5, 3.0, 4);
        List<Target> in = List.of(new Target(K, new V3(1, 1, 1), 12), new Target(Keys.of(6, 1, 2), V3.ZERO, 7));
        List<Output> out = s.update(in, off);
        assertEquals(2, out.size());
        for (int i = 0; i < in.size(); i++) {
            assertEquals(in.get(i).key(), out.get(i).key());
            assertEquals(in.get(i).pos(), out.get(i).pos());
            assertEquals(in.get(i).luminance(), out.get(i).luminance());
            assertFalse(out.get(i).ghost());
        }
        assertTrue(s.update(List.of(), off).isEmpty());
        assertEquals(0, s.ghostCount());
    }

    @Test
    void fadeTicksOneTeleports() {
        LightSmoother s = new LightSmoother();
        Settings fast = new Settings(true, 1.0, 3.0, 1);
        assertEquals(12, find(s.update(List.of(new Target(K, V3.ZERO, 12)), fast), K).luminance());
        List<Output> out = s.update(List.of(new Target(K, new V3(10, 0, 0), 12)), fast);
        assertEquals(1, out.size());
        assertEquals(12, out.get(0).luminance());
    }
}
