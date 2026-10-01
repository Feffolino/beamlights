package it.ratlab.beamlights.core;

import it.ratlab.beamlights.api.math.V3;

/** Decides when a light source is worth moving: every move costs chunk rebuilds in the backend. */
public final class SourceMotion {
    private SourceMotion() {
    }

    public static boolean shouldMove(V3 current, V3 target, int currentLuminance, int targetLuminance, double threshold) {
        if (current == null) return true;
        return shouldMove(current.distSq(target), currentLuminance, targetLuminance, threshold);
    }

    /** Same with a precomputed squared distance (no allocation). */
    public static boolean shouldMove(double distSq, int currentLuminance, int targetLuminance, double threshold) {
        return currentLuminance != targetLuminance || distSq >= threshold * threshold;
    }

    /**
     * Center of the block containing p. Sodium Dynamic Lights lights whole blocks, so a snapped source only moves (and
     * rebuilds sections) when it enters another block.
     */
    public static V3 snapToBlock(V3 p) {
        return new V3(Math.floor(p.x()) + 0.5, Math.floor(p.y()) + 0.5, Math.floor(p.z()) + 0.5);
    }

    /** True on the ticks a remote emitter is re-traced: every interval ticks, staggered by entity id. */
    public static boolean remoteDue(int entityId, long tick, int interval) {
        if (interval <= 1) return true;
        return Math.floorMod(tick + entityId, interval) == 0;
    }
}
