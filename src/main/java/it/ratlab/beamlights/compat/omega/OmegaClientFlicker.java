package it.ratlab.beamlights.compat.omega;

import com.omega.flashlight.client.FlashlightFlicker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** Client-only: flicker of the local player's flashlight. Only class-loaded from client-side code paths. */
final class OmegaClientFlicker {
    private OmegaClientFlicker() {
    }

    /** Flicker is a local visual effect; other players' lights stay steady. */
    static float localFlicker(Player player) {
        float raw = player == Minecraft.getInstance().player ? FlashlightFlicker.value() : 1.0f;
        // Quantized to two states: every luminance change forces chunk rebuilds.
        return raw < 0.5f ? 0.3f : 1.0f;
    }
}
