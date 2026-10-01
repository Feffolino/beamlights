package it.ratlab.beamlights.client;

import com.google.gson.JsonElement;
import it.ratlab.beamlights.BeamLights;
import it.ratlab.beamlights.core.LayoutProfile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

/**
 * Client resource layouts: assets/&lt;ns&gt;/beamlights/layouts/*.json (resource packs, KubeJS kubejs/assets/...),
 * reloaded with F3+T. Per provider name the highest priority wins (ties: later file id). Client thread.
 */
public final class LayoutProfiles extends SimpleJsonResourceReloadListener {
    public static final LayoutProfiles INSTANCE = new LayoutProfiles();

    private volatile Map<String, LayoutProfile> byProvider = Map.of();

    private LayoutProfiles() {
        super(new Gson(), "beamlights/layouts");
    }

    /** Profile for a provider name (BeamProvider.name(), e.g. "omegaflashlight", "data", "event"), or null. */
    public LayoutProfile get(String provider) {
        if (provider == null) return null;
        return byProvider.get(provider);
    }

    public int size() {
        return byProvider.size();
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, LayoutProfile> map = new HashMap<>();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            try {
                LayoutProfile p = LayoutProfile.parse(e.getValue());
                for (String provider : p.providers()) {
                    LayoutProfile old = map.get(provider);
                    if (old == null || p.priority() >= old.priority()) map.put(provider, p);
                }
            } catch (RuntimeException ex) {
                BeamLights.LOG.warn("Beam Lights: layout profile {} skipped: {}", e.getKey(), ex.getMessage());
            }
        });
        byProvider = Map.copyOf(map);
        BeamLights.LOG.info("Beam Lights: {} layout profile(s) loaded for providers {}", map.size(), map.keySet());
    }

    @EventBusSubscriber(modid = BeamLights.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        static void onRegister(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(INSTANCE);
        }
    }
}
