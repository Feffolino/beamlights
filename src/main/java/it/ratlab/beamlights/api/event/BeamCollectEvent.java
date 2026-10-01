package it.ratlab.beamlights.api.event;

import it.ratlab.beamlights.api.Beam;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;

import java.util.List;

/**
 * Posted on {@code NeoForge.EVENT_BUS} after every provider collected the beams of one entity, on both sides (client
 * thread for light, server thread for spawn blocking and mob attraction). Only fired for entities that at least one
 * provider accepts in {@link it.ratlab.beamlights.api.BeamProvider#mayEmit}; register a provider to give beams to
 * other entities.
 *
 * <p>The list is mutable: add, remove or replace beams ({@link Beam} is immutable, use {@code withLuminance} etc.).
 * Null entries are dropped. Do not keep the list after the listener returns. Listener exceptions are caught and
 * logged once.
 *
 * <p>Cost: fired per emitting entity per client tick (and per server lookup), so listeners must be cheap; check
 * {@link #isClientSide()} before touching client-only classes.
 */
public final class BeamCollectEvent extends Event {
    private final Entity entity;
    private final float partialTick;
    private final List<Beam> beams;

    /** Internal: posted by Beam Lights. */
    public BeamCollectEvent(Entity entity, float partialTick, List<Beam> beams) {
        this.entity = entity;
        this.partialTick = partialTick;
        this.beams = beams;
    }

    /** The emitting entity. */
    public Entity getEntity() {
        return entity;
    }

    /** True on the client (light), false on the server (spawn blocking, mob attraction). */
    public boolean isClientSide() {
        return entity.level().isClientSide();
    }

    /** Same value the providers received. */
    public float getPartialTick() {
        return partialTick;
    }

    /** Collected beams, mutable. */
    public List<Beam> getBeams() {
        return beams;
    }
}
