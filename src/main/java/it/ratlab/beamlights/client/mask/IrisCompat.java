package it.ratlab.beamlights.client.mask;

import it.ratlab.beamlights.BeamLights;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

/** Iris shader pack detection by reflection (no compile dependency). Client thread only. */
final class IrisCompat {
    private static boolean resolved;
    private static Object api;
    private static Method inUse;

    private IrisCompat() {
    }

    /** True when Iris is loaded and a shader pack is rendering; false without Iris or when the API is missing. */
    static boolean shaderPackInUse() {
        if (!resolved) resolve();
        if (inUse == null) return false;
        try {
            return (Boolean) inUse.invoke(api);
        } catch (Throwable t) {
            inUse = null;
            BeamLights.LOG.warn("Beam Lights: Iris API call failed, assuming no shader pack", t);
            return false;
        }
    }

    private static void resolve() {
        resolved = true;
        if (!ModList.get().isLoaded("iris")) return;
        try {
            Class<?> c = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            api = c.getMethod("getInstance").invoke(null);
            inUse = c.getMethod("isShaderPackInUse");
        } catch (Throwable t) {
            BeamLights.LOG.warn("Beam Lights: Iris loaded but its API was not found; the gamma mask stays on", t);
        }
    }
}
