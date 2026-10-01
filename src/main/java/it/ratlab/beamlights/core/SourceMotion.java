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
     * Change gate of a shown light source. threshold = moveThreshold; hysteresis / centralHysteresis = minimum distance
     * (blocks) between the shown position and the target before a move (the central hit point uses the smaller one);
     * lumTolerance = luminance difference ignored for lights other than the central hit point.
     */
    public record Gate(double threshold, double hysteresis, double centralHysteresis, int lumTolerance) {
        /** The 0.8.1 behaviour: moveThreshold only, every luminance change applied. */
        public static Gate legacy(double threshold) {
            return new Gate(threshold, 0, 0, 0);
        }

        /** Resync after the emitter settles (motion STILL): moveThreshold only, every luminance change. */
        public Gate exact() {
            return new Gate(threshold, 0, 0, 0);
        }
    }

    /**
     * True when a shown light (distSq from its target, currentLuminance) must be changed. A move needs the target at
     * least max(threshold, hysteresis) away; with snapped positions that means another block AND that distance. A
     * luminance change counts when it exceeds the tolerance (exact for the central hit point). A move also applies
     * the pending luminance.
     */
    public static boolean shouldChange(double distSq, int currentLuminance, int targetLuminance, boolean central,
                                       Gate g) {
        int dl = Math.abs(targetLuminance - currentLuminance);
        if (dl > (central ? 0 : Math.max(0, g.lumTolerance()))) return true;
        double min = Math.max(g.threshold(), central ? g.centralHysteresis() : g.hysteresis());
        return distSq > 0 && distSq >= min * min;
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
