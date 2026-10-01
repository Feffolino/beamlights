package it.ratlab.beamlights.api;

import it.ratlab.beamlights.core.BeamRegistry;
import it.ratlab.beamlights.core.BeamItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Public entry point for other mods. Package {@code it.ratlab.beamlights.api} and its subpackages are the stable API;
 * everything else is internal and may change between versions.
 *
 * <p>Ways to add a beam: a {@link BeamProvider} (code decides per tick), the item data
 * {@code beamlights:beam} in {@code minecraft:custom_data} ({@link #setBeam}, one stack), a datapack JSON
 * (data/&lt;ns&gt;/beamlights/beams/*.json, item ids or tags), or the
 * {@link it.ratlab.beamlights.api.event.BeamCollectEvent} (edit what the providers produced).
 */
public final class BeamLightsApi {
    /** Bumped on incompatible API changes. 1 = Beam Lights 0.8.0. */
    public static final int API_VERSION = 1;

    private BeamLightsApi() {
    }

    /**
     * Registers a provider on both sides. Call once, e.g. from FMLCommonSetupEvent inside enqueueWork: the client
     * uses beams for light, the server for spawn blocking and mob attraction. Thread-safe.
     */
    public static void register(BeamProvider provider) {
        BeamRegistry.INSTANCE.register(provider);
    }

    /** Removes a provider registered before; true when it was registered. Thread-safe. */
    public static boolean unregister(BeamProvider provider) {
        return BeamRegistry.INSTANCE.unregister(provider);
    }

    /** Names of the registered providers, in call order. Thread-safe. */
    public static List<String> providerNames() {
        return BeamRegistry.INSTANCE.providerNames();
    }

    /**
     * True when the entity emits at least one lit beam (luminance &gt; 0) right now, on the entity's side. Runs the
     * providers and the collect event: fine for occasional checks, avoid calling it for many entities every tick.
     * Call on the thread that owns the entity's level.
     */
    public static boolean isBeamActive(Entity entity) {
        for (Beam b : getBeams(entity)) {
            if (b.luminance() > 0) return true;
        }
        return false;
    }

    /**
     * Unmodifiable snapshot of the beams the entity emits right now on its side (providers plus BeamCollectEvent;
     * beams with luminance 0 included). Same cost and thread rules as {@link #isBeamActive}.
     */
    public static List<Beam> getBeams(Entity entity) {
        if (entity == null || !BeamRegistry.INSTANCE.mayEmit(entity)) return List.of();
        List<Beam> out = new ArrayList<>(2);
        BeamRegistry.INSTANCE.collect(entity, 1.0f, out, null);
        return List.copyOf(out);
    }

    /**
     * The beam stored in the stack's {@code minecraft:custom_data} under {@code "beamlights:beam"}; empty when absent
     * or invalid. Works on both sides and does not require Beam Lights on the other side. Cached, cheap to call.
     */
    public static Optional<BeamItemData> getBeam(ItemStack stack) {
        return BeamItems.get(stack);
    }

    /**
     * Stores the beam in the stack's {@code minecraft:custom_data} (other custom data is kept). Works on both sides and
     * does not require Beam Lights on the other side; e.g. {@code setBeam(stack, BeamItemData.of(12))}.
     */
    public static void setBeam(ItemStack stack, BeamItemData beam) {
        BeamItems.set(stack, beam);
    }

    /** Removes the beam data from the stack (other custom data is kept). Works on both sides. */
    public static void clearBeam(ItemStack stack) {
        BeamItems.clear(stack);
    }
}
