package it.ratlab.beamlights.core;

import it.ratlab.beamlights.api.math.V3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Source changes (each one = up to 8 chunk-section rebuilds) of the smoother + backend gate pipeline, 0.8.1 settings
 * vs the 0.8.2 balanced defaults.
 */
class ChangeRateTest {
    private static final LightSmoother.Settings OLD_SMOOTH = new LightSmoother.Settings(true, 0.5, 3.0, 4);
    private static final SourceMotion.Gate OLD_GATE = SourceMotion.Gate.legacy(0.25);
    private static final LightSmoother.Settings NEW_SMOOTH = new LightSmoother.Settings(true, 0.5, 3.0, 4, 2, 2, true);
    private static final SourceMotion.Gate NEW_GATE = new SourceMotion.Gate(0.25, 1.5, 0.75, 1);

    // Central hit, two central midpoints, five side ray hits (8 sources, like the in-game report).
    private static final long[] KEYS = {
            Keys.of(27, 0, 0), Keys.of(27, 0, 1), Keys.of(27, 0, 2),
            Keys.of(27, 1, 0), Keys.of(27, 2, 0), Keys.of(27, 3, 0), Keys.of(27, 4, 0), Keys.of(27, 5, 0)};
    private static final int[] LUMS = {10, 6, 6, 8, 8, 8, 8, 8};

    private record Shown(V3 pos, int lum) {
    }

    /** Mimics SdlBackend with an unlimited budget: counts creations, moves, luminance changes and removals. */
    private static int changes(LightSmoother.Settings ss, SourceMotion.Gate gate, int warmup, int ticks,
                               IntFunction<List<LightSmoother.Target>> targets) {
        LightSmoother smoother = new LightSmoother();
        Map<Long, Shown> shown = new HashMap<>();
        int counted = 0;
        for (int tick = 0; tick < warmup + ticks; tick++) {
            int n = 0;
            Set<Long> seen = new HashSet<>();
            for (LightSmoother.Output o : smoother.update(targets.apply(tick), ss)) {
                V3 pos = SourceMotion.snapToBlock(o.pos());
                seen.add(o.key());
                Shown s = shown.get(o.key());
                if (s == null || SourceMotion.shouldChange(s.pos().distSq(pos), s.lum(), o.luminance(),
                        Keys.isCentralHit(o.key()), gate)) {
                    shown.put(o.key(), new Shown(pos, o.luminance()));
                    n++;
                }
            }
            int before = shown.size();
            shown.keySet().retainAll(seen);
            n += before - shown.size();
            if (tick >= warmup) counted += n;
        }
        return counted;
    }

    /** All 8 points slide along a wall at speed blocks/tick (staggered offsets), after warmup still ticks. */
    private static IntFunction<List<LightSmoother.Target>> sweep(int warmup, double speed) {
        return tick -> {
            double x = Math.max(0, tick - warmup) * speed;
            List<LightSmoother.Target> out = new ArrayList<>();
            for (int i = 0; i < KEYS.length; i++) {
                out.add(new LightSmoother.Target(KEYS[i], new V3(x + i * 0.37 + 0.1, 64.2 + (i % 3), 10.5 + i),
                        LUMS[i]));
            }
            return out;
        };
    }

    @Test
    void slowSweepAcrossWallMakesFarFewerChanges() {
        int warmup = 10, ticks = 40;
        int old = changes(OLD_SMOOTH, OLD_GATE, warmup, ticks, sweep(warmup, 0.3));
        int now = changes(NEW_SMOOTH, NEW_GATE, warmup, ticks, sweep(warmup, 0.3));
        System.out.println("slow sweep, 8 sources, 40 ticks: old " + old + " changes, new " + now);
        // 8 points x 12 blocks: the old pipeline moves on every block change (~96), the new one every >= 1.5-2 blocks.
        assertTrue(old >= 85, "old " + old);
        assertTrue(now <= 60, "new " + now);
        assertTrue(now * 10 <= old * 6, "new " + now + " vs old " + old);
    }

    @Test
    void jumpsCostHalfTheChanges() {
        int warmup = 10, ticks = 40;
        // Every 10 ticks all points jump 5 blocks (beam crossing a wall edge): crossfade instead of glide.
        IntFunction<List<LightSmoother.Target>> jumps = tick -> {
            double x = tick < warmup ? 0 : 5.0 * ((tick - warmup) / 10 + 1);
            List<LightSmoother.Target> out = new ArrayList<>();
            for (int i = 0; i < KEYS.length; i++) {
                out.add(new LightSmoother.Target(KEYS[i], new V3(x + 0.5, 64.5 + i, 10.5), LUMS[i]));
            }
            return out;
        };
        int old = changes(OLD_SMOOTH, OLD_GATE, warmup, ticks, jumps);
        int now = changes(NEW_SMOOTH, NEW_GATE, warmup, ticks, jumps);
        // Per jump and point: old 4 ramp + 4 ghost steps = 8, new 2 + 2 = 4.
        assertEquals(4 * 8 * 8, old);
        assertEquals(4 * 8 * 4, now);
    }

    @Test
    void stillBeamMakesNoChanges() {
        int warmup = 10;
        assertEquals(0, changes(NEW_SMOOTH, NEW_GATE, warmup, 40, sweep(1000, 0.3)));
    }

    @Test
    void gateHysteresisAndLuminanceTolerance() {
        SourceMotion.Gate g = NEW_GATE;
        // Neighbour block (1 block): central moves, others wait; two blocks: everyone moves.
        assertTrue(SourceMotion.shouldChange(1.0, 8, 8, true, g));
        assertFalse(SourceMotion.shouldChange(1.0, 8, 8, false, g));
        assertFalse(SourceMotion.shouldChange(2.0, 8, 8, false, g));
        assertTrue(SourceMotion.shouldChange(3.0, 8, 8, false, g));
        assertTrue(SourceMotion.shouldChange(4.0, 8, 8, false, g));
        // Same block: never a move; 1 level of luminance ignored except for the central hit point.
        assertFalse(SourceMotion.shouldChange(0, 8, 8, true, g));
        assertFalse(SourceMotion.shouldChange(0, 8, 7, false, g));
        assertTrue(SourceMotion.shouldChange(0, 8, 6, false, g));
        assertTrue(SourceMotion.shouldChange(0, 10, 9, true, g));
        // Legacy gate: threshold only, every luminance change.
        assertTrue(SourceMotion.shouldChange(1.0, 8, 8, false, OLD_GATE));
        assertTrue(SourceMotion.shouldChange(0, 8, 7, false, OLD_GATE));
    }

    @Test
    void centralHitKey() {
        assertTrue(Keys.isCentralHit(Keys.of(5, 0, 0)));
        assertTrue(Keys.isCentralHit(Keys.of(5, 32, 0)));
        assertFalse(Keys.isCentralHit(Keys.of(5, 0, 1)));
        assertFalse(Keys.isCentralHit(Keys.of(5, 1, 0)));
        assertFalse(Keys.isCentralHit(Keys.ghost(Keys.of(5, 0, 0))));
    }

    @Test
    void quantizedFadesUseAtMostStepsLevels() {
        // fadeTicks 4, 2 steps: ramp half, half, full, full; ghost half, half, off.
        assertArrayEquals(new int[]{5, 5, 10, 10}, new int[]{LightSmoother.rampLum(10, 1, 4, 2),
                LightSmoother.rampLum(10, 2, 4, 2), LightSmoother.rampLum(10, 3, 4, 2), LightSmoother.rampLum(10, 4, 4, 2)});
        assertArrayEquals(new int[]{4, 4, 0, 0}, new int[]{LightSmoother.fadeLum(8, 1, 4, 2),
                LightSmoother.fadeLum(8, 2, 4, 2), LightSmoother.fadeLum(8, 3, 4, 2), LightSmoother.fadeLum(8, 4, 4, 2)});
        // fadeTicks 20, 2 steps: still only two distinct ramp levels.
        Set<Integer> levels = new HashSet<>();
        for (int t = 1; t <= 20; t++) levels.add(LightSmoother.rampLum(12, t, 20, 2));
        assertEquals(Set.of(6, 12), levels);
        // 1 step: no fade at all; 0 or steps >= fadeTicks: the linear 0.8.1 fade.
        assertEquals(10, LightSmoother.rampLum(10, 1, 4, 1));
        assertEquals(0, LightSmoother.fadeLum(10, 1, 4, 1));
        assertEquals(LightSmoother.rampLum(10, 1, 4), LightSmoother.rampLum(10, 1, 4, 0));
        assertEquals(LightSmoother.fadeLum(10, 2, 4), LightSmoother.fadeLum(10, 2, 4, 4));
    }

    @Test
    void glideChangesAtMostEveryGlideMinTicks() {
        LightSmoother s = new LightSmoother();
        LightSmoother.Settings ss = new LightSmoother.Settings(true, 0.5, 8.0, 1, 2, 3, false);
        long k = Keys.of(1, 0, 0);
        s.update(List.of(new LightSmoother.Target(k, new V3(0, 0, 0), 10)), ss);
        V3 last = new V3(0, 0, 0);
        int changes = 0;
        for (int t = 1; t <= 12; t++) {
            V3 p = s.update(List.of(new LightSmoother.Target(k, new V3(t * 0.3, 0, 0), 10)), ss).get(0).pos();
            if (!p.equals(last)) changes++;
            last = p;
        }
        assertEquals(4, changes);
    }
}
