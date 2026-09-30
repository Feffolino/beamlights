package it.ratlab.beamlights.client;

import com.mojang.blaze3d.platform.InputConstants;
import it.ratlab.beamlights.BeamLights;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** Client setup on the mod bus: key mappings (beam providers are registered in common setup, see BeamLights). */
@EventBusSubscriber(modid = BeamLights.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BeamClientSetup {
    public static final KeyMapping TOGGLE_DEBUG_RENDER = new KeyMapping(
            "key.beamlights.toggle_debug_render", InputConstants.UNKNOWN.getValue(), "key.categories.beamlights");

    private BeamClientSetup() {
    }

    @SubscribeEvent
    static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_DEBUG_RENDER);
    }
}
