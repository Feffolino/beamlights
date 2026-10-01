package it.ratlab.beamlights.client;

import it.ratlab.beamlights.api.math.V3;

/**
 * Receives the light points of one tick: begin(), put() for every point, end() removes keys not put this tick.
 * Client thread only.
 */
public interface LightBackend {
    String name();

    /**
     * Starts a tick. viewer = camera/player position (distance ranking), moveBudget = max source changes applied
     * this tick (0 = unlimited); the rest is deferred to later ticks.
     */
    void begin(V3 viewer, int moveBudget);

    /** priority = local player's central ray: its changes are applied first. */
    void put(long key, V3 pos, int luminance, boolean priority);

    void end();

    /** Removes every light this backend created. */
    void clear();

    int ownCount();

    /** Total sources in the underlying light engine, or -1 when unknown. */
    int totalCount();

    int movesThisTick();

    /** Changes left for later ticks because the move budget was used up. */
    int deferredThisTick();

    boolean movedThisTick(long key);

    /** One line for status/overlay: mode, or why the backend is inactive. */
    String statusLine();
}
