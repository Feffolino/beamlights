package it.ratlab.beamlights.core;

/**
 * Per beam: is it pointing into an open area (no cone, openAreaPattern side rays) or not (cone + rayPattern)?
 * Open = no hit, hit farther than maxDistance, or (skyOpen) the lit point has sky light >= skyLight. Switching needs
 * the condition past a hysteresis band around maxDistance and the current state held for minTicks. Pure, not
 * thread-safe (client thread).
 */
public final class OpenAreaGate {
    public record Settings(double maxDistance, double hysteresis, int minTicks, boolean skyOpen, int skyLight) {
    }

    private boolean initialized;
    private boolean open;
    private int held;

    /** Updates with one trace; elapsed = ticks since the previous update (>= 1). Returns the (possibly held) state. */
    public boolean update(boolean hit, double distance, int skyLightAtHit, int elapsed, Settings s) {
        boolean sky = s.skyOpen() && hit && skyLightAtHit >= s.skyLight();
        if (!initialized) {
            initialized = true;
            open = !hit || distance > s.maxDistance() || sky;
            held = 0;
            return open;
        }
        held = Math.min(1_000_000, held + Math.max(1, elapsed));
        if (held < s.minTicks()) return open;
        if (!open) {
            if (!hit || distance > s.maxDistance() + s.hysteresis() || sky) {
                open = true;
                held = 0;
            }
        } else if (hit && distance < s.maxDistance() - s.hysteresis() && !sky) {
            open = false;
            held = 0;
        }
        return open;
    }

    public boolean open() {
        return open;
    }

    /** Ticks since the last switch (or since the first update). */
    public int held() {
        return held;
    }

    /**
     * Side ray luminance offset right after switching to open: -dim for the first fadeTicks, then 0 (two steps only,
     * every luminance change costs a chunk rebuild).
     */
    public static int fadeOffset(int heldTicks, int fadeTicks, int dim) {
        return fadeTicks > 0 && heldTicks < fadeTicks ? -dim : 0;
    }
}
