package it.ratlab.beamlights.client.debug;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import it.ratlab.beamlights.client.mask.GammaMask;
import it.ratlab.beamlights.config.BeamClientConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * /beamlights mask ...: the [mask] keys of the client config, saved at once, applied on the next frame. Without a value
 * a key shows its current value; /beamlights mask shows the state and all keys.
 */
final class MaskCommands {
    private MaskCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("mask")
                .executes(MaskCommands::show)
                .then(Commands.literal("show").executes(MaskCommands::show))
                .then(ConeCommands.boolKey("enabled", "gammaMask", BeamClientConfig.GAMMA_MASK))
                .then(ConeCommands.doubleKey("strength", "maskStrength", BeamClientConfig.MASK_STRENGTH, 0, 3))
                .then(ConeCommands.doubleKey("nightVision", "maskNightVision", BeamClientConfig.MASK_NIGHT_VISION, 0, 3))
                .then(ConeCommands.doubleKey("shading", "maskShading", BeamClientConfig.MASK_SHADING, 0, 1))
                .then(ConeCommands.doubleKey("knee", "maskKnee", BeamClientConfig.MASK_KNEE, 0.3, 1))
                .then(ConeCommands.doubleKey("brightCutoff", "maskBrightCutoff", BeamClientConfig.MASK_BRIGHT_CUTOFF,
                        0.1, 1))
                .then(ConeCommands.doubleKey("ambientFade", "maskAmbientFade", BeamClientConfig.MASK_AMBIENT_FADE,
                        0, 1))
                .then(ConeCommands.doubleKey("blackLift", "maskBlackLift", BeamClientConfig.MASK_BLACK_LIFT, 0, 0.2))
                .then(ConeCommands.doubleKey("angle", "maskAngleScale", BeamClientConfig.MASK_ANGLE_SCALE, 0.2, 3))
                .then(ConeCommands.doubleKey("softness", "maskSoftness", BeamClientConfig.MASK_SOFTNESS, 0, 1))
                .then(ConeCommands.doubleKey("range", "maskRangeScale", BeamClientConfig.MASK_RANGE_SCALE, 0.1, 2))
                .then(ConeCommands.doubleKey("falloff", "maskFalloff", BeamClientConfig.MASK_FALLOFF, 0, 4))
                .then(ConeCommands.doubleKey("tint", "maskTint", BeamClientConfig.MASK_TINT, 0, 1))
                .then(ConeCommands.doubleKey("fade", "maskFadeSeconds", BeamClientConfig.MASK_FADE_SECONDS, 0, 2))
                .then(ConeCommands.boolKey("midpoints", "maskMidpoints", BeamClientConfig.MASK_MIDPOINTS));
    }

    static String settingsLine() {
        return "gammaMask " + BeamClientConfig.GAMMA_MASK.get() + ", maskStrength "
                + BeamClientConfig.MASK_STRENGTH.get() + ", maskNightVision " + BeamClientConfig.MASK_NIGHT_VISION.get()
                + ", maskShading " + BeamClientConfig.MASK_SHADING.get() + ", maskKnee " + BeamClientConfig.MASK_KNEE.get()
                + ", maskBrightCutoff " + BeamClientConfig.MASK_BRIGHT_CUTOFF.get() + ", maskAmbientFade "
                + BeamClientConfig.MASK_AMBIENT_FADE.get() + ", maskBlackLift " + BeamClientConfig.MASK_BLACK_LIFT.get()
                + ", maskAngleScale "
                + BeamClientConfig.MASK_ANGLE_SCALE.get() + ", maskSoftness " + BeamClientConfig.MASK_SOFTNESS.get()
                + ", maskRangeScale " + BeamClientConfig.MASK_RANGE_SCALE.get() + ", maskFalloff "
                + BeamClientConfig.MASK_FALLOFF.get() + ", maskTint " + BeamClientConfig.MASK_TINT.get()
                + ", maskFadeSeconds " + BeamClientConfig.MASK_FADE_SECONDS.get() + ", maskMidpoints "
                + BeamClientConfig.MASK_MIDPOINTS.get();
    }

    private static int show(CommandContext<CommandSourceStack> c) {
        BeamClientCommands.reply(c, GammaMask.statusLine());
        BeamClientCommands.reply(c, "Mask: " + settingsLine());
        return 1;
    }
}
