package it.ratlab.beamlights.client.sdl;

import it.ratlab.beamlights.client.GatedLightBackend;
import net.minecraft.world.level.Level;
import toni.sodiumdynamiclights.SodiumDynamicLights;

/** Sodium Dynamic Lights backend: one pooled BeamLightSource per key; gating and budget in GatedLightBackend. */
public final class SdlBackend extends GatedLightBackend<BeamLightSource> {
    private final SodiumDynamicLights sdl;

    private SdlBackend(SodiumDynamicLights sdl) {
        this.sdl = sdl;
    }

    public static SdlBackend create() {
        SodiumDynamicLights sdl = SodiumDynamicLights.get();
        if (sdl == null) throw new IllegalStateException("SodiumDynamicLights.get() returned null");
        return new SdlBackend(sdl);
    }

    @Override
    public String name() {
        return "sodiumdynamiclights";
    }

    @Override
    protected BeamLightSource newSource(Level level) {
        return new BeamLightSource(level);
    }

    @Override public int totalCount() { return sdl.getLightSourcesCount(); }

    @Override
    public String statusLine() {
        return "SDL mode " + sdl.config.getDynamicLightsMode();
    }
}
