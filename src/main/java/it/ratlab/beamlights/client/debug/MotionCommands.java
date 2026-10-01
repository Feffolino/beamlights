package it.ratlab.beamlights.client.debug;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import it.ratlab.beamlights.client.BeamClientTicker;
import it.ratlab.beamlights.config.BeamClientConfig;
import it.ratlab.beamlights.core.MotionGovernor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.Locale;

/** /beamlights motion ...: the [motion] keys of the client config, saved at once, applied on the next tick. */
final class MotionCommands {
    private MotionCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("motion")
                .executes(MotionCommands::show)
                .then(Commands.literal("show").executes(MotionCommands::show))
                .then(doubleKey("fastTurn", "fastTurnDegPerSec", BeamClientConfig.FAST_TURN, 1, 3600))
                .then(doubleKey("slowTurn", "slowTurnDegPerSec", BeamClientConfig.SLOW_TURN, 0, 3600))
                .then(doubleKey("fastMove", "fastMoveBlocksPerSec", BeamClientConfig.FAST_MOVE, 0.5, 200))
                .then(intKey("sideTicks", "sideUpdateTicks", BeamClientConfig.SIDE_UPDATE_TICKS, 1, 40))
                .then(intKey("settle", "settleTicks", BeamClientConfig.SETTLE_TICKS, 1, 40))
                .then(Commands.literal("mode")
                        .executes(c -> BeamClientCommands.current(c, "fastSideMode",
                                BeamClientConfig.FAST_SIDE_MODE.get()))
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(MotionGovernor.SideMode.values()).map(Enum::name), b))
                                .executes(MotionCommands::mode)));
    }

    static String settingsLine() {
        return "fastTurnDegPerSec " + BeamClientConfig.FAST_TURN.get() + ", slowTurnDegPerSec "
                + BeamClientConfig.SLOW_TURN.get() + ", fastMoveBlocksPerSec " + BeamClientConfig.FAST_MOVE.get()
                + ", sideUpdateTicks " + BeamClientConfig.SIDE_UPDATE_TICKS.get() + ", settleTicks "
                + BeamClientConfig.SETTLE_TICKS.get() + ", fastSideMode " + BeamClientConfig.FAST_SIDE_MODE.get();
    }

    /** Preset values (the side mode is left alone). Caller saves. */
    static void apply(double fastTurn, double slowTurn, double fastMove, int sideTicks, int settle) {
        BeamClientConfig.FAST_TURN.set(fastTurn);
        BeamClientConfig.SLOW_TURN.set(slowTurn);
        BeamClientConfig.FAST_MOVE.set(fastMove);
        BeamClientConfig.SIDE_UPDATE_TICKS.set(sideTicks);
        BeamClientConfig.SETTLE_TICKS.set(settle);
    }

    private static int show(CommandContext<CommandSourceStack> c) {
        BeamClientCommands.reply(c, "Motion: " + settingsLine());
        BeamClientCommands.reply(c, BeamClientTicker.STATS.motionLine());
        return 1;
    }

    private static int mode(CommandContext<CommandSourceStack> c) {
        String name = StringArgumentType.getString(c, "mode").toUpperCase(Locale.ROOT);
        MotionGovernor.SideMode m;
        try {
            m = MotionGovernor.SideMode.valueOf(name);
        } catch (IllegalArgumentException e) {
            BeamClientCommands.reply(c, "Unknown mode " + name + ", one of "
                    + Arrays.toString(MotionGovernor.SideMode.values()));
            return 0;
        }
        return saved(c, BeamClientConfig.FAST_SIDE_MODE, m, "fastSideMode");
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

    // set() also updates the cached value, save() writes beamlights-client.toml.
    private static <T> int saved(CommandContext<CommandSourceStack> c, ModConfigSpec.ConfigValue<T> v, T value,
                                 String key) {
        v.set(value);
        v.save();
        BeamClientCommands.reply(c, key + " = " + value + " (saved)");
        return 1;
    }
}
