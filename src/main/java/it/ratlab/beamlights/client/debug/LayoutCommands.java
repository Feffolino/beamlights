package it.ratlab.beamlights.client.debug;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import it.ratlab.beamlights.client.BeamClientTicker;
import it.ratlab.beamlights.config.BeamClientConfig;
import it.ratlab.beamlights.core.RayLayout;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * /beamlights layout ...: edits the ray layout keys of the client config. Values are saved to the file at once; the
 * ticker compares the layout keys every tick, so changes apply on the next frame.
 */
final class LayoutCommands {
    private static final List<String> PRESETS = List.of("default", "wide", "performance", "cliff", "floodlight");

    private LayoutCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("layout")
                .executes(LayoutCommands::show)
                .then(Commands.literal("show").executes(LayoutCommands::show))
                .then(Commands.literal("pattern")
                        .then(Commands.argument("pattern", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(RayLayout.Pattern.values()).map(Enum::name), b))
                                .executes(LayoutCommands::pattern)))
                .then(Commands.literal("rays")
                        .then(Commands.argument("n", IntegerArgumentType.integer(1, RayLayout.MAX_SIDE_RAYS))
                                .executes(c -> setInt(c, BeamClientConfig.SIDE_RAYS, "n", "sideRays"))))
                .then(Commands.literal("spread")
                        .then(Commands.argument("f", DoubleArgumentType.doubleArg(0, 1))
                                .executes(c -> setDouble(c, BeamClientConfig.CONE_SPREAD, "f", "coneSpread"))))
                .then(Commands.literal("roll")
                        .then(Commands.argument("deg", DoubleArgumentType.doubleArg(0, 360))
                                .executes(c -> setDouble(c, BeamClientConfig.RAY_ROLL_OFFSET, "deg", "rayRollOffset"))))
                .then(Commands.literal("inner")
                        .then(Commands.argument("n", IntegerArgumentType.integer(1, 12))
                                .then(Commands.argument("f", DoubleArgumentType.doubleArg(0, 1))
                                        .executes(LayoutCommands::inner))))
                .then(Commands.literal("range")
                        .then(Commands.argument("f", DoubleArgumentType.doubleArg(0.1, 1))
                                .executes(c -> setDouble(c, BeamClientConfig.SIDE_RANGE_FACTOR, "f", "sideRangeFactor"))))
                .then(Commands.literal("budget")
                        .then(Commands.argument("n", IntegerArgumentType.integer(1, 64))
                                .executes(c -> setInt(c, BeamClientConfig.MAX_SOURCES_PER_ENTITY, "n",
                                        "maxSourcesPerEntity"))))
                .then(Commands.literal("luminance")
                        .then(Commands.argument("offset", IntegerArgumentType.integer(-15, 0))
                                .executes(c -> setInt(c, BeamClientConfig.SIDE_LUMINANCE_OFFSET, "offset",
                                        "sideLuminanceOffset"))))
                .then(Commands.literal("midpoints")
                        .then(Commands.argument("on", BoolArgumentType.bool())
                                .executes(c -> {
                                    boolean v = BoolArgumentType.getBool(c, "on");
                                    save(BeamClientConfig.SIDE_MIDPOINTS, v);
                                    return done(c, "sideMidpoints = " + v);
                                })))
                .then(Commands.literal("custom")
                        .then(Commands.literal("add")
                                .then(Commands.argument("entry", StringArgumentType.greedyString())
                                        .executes(LayoutCommands::customAdd)))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                        .executes(LayoutCommands::customRemove)))
                        .then(Commands.literal("clear").executes(c -> {
                            save(BeamClientConfig.CUSTOM_RAYS, new ArrayList<String>());
                            return done(c, "customRays cleared");
                        }))
                        .then(Commands.literal("list").executes(LayoutCommands::customList)))
                .then(Commands.literal("preset")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(PRESETS, b))
                                .executes(LayoutCommands::preset)));
    }

    private static int show(CommandContext<CommandSourceStack> c) {
        reply(c, "Pattern " + BeamClientConfig.RAY_PATTERN.get() + ", sideRays " + BeamClientConfig.SIDE_RAYS.get()
                + ", spread " + BeamClientConfig.CONE_SPREAD.get() + ", roll " + BeamClientConfig.RAY_ROLL_OFFSET.get());
        reply(c, "Inner " + BeamClientConfig.INNER_RAYS.get() + " rays at " + BeamClientConfig.INNER_SPREAD.get()
                + ", sideRangeFactor " + BeamClientConfig.SIDE_RANGE_FACTOR.get()
                + ", maxSourcesPerEntity " + BeamClientConfig.MAX_SOURCES_PER_ENTITY.get());
        reply(c, "sideLuminanceOffset " + BeamClientConfig.SIDE_LUMINANCE_OFFSET.get()
                + ", sideMidpoints " + BeamClientConfig.SIDE_MIDPOINTS.get());
        customList(c);
        reply(c, "Active: " + BeamClientTicker.layoutLine());
        return 1;
    }

    private static int pattern(CommandContext<CommandSourceStack> c) {
        String name = StringArgumentType.getString(c, "pattern").toUpperCase(Locale.ROOT);
        RayLayout.Pattern p;
        try {
            p = RayLayout.Pattern.valueOf(name);
        } catch (IllegalArgumentException e) {
            reply(c, "Unknown pattern " + name + ", one of " + Arrays.toString(RayLayout.Pattern.values()));
            return 0;
        }
        save(BeamClientConfig.RAY_PATTERN, p);
        return done(c, "rayPattern = " + p);
    }

    private static int inner(CommandContext<CommandSourceStack> c) {
        int n = IntegerArgumentType.getInteger(c, "n");
        double f = DoubleArgumentType.getDouble(c, "f");
        BeamClientConfig.INNER_RAYS.set(n);
        save(BeamClientConfig.INNER_SPREAD, f);
        return done(c, "innerRays = " + n + ", innerSpread = " + f);
    }

    private static int customAdd(CommandContext<CommandSourceStack> c) {
        String entry = StringArgumentType.getString(c, "entry").trim();
        if (RayLayout.parse(entry, 0, false, 1.0) == null) {
            reply(c, "Invalid entry \"" + entry + "\": expected spread,roll[,lumOffset[,midpoints[,rangeFactor]]]"
                    + " with spread 0..1, lumOffset -15..15, midpoints true|false, rangeFactor 0.1..1");
            return 0;
        }
        List<String> list = new ArrayList<>(BeamClientConfig.CUSTOM_RAYS.get());
        if (list.size() >= RayLayout.MAX_SIDE_RAYS) {
            reply(c, "customRays already has " + list.size() + " entries (max " + RayLayout.MAX_SIDE_RAYS + ")");
            return 0;
        }
        list.add(entry);
        save(BeamClientConfig.CUSTOM_RAYS, list);
        String hint = BeamClientConfig.RAY_PATTERN.get() == RayLayout.Pattern.CUSTOM ? "" : " (used by pattern CUSTOM)";
        return done(c, "customRays[" + (list.size() - 1) + "] = " + entry + hint);
    }

    private static int customRemove(CommandContext<CommandSourceStack> c) {
        int i = IntegerArgumentType.getInteger(c, "index");
        List<String> list = new ArrayList<>(BeamClientConfig.CUSTOM_RAYS.get());
        if (i >= list.size()) {
            reply(c, "No customRays entry " + i + " (" + list.size() + " entries)");
            return 0;
        }
        String removed = list.remove(i);
        save(BeamClientConfig.CUSTOM_RAYS, list);
        return done(c, "Removed customRays[" + i + "] = " + removed + ", " + list.size() + " left");
    }

    private static int customList(CommandContext<CommandSourceStack> c) {
        List<? extends String> list = BeamClientConfig.CUSTOM_RAYS.get();
        if (list.isEmpty()) {
            reply(c, "customRays: empty");
            return 1;
        }
        StringBuilder sb = new StringBuilder("customRays:");
        for (int i = 0; i < list.size(); i++) {
            String e = list.get(i);
            sb.append(' ').append(i).append("=\"").append(e).append('"');
            if (RayLayout.parse(e, 0, false, 1.0) == null) sb.append(" (invalid)");
        }
        reply(c, sb.toString());
        return 1;
    }

    private static int preset(CommandContext<CommandSourceStack> c) {
        String name = StringArgumentType.getString(c, "name").toLowerCase(Locale.ROOT);
        switch (name) {
            case "default" -> {
                BeamClientConfig.RAY_PATTERN.set(RayLayout.Pattern.TRIANGLE);
                BeamClientConfig.CONE_SPREAD.set(0.6);
                BeamClientConfig.RAY_ROLL_OFFSET.set(270.0);
            }
            case "wide" -> {
                BeamClientConfig.RAY_PATTERN.set(RayLayout.Pattern.RING);
                BeamClientConfig.SIDE_RAYS.set(6);
                BeamClientConfig.CONE_SPREAD.set(0.8);
            }
            case "performance" -> BeamClientConfig.RAY_PATTERN.set(RayLayout.Pattern.CENTER_ONLY);
            case "cliff" -> {
                BeamClientConfig.RAY_PATTERN.set(RayLayout.Pattern.FAN_VERTICAL);
                BeamClientConfig.SIDE_RAYS.set(3);
                BeamClientConfig.CONE_SPREAD.set(0.7);
            }
            case "floodlight" -> {
                BeamClientConfig.RAY_PATTERN.set(RayLayout.Pattern.DOUBLE_RING);
                BeamClientConfig.INNER_RAYS.set(3);
                BeamClientConfig.INNER_SPREAD.set(0.35);
                BeamClientConfig.SIDE_RAYS.set(8);
                BeamClientConfig.CONE_SPREAD.set(0.8);
                BeamClientConfig.MAX_SOURCES_PER_ENTITY.set(24);
            }
            default -> {
                reply(c, "Unknown preset " + name + ", one of " + PRESETS);
                return 0;
            }
        }
        BeamClientConfig.SPEC.save();
        return done(c, "Preset " + name + " applied: " + BeamClientTicker.layoutLine());
    }

    private static int setInt(CommandContext<CommandSourceStack> c, ModConfigSpec.IntValue v, String arg, String key) {
        int n = IntegerArgumentType.getInteger(c, arg);
        save(v, n);
        return done(c, key + " = " + n);
    }

    private static int setDouble(CommandContext<CommandSourceStack> c, ModConfigSpec.DoubleValue v, String arg,
                                 String key) {
        double d = DoubleArgumentType.getDouble(c, arg);
        save(v, d);
        return done(c, key + " = " + d);
    }

    // set() also updates the cached value (no restart type), save() writes beamlights-client.toml.
    private static <T> void save(ModConfigSpec.ConfigValue<T> v, T value) {
        v.set(value);
        v.save();
    }

    private static int done(CommandContext<CommandSourceStack> c, String msg) {
        reply(c, msg + " (saved)");
        return 1;
    }

    private static void reply(CommandContext<CommandSourceStack> c, String msg) {
        BeamClientCommands.reply(c, msg);
    }
}
