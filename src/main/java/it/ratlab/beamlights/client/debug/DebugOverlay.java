package it.ratlab.beamlights.client.debug;

import it.ratlab.beamlights.BeamLights;
import it.ratlab.beamlights.client.BeamClientTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.List;

/** Debug text: top-left HUD, or appended to the F3 left column while F3 is open. */
@EventBusSubscriber(modid = BeamLights.MOD_ID, value = Dist.CLIENT)
public final class DebugOverlay {
    private DebugOverlay() {
    }

    @SubscribeEvent
    static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        if (!DebugState.overlay()) return;
        event.getLeft().add("");
        event.getLeft().addAll(lines());
    }

    @SubscribeEvent
    static void onGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!DebugState.overlay() || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) return;
        GuiGraphics g = event.getGuiGraphics();
        int y = 4;
        for (String line : lines()) {
            g.drawString(mc.font, line, 4, y, 0xFFFFFF, true);
            y += 10;
        }
    }

    private static List<String> lines() {
        return BeamClientTicker.STATS.lines(BeamClientTicker.backend());
    }
}
