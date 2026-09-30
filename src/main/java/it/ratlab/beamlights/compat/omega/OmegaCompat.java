package it.ratlab.beamlights.compat.omega;

import it.ratlab.beamlights.core.BeamRegistry;

/** Entry point; only called after ModList.isLoaded("omegaflashlight"). */
public final class OmegaCompat {
    private OmegaCompat() {
    }

    public static void register() {
        BeamRegistry.INSTANCE.register(new OmegaProvider());
    }
}
