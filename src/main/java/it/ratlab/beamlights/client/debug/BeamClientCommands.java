package it.ratlab.beamlights.client.debug;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import it.ratlab.beamlights.BeamLights;
import it.ratlab.beamlights.client.BeamClientTicker;
import it.ratlab.beamlights.client.LightBackend;
import it.ratlab.beamlights.config.BeamClientConfig;
import it.ratlab.beamlights.core.BeamRegistry;
import it.ratlab.beamlights.data.BeamDefinitions;
import it.ratlab.beamlights.server.BeamGameRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.function.Consumer;

/**
 * Client command /beamlights: status, reload, debug, layout and smoothing editing (client config, saved at once) and
 * the server game
 * rules. spawns|attract are forwarded to the vanilla /gamerule command, so the server checks op permission.
 */
@EventBusSubscriber(modid = BeamLights.MOD_ID, value = Dist.CLIENT)
public final class BeamClientCommands {
    private BeamClientCommands() {
    }

    @SubscribeEvent
    static void onRegister(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("beamlights")
                .executes(BeamClientCommands::status)
                .then(Commands.literal("status").executes(BeamClientCommands::status))
                // Quick A/B toggle: off clears every light on the next tick (BeamClientTicker).
                .then(Commands.literal("on").executes(c -> saved(c, BeamClientConfig.ENABLED, true, "enabled")))
                .then(Commands.literal("off").executes(c -> saved(c, BeamClientConfig.ENABLED, false, "enabled")))
                .then(Commands.literal("reload").executes(c -> {
                    BeamClientTicker.resetBackend();
                    reply(c, "Backend reset: " + BeamClientTicker.backend().name());
                    return 1;
                }))
                .then(gamerule("spawns", BeamGameRules.BLOCK_SPAWNS_NAME))
                .then(gamerule("attract", BeamGameRules.ATTRACT_MOBS_NAME))
                .then(LayoutCommands.build())
                .then(PerfCommands.build())
                .then(smoothing())
                .then(Commands.literal("debug")
                        .then(toggle("overlay", DebugState::setOverlay))
                        .then(toggle("render", DebugState::setRender))
                        .then(Commands.literal("dump")
                                .executes(c -> dump(c, DUMP_DELAY_TICKS))
                                .then(Commands.argument("ticks", IntegerArgumentType.integer(0, 200))
                                        .executes(c -> dump(c, IntegerArgumentType.getInteger(c, "ticks")))))));
    }

    // The view is frozen while the chat is open: dump a little later so the beam can be moving.
    private static final int DUMP_DELAY_TICKS = 40;

    private static int dump(CommandContext<CommandSourceStack> c, int delay) {
        if (!BeamClientConfig.ENABLED.get() || Minecraft.getInstance().level == null) {
            reply(c, "Beam Lights is disabled or no world loaded, nothing to dump");
            return 1;
        }
        DebugState.requestDump(delay);
        reply(c, delay == 0 ? "Dump requested, see latest.log"
                : "Dump in " + delay + " ticks (move the beam now), see latest.log");
        return 1;
    }

    // Forwarded as a normal command through the connection; the server replies with the result or a permission error.
    private static LiteralArgumentBuilder<CommandSourceStack> gamerule(String name, String rule) {
        return Commands.literal(name)
                .executes(c -> sendServerCommand(c, "gamerule " + rule))
                .then(Commands.literal("on").executes(c -> sendServerCommand(c, "gamerule " + rule + " true")))
                .then(Commands.literal("off").executes(c -> sendServerCommand(c, "gamerule " + rule + " false")));
    }

    private static int sendServerCommand(CommandContext<CommandSourceStack> c, String command) {
        ClientPacketListener conn = Minecraft.getInstance().getConnection();
        if (conn == null) {
            reply(c, "Not connected to a world");
            return 0;
        }
        conn.sendCommand(command);
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> toggle(String name, Consumer<Boolean> setter) {
        return Commands.literal(name)
                .then(Commands.literal("on").executes(c -> {
                    setter.accept(true);
                    reply(c, "Debug " + name + " on");
                    return 1;
                }))
                .then(Commands.literal("off").executes(c -> {
                    setter.accept(false);
                    reply(c, "Debug " + name + " off");
                    return 1;
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> smoothing() {
        return Commands.literal("smoothing")
                .executes(c -> {
                    reply(c, "Smoothing: " + smoothingLine());
                    return 1;
                })
                .then(Commands.literal("on").executes(c -> saved(c, BeamClientConfig.SMOOTHING, true, "smoothing")))
                .then(Commands.literal("off").executes(c -> saved(c, BeamClientConfig.SMOOTHING, false, "smoothing")))
                .then(Commands.literal("factor")
                        .executes(c -> current(c, "smoothFactor", BeamClientConfig.SMOOTH_FACTOR.get()))
                        .then(Commands.argument("v", DoubleArgumentType.doubleArg(0.1, 1.0))
                                .executes(c -> saved(c, BeamClientConfig.SMOOTH_FACTOR,
                                        DoubleArgumentType.getDouble(c, "v"), "smoothFactor"))))
                .then(Commands.literal("jump")
                        .executes(c -> current(c, "jumpDistance", BeamClientConfig.JUMP_DISTANCE.get()))
                        .then(Commands.argument("v", DoubleArgumentType.doubleArg(1.0, 16.0))
                                .executes(c -> saved(c, BeamClientConfig.JUMP_DISTANCE,
                                        DoubleArgumentType.getDouble(c, "v"), "jumpDistance"))))
                .then(Commands.literal("fade")
                        .executes(c -> current(c, "fadeTicks", BeamClientConfig.FADE_TICKS.get()))
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 20))
                                .executes(c -> saved(c, BeamClientConfig.FADE_TICKS,
                                        IntegerArgumentType.getInteger(c, "ticks"), "fadeTicks"))))
                .then(Commands.literal("steps")
                        .executes(c -> current(c, "fadeSteps", BeamClientConfig.FADE_STEPS.get()))
                        .then(Commands.argument("n", IntegerArgumentType.integer(0, 20))
                                .executes(c -> saved(c, BeamClientConfig.FADE_STEPS,
                                        IntegerArgumentType.getInteger(c, "n"), "fadeSteps"))))
                .then(Commands.literal("glide")
                        .executes(c -> current(c, "glideMinTicks", BeamClientConfig.GLIDE_MIN_TICKS.get()))
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 20))
                                .executes(c -> saved(c, BeamClientConfig.GLIDE_MIN_TICKS,
                                        IntegerArgumentType.getInteger(c, "ticks"), "glideMinTicks"))));
    }

    // set() also updates the cached value, save() writes beamlights-client.toml; applied on the next tick.
    private static <T> int saved(CommandContext<CommandSourceStack> c, ModConfigSpec.ConfigValue<T> v, T value,
                                 String key) {
        v.set(value);
        v.save();
        reply(c, key + " = " + value + " (saved)");
        return 1;
    }

    /** Prints the current value of a key (subcommand given without a value). */
    static int current(CommandContext<CommandSourceStack> c, String key, Object value) {
        reply(c, key + " = " + value);
        return 1;
    }

    private static String smoothingLine() {
        return (BeamClientConfig.SMOOTHING.get() ? "on" : "off") + ", factor " + BeamClientConfig.SMOOTH_FACTOR.get()
                + ", jump " + BeamClientConfig.JUMP_DISTANCE.get() + " blocks, fade "
                + BeamClientConfig.FADE_TICKS.get() + " ticks in " + BeamClientConfig.FADE_STEPS.get()
                + " steps, glide every " + BeamClientConfig.GLIDE_MIN_TICKS.get() + " ticks";
    }

    private static int status(CommandContext<CommandSourceStack> c) {
        LightBackend b = BeamClientTicker.backend();
        reply(c, "Enabled: " + (BeamClientConfig.ENABLED.get() ? "on" : "off") + " (/beamlights on|off)");
        reply(c, "Backend: " + b.name() + " (" + b.statusLine() + ")");
        reply(c, "Sources: " + b.ownCount() + " own / " + (b.totalCount() < 0 ? "?" : b.totalCount()) + " engine total");
        reply(c, "Layout: " + BeamClientTicker.layoutLine() + " (/beamlights layout show)");
        reply(c, "Smoothing: " + smoothingLine() + " (/beamlights smoothing ...)");
        reply(c, "Performance: " + PerfCommands.settingsLine() + " (/beamlights perf ...)");
        reply(c, "Server game rules: " + BeamGameRules.BLOCK_SPAWNS_NAME + ", " + BeamGameRules.ATTRACT_MOBS_NAME
                + " (/beamlights spawns|attract [on|off])");
        reply(c, "Providers: " + BeamRegistry.INSTANCE.providerNames()
                + ", Omega Flashlight loaded: " + ModList.get().isLoaded("omegaflashlight"));
        reply(c, "Data beam definitions: " + BeamDefinitions.CLIENT.size() + " (synced from the server"
                + (BeamDefinitions.CLIENT.usesCurios() ? ", Curios slots used" : "") + ")");
        return 1;
    }

    static void reply(CommandContext<CommandSourceStack> c, String msg) {
        c.getSource().sendSuccess(() -> Component.literal("[Beam Lights] " + msg), false);
    }
}
