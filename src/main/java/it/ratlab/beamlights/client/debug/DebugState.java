package it.ratlab.beamlights.client.debug;

import it.ratlab.beamlights.config.BeamClientConfig;

import java.util.List;

/** Debug toggles (command overrides win over config) and the last traced frame. Client thread only. */
public final class DebugState {
    private static Boolean overlayOverride;
    private static Boolean renderOverride;
    private static boolean dumpRequested;
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

    public static void requestDump() {
        dumpRequested = true;
    }

    public static boolean dumpRequested() {
        return dumpRequested;
    }

    public static boolean consumeDump() {
        boolean r = dumpRequested;
        dumpRequested = false;
        return r;
    }

    public static List<TracedBeam> lastFrame() {
        return lastFrame;
    }

    public static void setLastFrame(List<TracedBeam> frame) {
        lastFrame = frame;
    }
}
