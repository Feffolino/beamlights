package it.ratlab.beamlights.core;

import it.ratlab.beamlights.core.math.V3;

/** Decides when a light source is worth moving: every move costs chunk rebuilds in the backend. */
public final class SourceMotion {
    private SourceMotion() {
    }

    public static boolean shouldMove(V3 current, V3 target, int currentLuminance, int targetLuminance, double threshold) {
        if (current == null || currentLuminance != targetLuminance) return true;
        return current.distSq(target) >= threshold * threshold;
    }
}
