package it.ratlab.beamlights.client;

import it.ratlab.beamlights.api.math.V3;
import net.minecraft.world.level.Level;

/** One free-floating light of a GatedLightBackend (SDL source, LDL behavior). Client thread only. */
public interface PooledLight {
    Level level();

    boolean hasPosition();

    /** Squared distance to p without allocating. */
    double distSq(V3 p);

    int luminance();

    /** Moves the light and sets its level; the first call registers it with the light engine. */
    void set(V3 pos, int luminance);

    /** Turns the light off, lets the engine rebuild what it lit and unregisters it. */
    void remove();

    /** Called every tick the light is kept (SDL chunk tracking); no-op by default. */
    default void touch() {
    }
}
