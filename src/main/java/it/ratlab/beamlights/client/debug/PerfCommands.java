package it.ratlab.beamlights.client.debug;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import it.ratlab.beamlights.client.BeamClientTicker;
import it.ratlab.beamlights.config.BeamClientConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Locale;

/** /beamlights perf ...: the [performance] keys of the client config, saved at once, applied on the next tick. */
final class PerfCommands {
    private static final List<String> PRESETS = List.of("quality", "balanced", "performance");

    private PerfCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("perf")
                .executes(PerfCommands::show)
                .then(Commands.literal("show").executes(PerfCommands::show))
                .then(Commands.literal("snap")
                        .executes(c -> cur(c, "snapToBlock", BeamClientConfig.SNAP_TO_BLOCK))
                        .then(Commands.argument("on", BoolArgumentType.bool())
                                .executes(c -> saved(c, BeamClientConfig.SNAP_TO_BLOCK,
                                        BoolArgumentType.getBool(c, "on"), "snapToBlock"))))
                .then(Commands.literal("budget")
                        .executes(c -> cur(c, "maxMovesPerTick", BeamClientConfig.MAX_MOVES_PER_TICK))
                        .then(Commands.argument("n", IntegerArgumentType.integer(0, 512))
                                .executes(c -> saved(c, BeamClientConfig.MAX_MOVES_PER_TICK,
                                        IntegerArgumentType.getInteger(c, "n"), "maxMovesPerTick"))))
                .then(Commands.literal("interval")
                        .executes(c -> cur(c, "remoteUpdateInterval", BeamClientConfig.REMOTE_UPDATE_INTERVAL))
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 20))
                                .executes(c -> saved(c, BeamClientConfig.REMOTE_UPDATE_INTERVAL,
                                        IntegerArgumentType.getInteger(c, "ticks"), "remoteUpdateInterval"))))
                .then(Commands.literal("lod")
                        .executes(c -> cur(c, "lodDistance", BeamClientConfig.LOD_DISTANCE))
                        .then(Commands.argument("blocks", IntegerArgumentType.integer(0, 256))
                                .executes(c -> saved(c, BeamClientConfig.LOD_DISTANCE,
                                        IntegerArgumentType.getInteger(c, "blocks"), "lodDistance"))))
                .then(Commands.literal("sectionmerge")
                        .executes(c -> cur(c, "mergeSameSection", BeamClientConfig.MERGE_SAME_SECTION))
                        .then(Commands.argument("on", BoolArgumentType.bool())
                                .executes(c -> saved(c, BeamClientConfig.MERGE_SAME_SECTION,
                                        BoolArgumentType.getBool(c, "on"), "mergeSameSection"))))
                .then(Commands.literal("hysteresis")
                        .executes(c -> cur(c, "moveHysteresis", BeamClientConfig.MOVE_HYSTERESIS))
                        .then(Commands.argument("blocks", DoubleArgumentType.doubleArg(0, 8))
                                .executes(c -> saved(c, BeamClientConfig.MOVE_HYSTERESIS,
                                        DoubleArgumentType.getDouble(c, "blocks"), "moveHysteresis"))))
                .then(Commands.literal("centralhysteresis")
                        .executes(c -> cur(c, "centralHysteresis", BeamClientConfig.CENTRAL_HYSTERESIS))
                        .then(Commands.argument("blocks", DoubleArgumentType.doubleArg(0, 8))
                                .executes(c -> saved(c, BeamClientConfig.CENTRAL_HYSTERESIS,
                                        DoubleArgumentType.getDouble(c, "blocks"), "centralHysteresis"))))
                .then(Commands.literal("lumhysteresis")
                        .executes(c -> cur(c, "luminanceHysteresis", BeamClientConfig.LUMINANCE_HYSTERESIS))
                        .then(Commands.argument("levels", IntegerArgumentType.integer(0, 4))
                                .executes(c -> saved(c, BeamClientConfig.LUMINANCE_HYSTERESIS,
                                        IntegerArgumentType.getInteger(c, "levels"), "luminanceHysteresis"))))
                .then(Commands.literal("preset")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(PRESETS, b))
                                .executes(PerfCommands::preset)));
    }

    static String settingsLine() {
        int budget = BeamClientConfig.MAX_MOVES_PER_TICK.get();
        int lod = BeamClientConfig.LOD_DISTANCE.get();
        return "snapToBlock " + BeamClientConfig.SNAP_TO_BLOCK.get() + ", maxMovesPerTick "
                + (budget <= 0 ? "unlimited" : budget) + ", remoteUpdateInterval "
                + BeamClientConfig.REMOTE_UPDATE_INTERVAL.get() + ", lodDistance " + (lod <= 0 ? "off" : lod)
                + ", mergeSameSection " + BeamClientConfig.MERGE_SAME_SECTION.get() + ", midSpacing "
                + BeamClientConfig.MID_SPACING.get() + ", moveHysteresis " + BeamClientConfig.MOVE_HYSTERESIS.get()
                + ", centralHysteresis " + BeamClientConfig.CENTRAL_HYSTERESIS.get() + ", luminanceHysteresis "
                + BeamClientConfig.LUMINANCE_HYSTERESIS.get() + ", fadeSteps " + BeamClientConfig.FADE_STEPS.get()
                + ", glideMinTicks " + BeamClientConfig.GLIDE_MIN_TICKS.get();
    }

    private static int show(CommandContext<CommandSourceStack> c) {
        BeamClientCommands.reply(c, "Performance: " + settingsLine());
        BeamClientCommands.reply(c, BeamClientTicker.STATS.perfLine());
        return 1;
    }

    // quality: no snapping, large budget, every tick, little hysteresis; balanced: the defaults; performance: fewer,
    // steadier lights.
    private static int preset(CommandContext<CommandSourceStack> c) {
        String name = StringArgumentType.getString(c, "name").toLowerCase(Locale.ROOT);
        switch (name) {
            case "quality" -> apply(false, 32, 1, 48, 6.0, 0.5, 0.25, 0, 4, 1);
            case "balanced" -> apply(true, 12, 2, 24, 6.0, 1.5, 0.75, 1, 2, 2);
            case "performance" -> apply(true, 6, 4, 16, 8.0, 2.0, 1.0, 1, 2, 3);
            default -> {
                BeamClientCommands.reply(c, "Unknown preset " + name + ", one of " + PRESETS);
                return 0;
            }
        }
        BeamClientConfig.SPEC.save();
        BeamClientCommands.reply(c, "Preset " + name + " applied (saved): " + settingsLine());
        return 1;
    }

    private static void apply(boolean snap, int budget, int interval, int lod, double midSpacing, double hysteresis,
                              double centralHysteresis, int lumHysteresis, int fadeSteps, int glideMinTicks) {
        BeamClientConfig.MOVE_HYSTERESIS.set(hysteresis);
        BeamClientConfig.CENTRAL_HYSTERESIS.set(centralHysteresis);
        BeamClientConfig.LUMINANCE_HYSTERESIS.set(lumHysteresis);
        BeamClientConfig.FADE_STEPS.set(fadeSteps);
        BeamClientConfig.GLIDE_MIN_TICKS.set(glideMinTicks);
        BeamClientConfig.SNAP_TO_BLOCK.set(snap);
        BeamClientConfig.MAX_MOVES_PER_TICK.set(budget);
        BeamClientConfig.REMOTE_UPDATE_INTERVAL.set(interval);
        BeamClientConfig.LOD_DISTANCE.set(lod);
        BeamClientConfig.MID_SPACING.set(midSpacing);
    }

    private static <T> int cur(CommandContext<CommandSourceStack> c, String key, ModConfigSpec.ConfigValue<T> v) {
        return BeamClientCommands.current(c, key, v.get());
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
