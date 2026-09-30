package it.ratlab.beamlights.client;

import it.ratlab.beamlights.BeamLights;
import it.ratlab.beamlights.client.sdl.SdlBackend;
import net.neoforged.fml.ModList;

/** Picks the light backend. Optional-mod classes are only touched after the isLoaded check. */
public final class BackendFactory {
    private BackendFactory() {
    }

    public static LightBackend create() {
        if (!ModList.get().isLoaded("sodiumdynamiclights")) {
            return new NoopBackend("Sodium Dynamic Lights not installed");
        }
        try {
            return SdlBackend.create();
        } catch (Throwable t) {
            BeamLights.LOG.warn("Beam Lights: Sodium Dynamic Lights backend failed to start", t);
            return new NoopBackend("SDL init failed: " + t);
        }
    }
}
