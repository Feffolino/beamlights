package it.ratlab.beamlights.core;

import it.ratlab.beamlights.api.math.V3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Governor + smoother + backend gate pipeline (balanced settings) with 9 sources: central hit, 2 central midpoints,
 * RING of 6 side rays. The player stands in a round room (radius 10) and sweeps the camera at 180 deg/s.
 */
class MotionChangeRateTest {
    private static final LightSmoother.Settings SMOOTH = new LightSmoother.Settings(true, 0.5, 3.0, 4, 2, 2, true);
    private static final SourceMotion.Gate GATE = new SourceMotion.Gate(0.25, 1.5, 0.75, 1);
    private static final MotionGovernor.Settings FREEZE = MotionGovernor.Settings.DEFAULT;
    private static final MotionGovernor.Settings OFF = new MotionGovernor.Settings(90, 20, 8, 3, 6,
            MotionGovernor.SideMode.OFF);
    private static final int ID = 27, WARMUP = 20, SWEEP = 40, STILL = 60;
    private static final double R = 10, DEG_PER_TICK = 9, SIDE_DEG = 12;
    private static final V3 EYE = new V3(0.5, 65.6, 0.5);

    private record Shown(V3 pos, int lum) {
    }

    private static double yaw(int tick) {
        // 30 ticks to the right, 10 back: 180 deg/s all along, ending 180 degrees away from the start.
        int moving = Math.max(0, Math.min(SWEEP, tick - WARMUP));
        int back = Math.max(0, moving - 30);
        return Math.toRadians(20 + (moving - 2 * back) * DEG_PER_TICK);
    }

    private static V3 wall(double yaw, double pitch, double dist) {
        return new V3(EYE.x() + dist * Math.cos(yaw), EYE.y() + dist * Math.tan(pitch), EYE.z() + dist * Math.sin(yaw));
    }

    private static List<LightSmoother.Target> trace(int tick) {
        double y = yaw(tick);
        List<LightSmoother.Target> out = new ArrayList<>();
        out.add(new LightSmoother.Target(Keys.of(ID, 0, 0), wall(y, 0, R - 0.5), 12));
        out.add(new LightSmoother.Target(Keys.of(ID, 0, 1), wall(y, 0, 3.3), 8));
        out.add(new LightSmoother.Target(Keys.of(ID, 0, 2), wall(y, 0, 6.6), 8));
        for (int i = 0; i < 6; i++) {
            double roll = Math.toRadians(270 + 60 * i);
            double sy = y + Math.toRadians(SIDE_DEG * Math.cos(roll));
            double sp = Math.toRadians(SIDE_DEG * Math.sin(roll));
            out.add(new LightSmoother.Target(Keys.of(ID, i + 1, 0), wall(sy, sp, R - 0.5), 10));
        }
        return out;
    }

    /** Changes per tick (creation, move, luminance change, removal) like SdlBackend with an unlimited budget. */
    private static int[] run(MotionGovernor.Settings ms, Map<Long, Shown> shown, int[] nonCentral) {
        MotionGovernor gov = new MotionGovernor();
        LightSmoother smoother = new LightSmoother();
        int total = WARMUP + SWEEP + STILL;
        int[] perTick = new int[total];
        for (int tick = 0; tick < total; tick++) {
            double y = yaw(tick);
            gov.observe(EYE, new V3(Math.cos(y), 0, Math.sin(y)), 1, ms);
            List<LightSmoother.Target> filtered = new ArrayList<>();
            gov.filter(trace(tick), filtered, ms);
            SourceMotion.Gate g = gov.resync() ? GATE.exact() : GATE;
            Set<Long> seen = new HashSet<>();
            int n = 0;
            for (LightSmoother.Output o : smoother.update(filtered, SMOOTH)) {
                V3 pos = SourceMotion.snapToBlock(o.pos());
                seen.add(o.key());
                Shown s = shown.get(o.key());
                boolean central = Keys.isCentralHit(o.key() & ~Keys.GHOST_BIT);
                if (s == null || SourceMotion.shouldChange(s.pos().distSq(pos), s.lum(), o.luminance(),
                        Keys.isCentralHit(o.key()),
                        o.ghost() ? GATE : g)) {
                    shown.put(o.key(), new Shown(pos, o.luminance()));
                    n++;
                    if (!central && nonCentral != null) nonCentral[tick]++;
                }
            }
            int before = shown.size();
            shown.keySet().retainAll(seen);
            n += before - shown.size();
            perTick[tick] = n;
        }
        return perTick;
    }

    private static int sum(int[] a, int from, int to) {
        int s = 0;
        for (int i = from; i < to; i++) s += a[i];
        return s;
    }

    @Test
    void fastSweepChangesOnlyTheCentralHitThenOneResyncBurst() {
        Map<Long, Shown> shown = new HashMap<>();
        int[] side = new int[WARMUP + SWEEP + STILL];
        int[] gov = run(FREEZE, shown, side);
        int[] off = run(OFF, new HashMap<>(), null);
        int sweepGov = sum(gov, WARMUP, WARMUP + SWEEP), sweepOff = sum(off, WARMUP, WARMUP + SWEEP);
        int stillGov = sum(gov, WARMUP + SWEEP, gov.length), stillOff = sum(off, WARMUP + SWEEP, off.length);
        System.out.println("180 deg/s sweep, 9 sources, 40 ticks: governor " + sweepGov + " changes (off "
                + sweepOff + "); settling: governor " + stillGov + " (off " + stillOff + ")");

        // While turning fast: at most ~1 change per tick, and only the central hit point (and its crossfade ghost).
        assertEquals(0, sum(side, WARMUP, WARMUP + SWEEP), "side/midpoint changes while fast");
        assertTrue(sweepGov <= SWEEP * 1.1, "sweep " + sweepGov);
        assertTrue(sweepGov * 4 < sweepOff, "governor " + sweepGov + " vs off " + sweepOff);

        // Settling: one contiguous burst (no 5-tick gap inside), then nothing at all.
        int first = -1, last = -1;
        for (int t = WARMUP + SWEEP; t < gov.length; t++) {
            if (gov[t] == 0) continue;
            if (first < 0) first = t;
            assertTrue(last < 0 || t - last < 5, "second burst at tick " + (t - WARMUP - SWEEP));
            last = t;
        }
        assertTrue(first >= 0, "no resync");
        assertTrue(last - WARMUP - SWEEP <= 20, "burst ends at still tick " + (last - WARMUP - SWEEP));
        assertTrue(stillGov <= 9 * 6, "burst " + stillGov);

        // After the resync every light sits in the block of its exact target (no hysteresis leftover).
        for (LightSmoother.Target t : trace(gov.length - 1)) {
            Shown s = shown.get(t.key());
            assertNotNull(s, "missing " + Keys.ray(t.key()) + "/" + Keys.slot(t.key()));
            assertEquals(SourceMotion.snapToBlock(t.pos()), s.pos(), "ray " + Keys.ray(t.key()));
            assertEquals(t.luminance(), s.lum());
        }
    }
}
