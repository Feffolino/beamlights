package it.ratlab.beamlights.core;

import it.ratlab.beamlights.BeamLights;
import it.ratlab.beamlights.api.Beam;
import it.ratlab.beamlights.api.BeamProvider;
import it.ratlab.beamlights.api.event.BeamCollectEvent;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Ordered provider list. A failing provider is logged once and skipped for that entity. collect posts
 * BeamCollectEvent after the providers (the bus has no cheap "any listeners" check; a post without listeners is cheap).
 */
public final class BeamRegistry {
    public static final BeamRegistry INSTANCE = new BeamRegistry();
    private static final String EVENT_NAME = "event";

    private final List<BeamProvider> providers = new CopyOnWriteArrayList<>();
    private final Set<String> warned = ConcurrentHashMap.newKeySet();

    private BeamRegistry() {
    }

    public void register(BeamProvider provider) {
        providers.add(provider);
        BeamLights.LOG.info("Beam Lights: registered beam provider '{}'", provider.name());
    }

    public boolean unregister(BeamProvider provider) {
        boolean removed = providers.remove(provider);
        if (removed) BeamLights.LOG.info("Beam Lights: unregistered beam provider '{}'", provider.name());
        return removed;
    }

    public List<String> providerNames() {
        return providers.stream().map(BeamProvider::name).toList();
    }

    public boolean mayEmit(Entity entity) {
        for (BeamProvider p : providers) {
            try {
                if (p.mayEmit(entity)) return true;
            } catch (RuntimeException | LinkageError e) {
                warnOnce(p.name(), e);
            }
        }
        return false;
    }

    /**
     * Clears {@code out} and fills it with the beams of every provider, then posts BeamCollectEvent (listeners may edit
     * the list). With {@code names} (debug output) the provider name of each beam is kept parallel to out; beams added
     * or replaced by a listener are named "event".
     */
    public void collect(Entity entity, float partialTick, List<Beam> out, List<String> names) {
        out.clear();
        if (names != null) names.clear();
        for (BeamProvider p : providers) {
            try {
                if (!p.mayEmit(entity)) continue;
                int before = out.size();
                p.collect(entity, partialTick, out::add);
                if (names != null) {
                    for (int i = before; i < out.size(); i++) names.add(p.name());
                }
            } catch (RuntimeException | LinkageError e) {
                warnOnce(p.name(), e);
            }
        }
        Beam[] snapshot = names != null ? out.toArray(new Beam[0]) : null;
        try {
            NeoForge.EVENT_BUS.post(new BeamCollectEvent(entity, partialTick, out));
        } catch (RuntimeException | LinkageError e) {
            warnOnce(EVENT_NAME, e);
        }
        out.removeIf(Objects::isNull);
        if (names != null) remapNames(snapshot, out, names);
    }

    // Name by identity: untouched beams keep their provider name.
    private static void remapNames(Beam[] before, List<Beam> after, List<String> names) {
        if (before.length == after.size()) {
            boolean same = true;
            for (int i = 0; i < before.length && same; i++) same = before[i] == after.get(i);
            if (same) return;
        }
        String[] old = names.toArray(new String[0]);
        names.clear();
        for (Beam b : after) {
            String n = EVENT_NAME;
            for (int j = 0; j < before.length; j++) {
                if (before[j] == b) {
                    n = old[j];
                    break;
                }
            }
            names.add(n);
        }
    }

    private void warnOnce(String name, Throwable e) {
        if (warned.add(name)) BeamLights.LOG.warn("Beam Lights: provider '{}' failed, skipping entity", name, e);
    }
}
