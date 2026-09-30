package it.ratlab.beamlights.core;

/**
 * Stable light-source key: entity id (high bits), ray index (8 bits), slot (8 bits; 0 = hit, 1..n = midpoints).
 * The client uses ray = beamIndex * 32 + subRay (sub 0 = central, 1..24 side rays), so up to 8 beams per entity.
 */
public final class Keys {
    private Keys() {
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
}
