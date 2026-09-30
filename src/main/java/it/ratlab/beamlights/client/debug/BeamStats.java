package it.ratlab.beamlights.client.debug;

import it.ratlab.beamlights.client.LightBackend;
import it.ratlab.beamlights.config.BeamClientConfig;
import it.ratlab.beamlights.core.BeamRegistry;
import it.ratlab.beamlights.core.LightPointPlanner;
import it.ratlab.beamlights.core.LightPointPlanner.Status;

import java.util.List;
import java.util.Locale;

/** Per-tick counters plus smoothed tick cost and moves per second. */
public final class BeamStats {
    public int beams, accepted, skippedAir, merged, capped, globalCapped;
    /** Set by the ticker when the ray layout is rebuilt. */
    public String layout = "-";
    private double avgMicros;
    private int movesWindow;
    private long windowStart;
    private int movesPerSecond;

    public void beginTick() {
        beams = accepted = skippedAir = merged = capped = globalCapped = 0;
    }

    public void addPlan(LightPointPlanner.Plan plan) {
        beams++;
        accepted += plan.count(Status.ACCEPTED);
        skippedAir += plan.count(Status.SKIPPED_AIR);
        merged += plan.count(Status.MERGED);
        capped += plan.count(Status.CAPPED);
    }

    public void endTick(long nanos, int moves) {
        avgMicros = avgMicros * 0.9 + (nanos / 1000.0) * 0.1;
        movesWindow += moves;
        long now = System.currentTimeMillis();
        if (now - windowStart >= 1000) {
            movesPerSecond = movesWindow;
            movesWindow = 0;
            windowStart = now;
        }
    }

    public List<String> lines(LightBackend backend) {
        int total = backend.totalCount();
        return List.of(
                "[Beam Lights] backend: " + backend.name() + " (" + backend.statusLine() + ")",
                "sources: " + backend.ownCount() + " own / " + (total < 0 ? "?" : total) + " engine total, max "
                        + BeamClientConfig.MAX_SOURCES.get(),
                "beams: " + beams + "  points: " + accepted + " ok, " + skippedAir + " air, " + merged + " merged, "
                        + capped + " capped, " + globalCapped + " over max",
                String.format(Locale.ROOT, "moves/s: %d  tick: %.1f us", movesPerSecond, avgMicros),
                "layout: " + layout,
                "providers: " + BeamRegistry.INSTANCE.providerNames());
    }
}
