package it.ratlab.beamlights;

import com.mojang.logging.LogUtils;
import it.ratlab.beamlights.compat.omega.OmegaCompat;
import it.ratlab.beamlights.config.BeamClientConfig;
import it.ratlab.beamlights.config.BeamServerConfig;
import it.ratlab.beamlights.server.BeamGameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(BeamLights.MOD_ID)
public final class BeamLights {
    public static final String MOD_ID = "beamlights";
    public static final Logger LOG = LogUtils.getLogger();

    public BeamLights(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, BeamClientConfig.SPEC);
        container.registerConfig(ModConfig.Type.SERVER, BeamServerConfig.SPEC);
        modBus.addListener(BeamLights::onCommonSetup);
    }

    // Providers are registered on both sides: the client lights beams, the server blocks spawns inside them.
    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BeamGameRules.register();
            if (ModList.get().isLoaded("omegaflashlight")) OmegaCompat.register();
        });
    }
}
