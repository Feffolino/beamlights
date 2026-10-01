package it.ratlab.beamlights.core;

import it.ratlab.beamlights.api.math.V3;
import it.ratlab.beamlights.core.MotionGovernor.SideMode;
import it.ratlab.beamlights.core.MotionGovernor.State;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MotionGovernorTest {
    private static final MotionGovernor.Settings S = MotionGovernor.Settings.DEFAULT;
    private static final long CENTRAL = Keys.of(3, 0, 0), MID = Keys.of(3, 0, 1), SIDE = Keys.of(3, 1, 0);

    private static State step(MotionGovernor g, double turn, double move) {
        g.setSpeeds(turn, move);
        return g.advance(1, S);
    }

    @Test
    void angleAndEma() {
        assertEquals(90, MotionGovernor.angleDeg(new V3(1, 0, 0), new V3(0, 0, 2)), 1e-9);
        assertEquals(0, MotionGovernor.angleDeg(new V3(1, 0, 0), new V3(3, 0, 0)), 1e-6);
        assertEquals(0, MotionGovernor.angleDeg(V3.ZERO, new V3(1, 0, 0)));
        assertEquals(15, MotionGovernor.ema(10, 20, 0.5), 1e-9);
    }

    @Test
    void observeMeasuresSpeedsWithEma() {
        MotionGovernor g = new MotionGovernor();
        V3 o = new V3(0, 64, 0);
        g.observe(o, new V3(1, 0, 0), 1, S);
        assertEquals(0, g.turnDegPerSec(), 1e-9);
        // 10 degrees in one tick = 200 deg/s, EMA 0.5 -> 100 = FAST at once.
        double a = Math.toRadians(10);
        assertEquals(State.FAST, g.observe(o, new V3(Math.cos(a), 0, Math.sin(a)), 1, S));
        assertEquals(100, g.turnDegPerSec(), 1e-6);
        // Origin 1 block in 2 ticks = 10 blocks/s (sample), EMA -> 5.
        g.observe(new V3(1, 64, 0), new V3(Math.cos(a), 0, Math.sin(a)), 2, S);
        assertEquals(5, g.moveBlocksPerSec(), 1e-6);
        assertEquals(50, g.turnDegPerSec(), 1e-6);
    }

    @Test
    void transitionsWithHysteresis() {
        MotionGovernor g = new MotionGovernor();
        assertEquals(State.STILL, step(g, 0, 0));
        assertEquals(State.STILL, step(g, 19, 1.9));
        assertEquals(State.MOVING, step(g, 30, 0));
        assertEquals(State.FAST, step(g, 90, 0));
        // FAST is kept down to 0.7 x 90 = 63.
        assertEquals(State.FAST, step(g, 70, 0));
        assertEquals(State.FAST, step(g, 63, 0));
        assertEquals(State.MOVING, step(g, 62, 0));
        // Back above 63 but below 90: MOVING stays MOVING.
        assertEquals(State.MOVING, step(g, 80, 0));
        // Translation alone: 8 blocks/s = FAST, kept down to 5.6.
        assertEquals(State.FAST, step(g, 0, 8));
        assertEquals(State.FAST, step(g, 0, 5.7));
        assertEquals(State.MOVING, step(g, 0, 5.5));
        // Walking (2 blocks/s and more) is not slow.
        assertEquals(State.MOVING, step(g, 0, 2.0));
    }

    @Test
    void settleNeedsSlowTicksInARowThenResyncsOnce() {
        MotionGovernor g = new MotionGovernor();
        step(g, 200, 0);
        for (int i = 1; i < S.settleTicks(); i++) {
            assertEquals(State.MOVING, step(g, 5, 0), "slow tick " + i);
            assertFalse(g.resync());
        }
        // A fast-ish tick resets the count.
        assertEquals(State.MOVING, step(g, 25, 0));
        for (int i = 1; i < S.settleTicks(); i++) step(g, 5, 0);
        assertEquals(State.STILL, step(g, 5, 0));
        int resyncTicks = 0;
        for (int i = 0; i < 30; i++) {
            if (g.resync()) resyncTicks++;
            assertEquals(State.STILL, step(g, 0, 0));
        }
        assertEquals(S.settleTicks(), resyncTicks);
    }

    private static List<LightSmoother.Target> targets(double x, int lum) {
        return List.of(new LightSmoother.Target(CENTRAL, new V3(x, 64, 0), lum),
                new LightSmoother.Target(MID, new V3(x / 2, 64, 0), lum - 4),
                new LightSmoother.Target(SIDE, new V3(x, 66, 3), lum - 2));
    }

    private static List<LightSmoother.Target> filter(MotionGovernor g, List<LightSmoother.Target> fresh,
                                                     MotionGovernor.Settings s) {
        List<LightSmoother.Target> out = new ArrayList<>();
        g.filter(fresh, out, s);
        return out;
    }

    private static V3 pos(List<LightSmoother.Target> out, long key) {
        return out.stream().filter(t -> t.key() == key).findFirst().map(LightSmoother.Target::pos).orElse(null);
    }

    @Test
    void fastFreezesSideRaysAndMidpointsButNotTheCentralHit() {
        MotionGovernor g = new MotionGovernor();
        step(g, 0, 0);
        filter(g, targets(10, 12), S);
        step(g, 300, 0);
        List<LightSmoother.Target> out = filter(g, targets(20, 12), S);
        assertEquals(new V3(20, 64, 0), pos(out, CENTRAL));
        assertEquals(new V3(5, 64, 0), pos(out, MID));
        assertEquals(new V3(10, 66, 3), pos(out, SIDE));
        assertEquals(2, g.suppressed());
        // A held light whose ray found nothing stays lit.
        out = filter(g, List.of(new LightSmoother.Target(CENTRAL, new V3(30, 64, 0), 12)), S);
        assertEquals(3, out.size());
        assertEquals(2, g.suppressed());
    }

    @Test
    void fadeModeDropsSideTargetsWhileFast() {
        MotionGovernor.Settings fade = new MotionGovernor.Settings(90, 20, 8, 3, 6, SideMode.FADE);
        MotionGovernor g = new MotionGovernor();
        g.advance(1, fade);
        filter(g, targets(10, 12), fade);
        g.setSpeeds(300, 0);
        g.advance(1, fade);
        List<LightSmoother.Target> out = filter(g, targets(20, 12), fade);
        assertEquals(1, out.size());
        assertEquals(CENTRAL, out.get(0).key());
    }

    @Test
    void offModePassesEverything() {
        MotionGovernor.Settings off = new MotionGovernor.Settings(90, 20, 8, 3, 6, SideMode.OFF);
        MotionGovernor g = new MotionGovernor();
        g.setSpeeds(300, 0);
        assertEquals(State.FAST, g.advance(1, off));
        assertEquals(targets(20, 12), filter(g, targets(20, 12), off));
        assertEquals(0, g.suppressed());
    }

    @Test
    void movingRefreshesSideRaysEverySideUpdateTicks() {
        MotionGovernor g = new MotionGovernor();
        int refreshes = 0;
        for (int t = 0; t < 12; t++) {
            assertEquals(State.MOVING, step(g, 40, 0));
            List<LightSmoother.Target> out = filter(g, targets(t, 12), S);
            assertEquals(new V3(t, 64, 0), pos(out, CENTRAL));
            if (new V3(t, 66, 3).equals(pos(out, SIDE))) refreshes++;
        }
        assertEquals(12 / S.sideUpdateTicks(), refreshes);
    }
}
