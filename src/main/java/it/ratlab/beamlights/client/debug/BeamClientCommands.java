package it.ratlab.beamlights.client.debug;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import it.ratlab.beamlights.BeamLights;
import it.ratlab.beamlights.client.BeamClientTicker;
import it.ratlab.beamlights.client.LightBackend;
import it.ratlab.beamlights.config.BeamClientConfig;
import it.ratlab.beamlights.core.BeamRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.function.Consumer;

/** Client command /beamlights: status, reload, debug overlay|render on|off, debug dump. */
@EventBusSubscriber(modid = BeamLights.MOD_ID, value = Dist.CLIENT)
public final class BeamClientCommands {
    private BeamClientCommands() {
    }

    @SubscribeEvent
    static void onRegister(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("beamlights")
                .then(Commands.literal("status").executes(BeamClientCommands::status))
                .then(Commands.literal("reload").executes(c -> {
                    BeamClientTicker.resetBackend();
                    reply(c, "Backend reset: " + BeamClientTicker.backend().name());
                    return 1;
                }))
                .then(Commands.literal("debug")
                        .then(toggle("overlay", DebugState::setOverlay))
                        .then(toggle("render", DebugState::setRender))
                        .then(Commands.literal("dump").executes(c -> {
                            if (!BeamClientConfig.ENABLED.get() || Minecraft.getInstance().level == null) {
                                reply(c, "Beam Lights is disabled or no world loaded, nothing to dump");
                                return 1;
                            }
                            DebugState.requestDump();
                            reply(c, "Dump requested, see latest.log");
                            return 1;
                        }))));
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

    private static int status(CommandContext<CommandSourceStack> c) {
        LightBackend b = BeamClientTicker.backend();
        reply(c, "Backend: " + b.name() + " (" + b.statusLine() + ")");
        reply(c, "Sources: " + b.ownCount() + " own / " + (b.totalCount() < 0 ? "?" : b.totalCount()) + " engine total");
        reply(c, "Layout: " + BeamClientTicker.layoutLine());
        reply(c, "Providers: " + BeamRegistry.INSTANCE.providerNames()
                + ", Omega Flashlight loaded: " + ModList.get().isLoaded("omegaflashlight"));
        return 1;
    }

    private static void reply(CommandContext<CommandSourceStack> c, String msg) {
        c.getSource().sendSuccess(() -> Component.literal("[Beam Lights] " + msg), false);
    }
}
