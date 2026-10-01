package it.ratlab.beamlights.client;

import it.ratlab.beamlights.api.math.V3;

/**
 * Receives the light points of one tick: begin(), put() for every point, end() removes keys not put this tick.
 * Client thread only.
 */
public interface LightBackend {
    String name();

    void begin();

    void put(long key, V3 pos, int luminance);

    void end();

    /** Removes every light this backend created. */
    void clear();

    int ownCount();

    /** Total sources in the underlying light engine, or -1 when unknown. */
    int totalCount();

    int movesThisTick();

    boolean movedThisTick(long key);

    /** One line for status/overlay: mode, or why the backend is inactive. */
    String statusLine();
}
