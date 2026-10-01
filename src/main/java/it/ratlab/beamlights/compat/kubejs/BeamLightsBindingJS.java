package it.ratlab.beamlights.compat.kubejs;

import it.ratlab.beamlights.core.BeamRegistry;
import net.minecraft.world.entity.Entity;

/** The BeamLights script binding; works on the side of the given entity (server scripts: server definitions). */
public final class BeamLightsBindingJS {
    /** True when any provider emits a lit beam (luminance > 0) for the entity right now. */
    public boolean isBeamActive(Entity entity) {
        return beamCount(entity) > 0;
    }

    /** Number of lit beams the entity emits right now. */
    public int beamCount(Entity entity) {
        if (entity == null || !BeamRegistry.INSTANCE.mayEmit(entity)) return 0;
        int[] n = {0};
        BeamRegistry.INSTANCE.collect(entity, 1.0f, b -> {
            if (b.luminance() > 0) n[0]++;
        });
        return n[0];
    }
}
