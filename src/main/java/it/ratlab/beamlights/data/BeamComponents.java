package it.ratlab.beamlights.data;

import it.ratlab.beamlights.BeamLights;
import it.ratlab.beamlights.api.BeamItemData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item data components: beamlights:beam (persistent, synced). */
public final class BeamComponents {
    public static final DeferredRegister.DataComponents REGISTER =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, BeamLights.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BeamItemData>> BEAM =
            REGISTER.registerComponentType("beam", b -> b.persistent(BeamItemData.CODEC)
                    .networkSynchronized(BeamItemData.STREAM_CODEC).cacheEncoding());

    private BeamComponents() {
    }
}
