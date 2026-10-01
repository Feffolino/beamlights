package it.ratlab.beamlights.api;

import net.minecraft.world.entity.Entity;

import java.util.function.Consumer;

/**
 * Supplies the beams an entity emits. Register once with {@link BeamLightsApi#register(BeamProvider)}.
 *
 * <p>Side and thread: called on the client thread (light, every client tick for every rendered entity near the player)
 * and on the server thread (spawn blocking and mob attraction, on demand). Check {@code entity.level().isClientSide()}
 * before touching client-only classes or client config. Implementations must not keep the consumer.
 *
 * <p>Cost: {@link #mayEmit} runs for every candidate entity every tick, so it must be a few field reads (held item
 * checks, a cached flag). {@link #collect} runs only when mayEmit returned true; emit at most a few beams per entity
 * (the client uses at most 8 beams per entity, the data provider 2). Exceptions are caught, logged once per provider
 * and the provider is skipped for that entity.
 */
public interface BeamProvider {
    /** Short stable id shown in debug output, e.g. "mymod:lantern". */
    String name();

    /** Cheap pre-filter; collect is only called for entities that pass. Both sides, hot path. */
    boolean mayEmit(Entity entity);

    /**
     * Adds the beams this entity emits right now (none when off). Both sides.
     *
     * @param partialTick 1.0 on the tick paths (client ticker, server); interpolation factor otherwise
     */
    void collect(Entity entity, float partialTick, Consumer<Beam> out);
}
