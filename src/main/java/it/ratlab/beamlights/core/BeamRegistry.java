package it.ratlab.beamlights.core;

import it.ratlab.beamlights.BeamLights;
import it.ratlab.beamlights.api.Beam;
import it.ratlab.beamlights.api.BeamProvider;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Ordered provider list. A failing provider is logged once and skipped for that entity. */
public final class BeamRegistry {
    public static final BeamRegistry INSTANCE = new BeamRegistry();

    private final List<BeamProvider> providers = new CopyOnWriteArrayList<>();
    private final Set<String> warned = ConcurrentHashMap.newKeySet();

    private BeamRegistry() {
    }

    public void register(BeamProvider provider) {
        providers.add(provider);
        BeamLights.LOG.info("Beam Lights: registered beam provider '{}'", provider.name());
    }

    public List<String> providerNames() {
        return providers.stream().map(BeamProvider::name).toList();
    }

    public boolean mayEmit(Entity entity) {
        for (BeamProvider p : providers) {
            try {
                if (p.mayEmit(entity)) return true;
            } catch (RuntimeException | LinkageError e) {
                warnOnce(p, e);
            }
        }
        return false;
    }

    public void collect(Entity entity, float partialTick, Consumer<Beam> out) {
        for (BeamProvider p : providers) {
            try {
                if (p.mayEmit(entity)) p.collect(entity, partialTick, out);
            } catch (RuntimeException | LinkageError e) {
                warnOnce(p, e);
            }
        }
    }

    /** Like collect, with the provider name of each beam (debug output). */
    public void collectNamed(Entity entity, float partialTick, BiConsumer<String, Beam> out) {
        for (BeamProvider p : providers) {
            try {
                if (p.mayEmit(entity)) p.collect(entity, partialTick, b -> out.accept(p.name(), b));
            } catch (RuntimeException | LinkageError e) {
                warnOnce(p, e);
            }
        }
    }

    private void warnOnce(BeamProvider p, Throwable e) {
        if (warned.add(p.name())) BeamLights.LOG.warn("Beam Lights: provider '{}' failed, skipping entity", p.name(), e);
    }
}
