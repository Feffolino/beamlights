package it.ratlab.beamlights.api;

import it.ratlab.beamlights.core.BeamRegistry;

/**
 * Public entry point for other mods. Register a {@link BeamProvider} once, on both sides, e.g. from
 * FMLCommonSetupEvent (inside enqueueWork): the client uses the beams for light, the server for spawn blocking and
 * mob attraction. Items that only need a fixed beam are easier to add with a datapack JSON
 * (data/&lt;ns&gt;/beamlights/beams/*.json).
 */
public final class BeamLightsApi {
    private BeamLightsApi() {
    }

    public static void register(BeamProvider provider) {
        BeamRegistry.INSTANCE.register(provider);
    }
}
