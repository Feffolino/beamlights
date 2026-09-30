package it.ratlab.beamlights.client;

import net.neoforged.fml.ModList;

/** Picks the light backend. Optional-mod classes are only touched after the isLoaded check. */
public final class BackendFactory {
    private BackendFactory() {
    }

    public static LightBackend create() {
        if (!ModList.get().isLoaded("sodiumdynamiclights")) {
            return new NoopBackend("Sodium Dynamic Lights not installed");
        }
        return new NoopBackend("SDL backend not built yet");
    }
}
