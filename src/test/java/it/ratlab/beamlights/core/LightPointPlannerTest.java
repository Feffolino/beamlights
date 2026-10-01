package it.ratlab.beamlights.core;

import it.ratlab.beamlights.core.LightPointPlanner.PlannedPoint;
import it.ratlab.beamlights.core.LightPointPlanner.Status;
import it.ratlab.beamlights.api.math.V3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LightPointPlannerTest {
    private static final V3 O = new V3(0.5, 64.5, 0.5);
    private static final V3 DIR = new V3(1, 0, 0);
    private static final LightPointPlanner.OccluderProbe ALWAYS_NEAR = p -> true;

    private static LightPointPlanner.Settings settings(boolean mid, int maxPerBeam) {
        return new LightPointPlanner.Settings(mid, 6.0, -4, maxPerBeam, 3.0, 0.5);
    }

    private static BeamTracer.Result hitAt(double d) {
        return new BeamTracer.Result(true, O.add(DIR.scale(d)), d, 0, 0, 0);
    }

    private static BeamTracer.Result miss(double range) {
        return new BeamTracer.Result(false, O.add(DIR.scale(range)), range, 0, 0, 0);
    }

    @Test
    void hitOnlyWhenMidpointsOff() {
        LightPointPlanner.Plan plan = LightPointPlanner.plan(O, DIR, hitAt(20), 14, settings(false, 6), ALWAYS_NEAR);
        assertEquals(1, plan.points().size());
        PlannedPoint p = plan.points().get(0);
        assertEquals(0, p.slot());
        assertEquals(Status.ACCEPTED, p.status());
        assertEquals(14, p.luminance());
        assertEquals(O.x() + 19.5, p.pos().x(), 1e-9);
    }

    @Test
    void midpointsWithMergeNearHit() {
        LightPointPlanner.Plan plan = LightPointPlanner.plan(O, DIR, hitAt(20), 14, settings(true, 6), ALWAYS_NEAR);
        List<PlannedPoint> pts = plan.points();
        assertEquals(4, pts.size());
        assertEquals(Status.ACCEPTED, pts.get(1).status());   // d = 6
        assertEquals(Status.ACCEPTED, pts.get(2).status());   // d = 12
        assertEquals(Status.MERGED, pts.get(3).status());     // d = 18, 1.5 from hit at 19.5
        assertEquals(10, pts.get(1).luminance());
        assertEquals(3, plan.accepted().size());
        assertEquals(1, plan.count(Status.MERGED));
    }

    @Test
    void missHasNoHitPointButMidpoints() {
        LightPointPlanner.Plan plan = LightPointPlanner.plan(O, DIR, miss(13), 14, settings(true, 6), ALWAYS_NEAR);
        assertEquals(2, plan.points().size());
        assertEquals(1, plan.points().get(0).slot());
        assertEquals(2, plan.points().get(1).slot());
    }

    @Test
    void openAirMidpointsSkipped() {
        LightPointPlanner.OccluderProbe nearOnlyClose = p -> p.x() < 10;
        LightPointPlanner.Plan plan = LightPointPlanner.plan(O, DIR, miss(20), 14, settings(true, 6), nearOnlyClose);
        assertEquals(Status.ACCEPTED, plan.points().get(0).status());     // d = 6
        assertEquals(Status.SKIPPED_AIR, plan.points().get(1).status());  // d = 12
        assertEquals(Status.SKIPPED_AIR, plan.points().get(2).status());  // d = 18
    }

    @Test
    void capLimitsAcceptedPoints() {
        LightPointPlanner.Plan plan = LightPointPlanner.plan(O, DIR, miss(30), 14, settings(true, 2), ALWAYS_NEAR);
        assertEquals(2, plan.accepted().size());
        assertEquals(2, plan.count(Status.CAPPED));  // d = 18, 24
    }

    @Test
    void zeroLuminanceGivesEmptyPlan() {
        assertTrue(LightPointPlanner.plan(O, DIR, hitAt(10), 0, settings(true, 6), ALWAYS_NEAR).points().isEmpty());
    }

    @Test
    void midLuminanceNeverBelowOne() {
        LightPointPlanner.Plan plan = LightPointPlanner.plan(O, DIR, miss(7), 3, settings(true, 6), ALWAYS_NEAR);
        assertEquals(1, plan.points().get(0).luminance());
    }

    @Test
    void sideHitMergedAgainstSharedPoint() {
        List<V3> shared = new ArrayList<>(List.of(O.add(DIR.scale(18.5))));
        LightPointPlanner.Plan plan = LightPointPlanner.plan(O, DIR, hitAt(20), 12, settings(false, 6), ALWAYS_NEAR,
                shared, true);
        assertEquals(Status.MERGED, plan.points().get(0).status());
        assertEquals(1, shared.size());
    }

    @Test
    void sideHitAcceptedWhenFarAndAppended() {
        List<V3> shared = new ArrayList<>(List.of(O.add(DIR.scale(5))));
        LightPointPlanner.Plan plan = LightPointPlanner.plan(O, DIR, hitAt(20), 12, settings(false, 6), ALWAYS_NEAR,
                shared, true);
        assertEquals(Status.ACCEPTED, plan.points().get(0).status());
        assertEquals(2, shared.size());
    }

    @Test
    void hitNotMergedWithoutMergeHit() {
        List<V3> shared = new ArrayList<>(List.of(O.add(DIR.scale(19.5))));
        LightPointPlanner.Plan plan = LightPointPlanner.plan(O, DIR, hitAt(20), 12, settings(false, 6), ALWAYS_NEAR,
                shared, false);
        assertEquals(Status.ACCEPTED, plan.points().get(0).status());
    }

    @Test
    void capIgnoresSharedPoints() {
        List<V3> shared = new ArrayList<>();
        for (int i = 0; i < 10; i++) shared.add(new V3(0, 200 + i * 10, 0));
        LightPointPlanner.Plan plan = LightPointPlanner.plan(O, DIR, miss(30), 14, settings(true, 2), ALWAYS_NEAR,
                shared, true);
        assertEquals(2, plan.accepted().size());
        assertEquals(2, plan.count(Status.CAPPED));
        assertEquals(12, shared.size());
    }
}
