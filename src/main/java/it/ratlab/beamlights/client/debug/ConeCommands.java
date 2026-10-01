package it.ratlab.beamlights.client.debug;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import it.ratlab.beamlights.config.BeamClientConfig;
import it.ratlab.beamlights.core.ConePolicy;
import it.ratlab.beamlights.core.RayLayout;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Locale;

/**
 * /beamlights cone ...: the [cone] keys of the client config plus ldlConeLight / ldlConeLuminanceOffset, saved at once,
 * applied on the next tick. Without a value a key shows its current value; /beamlights cone shows all.
 */
final class ConeCommands {
    private static final List<String> PRESETS = List.of("light", "balanced", "wide");

    private ConeCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("cone")
                .executes(ConeCommands::show)
                .then(Commands.literal("show").executes(ConeCommands::show))
                .then(boolKey("enabled", "ldlConeLight", BeamClientConfig.LDL_CONE_LIGHT))
                .then(intKey("luminanceOffset", "ldlConeLuminanceOffset", BeamClientConfig.LDL_CONE_LUMINANCE_OFFSET,
                        -15, 0))
                .then(doubleKey("maxAngle", "coneMaxAngle", BeamClientConfig.CONE_MAX_ANGLE, 5, 60))
                .then(doubleKey("maxLength", "coneMaxLength", BeamClientConfig.CONE_MAX_LENGTH, 4, 48))
                .then(doubleKey("minLength", "coneMinLength", BeamClientConfig.CONE_MIN_LENGTH, 0, 16))
                .then(doubleKey("maxDistance", "coneMaxDistanceForCone", BeamClientConfig.CONE_MAX_DISTANCE, 4, 64))
                .then(doubleKey("endFactor", "coneEndFactor", BeamClientConfig.CONE_END_FACTOR, 0.1, 1))
                .then(doubleKey("edgeSoftness", "coneEdgeSoftness", BeamClientConfig.CONE_EDGE_SOFTNESS, 0, 1))
                .then(boolKey("freezeWhenFast", "coneFreezeWhenFast", BeamClientConfig.CONE_FREEZE_WHEN_FAST))
                .then(doubleKey("lengthHysteresis", "coneLengthHysteresis", BeamClientConfig.CONE_LENGTH_HYSTERESIS,
                        0, 8))
                .then(doubleKey("apexHysteresis", "coneApexHysteresis", BeamClientConfig.CONE_APEX_HYSTERESIS, 0, 8))
                .then(doubleKey("angleHysteresis", "coneAngleHysteresis", BeamClientConfig.CONE_ANGLE_HYSTERESIS,
                        0, 45))
                .then(Commands.literal("openArea")
                        .executes(c -> BeamClientCommands.current(c, "openAreaPattern",
                                BeamClientConfig.OPEN_AREA_PATTERN.get()))
                        .then(Commands.argument("pattern", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        java.util.Arrays.stream(RayLayout.Pattern.values()).map(Enum::name), b))
                                .executes(ConeCommands::openArea)))
                .then(doubleKey("openHysteresis", "openAreaHysteresis", BeamClientConfig.OPEN_AREA_HYSTERESIS, 0, 16))
                .then(intKey("openMinTicks", "openAreaMinTicks", BeamClientConfig.OPEN_AREA_MIN_TICKS, 0, 100))
                .then(boolKey("openSky", "openAreaSky", BeamClientConfig.OPEN_AREA_SKY))
                .then(intKey("openSkyLight", "openAreaSkyLight", BeamClientConfig.OPEN_AREA_SKY_LIGHT, 1, 15))
                .then(intKey("openFade", "openAreaFadeTicks", BeamClientConfig.OPEN_AREA_FADE_TICKS, 0, 40))
                .then(Commands.literal("preset")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(PRESETS, b))
                                .executes(ConeCommands::preset)));
    }

    private static int openArea(CommandContext<CommandSourceStack> c) {
        String name = StringArgumentType.getString(c, "pattern").toUpperCase(Locale.ROOT);
        RayLayout.Pattern p;
        try {
            p = RayLayout.Pattern.valueOf(name);
        } catch (IllegalArgumentException e) {
            BeamClientCommands.reply(c, "Unknown pattern " + name);
            return 0;
        }
        return saved(c, BeamClientConfig.OPEN_AREA_PATTERN, p, "openAreaPattern");
    }

    static String settingsLine() {
        return "ldlConeLight " + BeamClientConfig.LDL_CONE_LIGHT.get() + ", openAreaPattern "
                + BeamClientConfig.OPEN_AREA_PATTERN.get() + ", openAreaHysteresis "
                + BeamClientConfig.OPEN_AREA_HYSTERESIS.get() + ", openAreaMinTicks "
                + BeamClientConfig.OPEN_AREA_MIN_TICKS.get() + ", openAreaSky " + BeamClientConfig.OPEN_AREA_SKY.get()
                + ", openAreaSkyLight " + BeamClientConfig.OPEN_AREA_SKY_LIGHT.get() + ", openAreaFadeTicks "
                + BeamClientConfig.OPEN_AREA_FADE_TICKS.get() + ", ldlConeLuminanceOffset "
                + BeamClientConfig.LDL_CONE_LUMINANCE_OFFSET.get() + ", coneMaxAngle "
                + BeamClientConfig.CONE_MAX_ANGLE.get() + ", coneMaxLength " + BeamClientConfig.CONE_MAX_LENGTH.get()
                + ", coneMinLength " + BeamClientConfig.CONE_MIN_LENGTH.get() + ", coneMaxDistanceForCone "
                + BeamClientConfig.CONE_MAX_DISTANCE.get() + ", coneEndFactor " + BeamClientConfig.CONE_END_FACTOR.get()
                + ", coneEdgeSoftness " + BeamClientConfig.CONE_EDGE_SOFTNESS.get() + ", coneFreezeWhenFast "
                + BeamClientConfig.CONE_FREEZE_WHEN_FAST.get() + ", coneLengthHysteresis "
                + BeamClientConfig.CONE_LENGTH_HYSTERESIS.get() + ", coneApexHysteresis "
                + BeamClientConfig.CONE_APEX_HYSTERESIS.get() + ", coneAngleHysteresis "
                + BeamClientConfig.CONE_ANGLE_HYSTERESIS.get();
    }

    private static int show(CommandContext<CommandSourceStack> c) {
        BeamClientCommands.reply(c, "Cone: " + settingsLine());
        return 1;
    }

    // light: small and steady; balanced: the defaults; wide: a big cone (costs more outdoors). The look keys
    // (end factor, softness) and the luminance keys are left alone.
    private static int preset(CommandContext<CommandSourceStack> c) {
        String name = StringArgumentType.getString(c, "name").toLowerCase(Locale.ROOT);
        ConePolicy.Settings s = switch (name) {
            case "light" -> ConePolicy.Settings.LIGHT;
            case "balanced" -> ConePolicy.Settings.BALANCED;
            case "wide" -> ConePolicy.Settings.WIDE;
            default -> null;
        };
        if (s == null) {
            BeamClientCommands.reply(c, "Unknown preset " + name + ", one of " + PRESETS);
            return 0;
        }
        apply(s);
        BeamClientConfig.SPEC.save();
        BeamClientCommands.reply(c, "Cone preset " + name + " applied (saved): " + settingsLine());
        return 1;
    }

    /** Preset values. Caller saves. */
    static void apply(ConePolicy.Settings s) {
        BeamClientConfig.CONE_MAX_ANGLE.set(s.maxAngleDeg());
        BeamClientConfig.CONE_MAX_LENGTH.set(s.maxLength());
        BeamClientConfig.CONE_MIN_LENGTH.set(s.minLength());
        BeamClientConfig.CONE_MAX_DISTANCE.set(s.maxDistance());
        BeamClientConfig.CONE_LENGTH_HYSTERESIS.set(s.lengthHysteresis());
        BeamClientConfig.CONE_APEX_HYSTERESIS.set(s.apexHysteresis());
        BeamClientConfig.CONE_ANGLE_HYSTERESIS.set(s.angleHysteresisDeg());
        BeamClientConfig.CONE_FREEZE_WHEN_FAST.set(s.freezeWhenFast());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> doubleKey(String name, String key,
                                                                        ModConfigSpec.DoubleValue v, double min,
                                                                        double max) {
        return Commands.literal(name)
                .executes(c -> BeamClientCommands.current(c, key, v.get()))
                .then(Commands.argument("value", DoubleArgumentType.doubleArg(min, max))
                        .executes(c -> saved(c, v, DoubleArgumentType.getDouble(c, "value"), key)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> intKey(String name, String key,
                                                                     ModConfigSpec.IntValue v, int min, int max) {
        return Commands.literal(name)
                .executes(c -> BeamClientCommands.current(c, key, v.get()))
                .then(Commands.argument("value", IntegerArgumentType.integer(min, max))
                        .executes(c -> saved(c, v, IntegerArgumentType.getInteger(c, "value"), key)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> boolKey(String name, String key,
                                                                      ModConfigSpec.BooleanValue v) {
        return Commands.literal(name)
                .executes(c -> BeamClientCommands.current(c, key, v.get()))
                .then(Commands.argument("on", BoolArgumentType.bool())
                        .executes(c -> saved(c, v, BoolArgumentType.getBool(c, "on"), key)));
    }

    // set() also updates the cached value, save() writes beamlights-client.toml.
    private static <T> int saved(CommandContext<CommandSourceStack> c, ModConfigSpec.ConfigValue<T> v, T value,
                                 String key) {
        v.set(value);
        v.save();
        BeamClientCommands.reply(c, key + " = " + value + " (saved)");
        return 1;
    }
}
