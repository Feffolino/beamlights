package it.ratlab.beamlights.client;

import it.ratlab.beamlights.api.math.V3;

/** Used when no light engine is available; carries the reason for /beamlights status. */
public final class NoopBackend implements LightBackend {
    private final String reason;

    public NoopBackend(String reason) {
        this.reason = reason;
    }

    @Override public String name() { return "none"; }
    @Override public void begin() { }
    @Override public void put(long key, V3 pos, int luminance) { }
    @Override public void end() { }
    @Override public void clear() { }
    @Override public int ownCount() { return 0; }
    @Override public int totalCount() { return -1; }
    @Override public int movesThisTick() { return 0; }
    @Override public boolean movedThisTick(long key) { return false; }
    @Override public String statusLine() { return reason; }
}
