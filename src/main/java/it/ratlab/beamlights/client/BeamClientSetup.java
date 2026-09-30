package it.ratlab.beamlights.client;

import com.mojang.blaze3d.platform.InputConstants;
import it.ratlab.beamlights.BeamLights;
import it.ratlab.beamlights.compat.omega.OmegaCompat;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** Client setup on the mod bus: optional integrations, key mappings. */
@EventBusSubscriber(modid = BeamLights.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BeamClientSetup {
    public static final KeyMapping TOGGLE_DEBUG_RENDER = new KeyMapping(
            "key.beamlights.toggle_debug_render", InputConstants.UNKNOWN.getValue(), "key.categories.beamlights");

    private BeamClientSetup() {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (ModList.get().isLoaded("omegaflashlight")) OmegaCompat.register();
        });
    }

    @SubscribeEvent
    static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_DEBUG_RENDER);
    }
}
