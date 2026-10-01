package it.ratlab.beamlights.client.debug;

import it.ratlab.beamlights.config.BeamClientConfig;

import java.util.List;

/** Debug toggles (command overrides win over config) and the last traced frame. Client thread only. */
public final class DebugState {
    private static Boolean overlayOverride;
    private static Boolean renderOverride;
    /** Ticks until the requested dump, -1 = none. */
    private static int dumpIn = -1;
    private static List<TracedBeam> lastFrame = List.of();

    private DebugState() {
    }

    public static boolean overlay() {
        return overlayOverride != null ? overlayOverride : BeamClientConfig.DEBUG_OVERLAY.get();
    }

    public static boolean render() {
        return renderOverride != null ? renderOverride : BeamClientConfig.DEBUG_RENDER.get();
    }

    public static void setOverlay(boolean on) {
        overlayOverride = on;
    }

    public static void setRender(boolean on) {
        renderOverride = on;
    }

    /**
     * Dumps the tick delayTicks from now. The view is frozen while the chat is open, so a dump right after the command
     * always shows a still beam (moved=false everywhere); the delay leaves time to move.
     */
    public static void requestDump(int delayTicks) {
        dumpIn = Math.max(0, delayTicks);
    }

    /** True on the tick the dump is written (the ticker captures the frame). */
    public static boolean dumpRequested() {
        return dumpIn == 0;
    }

    /** Called once at the end of every tick: true when the dump must be written now, else counts down. */
    public static boolean consumeDump() {
        if (dumpIn < 0) return false;
        if (dumpIn == 0) {
            dumpIn = -1;
            return true;
        }
        dumpIn--;
        return false;
    }

    public static List<TracedBeam> lastFrame() {
        return lastFrame;
    }

    public static void setLastFrame(List<TracedBeam> frame) {
        lastFrame = frame;
    }
}
