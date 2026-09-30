package it.ratlab.beamlights.api;

import net.minecraft.world.entity.Entity;

import java.util.function.Consumer;

/**
 * Supplies beams emitted by entities; keep it cheap. Called every tick on the client thread (light) and on the server
 * thread (spawn blocking): check entity.level().isClientSide() before touching client-only classes or the client config.
 */
public interface BeamProvider {
    /** Short id shown in debug output. */
    String name();

    /** Cheap pre-filter; the ticker only calls collect for entities that pass. */
    boolean mayEmit(Entity entity);

    /** Adds the beams this entity emits right now (none when off). */
    void collect(Entity entity, float partialTick, Consumer<Beam> out);
}
