package it.ratlab.beamlights.core;

/**
 * Stable light-source key: entity id (high bits), ray index (8 bits), slot (8 bits; 0 = hit, 1..n = midpoints).
 * The client uses ray = beamIndex * 32 + subRay (sub 0 = central, 1..24 side rays), so up to 8 beams per entity.
 * Slot bit 7 marks a fading ghost of the same key (see LightSmoother), so planner slots stay below 128.
 */
public final class Keys {
    /** Slot bit of a ghost key: the fading copy left behind when a light jumps or disappears. */
    public static final int GHOST_BIT = 0x80;
    /** Ray indices per beam: ray = beamIndex * RAYS_PER_BEAM + subRay (sub 0 = central). */
    public static final int RAYS_PER_BEAM = 32;

    private Keys() {
    }

    public static long ghost(long key) {
        return key | GHOST_BIT;
    }

    public static boolean isGhost(long key) {
        return (key & GHOST_BIT) != 0;
    }

    public static long of(int entityId, int ray, int slot) {
        return ((long) entityId << 16) | ((long) (ray & 0xFF) << 8) | (slot & 0xFF);
    }

    public static int entityId(long key) {
        return (int) (key >> 16);
    }

    public static int ray(long key) {
        return (int) ((key >> 8) & 0xFF);
    }

    public static int slot(long key) {
        return (int) (key & 0xFF);
    }

    /** Hit point (slot 0) of a central ray, not a ghost: the light the player looks at. */
    public static boolean isCentralHit(long key) {
        return slot(key) == 0 && ray(key) % RAYS_PER_BEAM == 0;
    }
}
