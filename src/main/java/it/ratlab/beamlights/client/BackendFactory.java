package it.ratlab.beamlights.client;

import it.ratlab.beamlights.BeamLights;
import it.ratlab.beamlights.client.ldl.LdlBackend;
import it.ratlab.beamlights.client.sdl.SdlBackend;
import net.neoforged.fml.ModList;

/**
 * Picks the light backend: Sodium Dynamic Lights, else LambDynamicLights (the two mods exclude each other), else none.
 * Optional-mod classes are only touched after the isLoaded check.
 */
public final class BackendFactory {
    /** Mod id of LambDynamicLights (the wrapper mod; its runtime is lambdynlights_runtime). */
    public static final String LDL_MOD_ID = "lambdynlights";

    private BackendFactory() {
    }

    public static LightBackend create() {
        ModList mods = ModList.get();
        if (mods.isLoaded("sodiumdynamiclights")) {
            try {
                return SdlBackend.create();
            } catch (Throwable t) {
                BeamLights.LOG.warn("Beam Lights: Sodium Dynamic Lights backend failed to start", t);
                return new NoopBackend("SDL init failed: " + t);
            }
        }
        if (mods.isLoaded(LDL_MOD_ID)) {
            try {
                return LdlBackend.create();
            } catch (Throwable t) {
                BeamLights.LOG.warn("Beam Lights: LambDynamicLights backend failed to start", t);
                return new NoopBackend("LDL init failed: " + t);
            }
        }
        return new NoopBackend("no dynamic lights mod installed (Sodium Dynamic Lights or LambDynamicLights)");
    }
}
