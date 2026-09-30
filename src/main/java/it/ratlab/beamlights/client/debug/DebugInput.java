package it.ratlab.beamlights.client.debug;

import it.ratlab.beamlights.BeamLights;
import it.ratlab.beamlights.client.BeamClientSetup;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Key toggle for the world debug render (unbound by default). */
@EventBusSubscriber(modid = BeamLights.MOD_ID, value = Dist.CLIENT)
public final class DebugInput {
    private DebugInput() {
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        while (BeamClientSetup.TOGGLE_DEBUG_RENDER.consumeClick()) {
            boolean on = !DebugState.render();
            DebugState.setRender(on);
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal("[Beam Lights] debug render " + (on ? "on" : "off")), true);
            }
        }
    }
}
