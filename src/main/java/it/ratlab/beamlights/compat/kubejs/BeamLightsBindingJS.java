package it.ratlab.beamlights.compat.kubejs;

import it.ratlab.beamlights.api.Beam;
import it.ratlab.beamlights.api.BeamLightsApi;
import net.minecraft.world.entity.Entity;

import java.util.List;

/** The BeamLights script binding; works on the side of the given entity (server scripts: server definitions). */
public final class BeamLightsBindingJS {
    /** True when any provider emits a lit beam (luminance > 0) for the entity right now. */
    public boolean isBeamActive(Entity entity) {
        return BeamLightsApi.isBeamActive(entity);
    }

    /** Number of lit beams the entity emits right now. */
    public int beamCount(Entity entity) {
        int n = 0;
        for (Beam b : BeamLightsApi.getBeams(entity)) {
            if (b.luminance() > 0) n++;
        }
        return n;
    }

    /** Lit and unlit beams the entity emits right now (read-only snapshot). */
    public List<Beam> beams(Entity entity) {
        return BeamLightsApi.getBeams(entity);
    }
}
