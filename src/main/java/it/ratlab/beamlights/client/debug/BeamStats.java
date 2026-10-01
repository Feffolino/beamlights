package it.ratlab.beamlights.client.debug;

import it.ratlab.beamlights.client.LightBackend;
import it.ratlab.beamlights.config.BeamClientConfig;
import it.ratlab.beamlights.core.BeamRegistry;
import it.ratlab.beamlights.core.LightPointPlanner;
import it.ratlab.beamlights.core.LightPointPlanner.Status;
import it.ratlab.beamlights.core.MotionGovernor;

import java.util.List;
import java.util.Locale;

/** Per-tick counters plus smoothed tick cost and moves per second. */
public final class BeamStats {
    public int beams, accepted, skippedAir, merged, capped, globalCapped, ghosts;
    /** Emitters reduced to the central ray (lodDistance) and emitters whose last points were reused this tick. */
    public int lodEmitters, reusedEmitters;
    public boolean snapped;
    /** Side ray / midpoint target changes held back by the motion governor this tick (all emitters). */
    public int suppressed;
    private MotionGovernor.State motionState = MotionGovernor.State.STILL;
    private double turnDegPerSec, moveBlocksPerSec;
    private boolean resync;
    private int movesTick, deferredTick;
    /** Set by the ticker when the ray layout is rebuilt. */
    public String layout = "-";
    private double avgMicros;
    private int movesWindow;
    private long windowStart;
    private int movesPerSecond;
    /** Chunk sections Sodium Dynamic Lights rebuilds per light source change (at most). */
    public static final int SECTIONS_PER_CHANGE = 8;

    public void beginTick() {
        beams = accepted = skippedAir = merged = capped = globalCapped = ghosts = lodEmitters = reusedEmitters = suppressed = 0;
    }

    /** Motion of the local player's beam (overlay). */
    public void setMotion(MotionGovernor.State state, double turnDegPerSec, double moveBlocksPerSec, boolean resync) {
        this.motionState = state;
        this.turnDegPerSec = turnDegPerSec;
        this.moveBlocksPerSec = moveBlocksPerSec;
        this.resync = resync;
    }

    /** Everything back to zero (mod disabled or no level): the overlay must not show stale numbers. */
    public void reset() {
        beginTick();
        snapped = false;
        movesTick = deferredTick = movesWindow = movesPerSecond = 0;
        avgMicros = 0;
        setMotion(MotionGovernor.State.STILL, 0, 0, false);
    }

    public String motionLine() {
        return String.format(Locale.ROOT, "motion: %s %.0f\u00B0/s, %.1f b/s%s, suppressed %d (mode %s)",
                motionState, turnDegPerSec, moveBlocksPerSec, resync ? ", resync" : "", suppressed,
                BeamClientConfig.FAST_SIDE_MODE.get());
    }

    public void addPlan(LightPointPlanner.Plan plan) {
        beams++;
        accepted += plan.count(Status.ACCEPTED);
        skippedAir += plan.count(Status.SKIPPED_AIR);
        merged += plan.count(Status.MERGED);
        capped += plan.count(Status.CAPPED);
    }

    public void endTick(long nanos, int moves, int deferred) {
        movesTick = moves;
        deferredTick = deferred;
        avgMicros = avgMicros * 0.9 + (nanos / 1000.0) * 0.1;
        movesWindow += moves;
        long now = System.currentTimeMillis();
        if (now - windowStart >= 1000) {
            movesPerSecond = movesWindow;
            movesWindow = 0;
            windowStart = now;
        }
    }

    /** Moves applied/deferred this tick, snapping, LOD and reuse counts (overlay and /beamlights perf). */
    public String perfLine() {
        int budget = BeamClientConfig.MAX_MOVES_PER_TICK.get();
        return "perf: moves " + movesTick + " applied, " + deferredTick + " deferred (budget "
                + (budget <= 0 ? "off" : budget) + "), snap " + (snapped ? "on" : "off") + ", LOD " + lodEmitters
                + " emitters, reused " + reusedEmitters + " (every " + BeamClientConfig.REMOTE_UPDATE_INTERVAL.get()
                + " t)";
    }

    public List<String> lines(LightBackend backend) {
        int total = backend.totalCount();
        return List.of(
                "[Beam Lights] backend: " + backend.name() + " (" + backend.statusLine() + ")",
                "sources: " + backend.ownCount() + " own (" + ghosts + " ghosts) / " + (total < 0 ? "?" : total)
                        + " engine total, max "
                        + BeamClientConfig.MAX_SOURCES.get(),
                "beams: " + beams + "  points: " + accepted + " ok, " + skippedAir + " air, " + merged + " merged, "
                        + capped + " capped, " + globalCapped + " over max",
                String.format(Locale.ROOT, "moves/s: %d  rebuilds/s ~%d  tick: %.1f us", movesPerSecond,
                        movesPerSecond * SECTIONS_PER_CHANGE, avgMicros),
                perfLine(),
                motionLine(),
                "layout: " + layout,
                "providers: " + BeamRegistry.INSTANCE.providerNames());
    }
}
