package it.ratlab.beamlights.client;

import it.ratlab.beamlights.BeamLights;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** Client setup on the mod bus: optional integrations, key mappings. */
@EventBusSubscriber(modid = BeamLights.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BeamClientSetup {
    private BeamClientSetup() {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        BeamLights.LOG.info("Beam Lights: client setup");
    }
}
