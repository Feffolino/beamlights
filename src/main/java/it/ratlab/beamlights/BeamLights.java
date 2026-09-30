package it.ratlab.beamlights;

import com.mojang.logging.LogUtils;
import it.ratlab.beamlights.config.BeamClientConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(BeamLights.MOD_ID)
public final class BeamLights {
    public static final String MOD_ID = "beamlights";
    public static final Logger LOG = LogUtils.getLogger();

    public BeamLights(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, BeamClientConfig.SPEC);
    }
}
