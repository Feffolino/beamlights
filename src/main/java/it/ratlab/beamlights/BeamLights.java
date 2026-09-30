package it.ratlab.beamlights;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(BeamLights.MOD_ID)
public final class BeamLights {
    public static final String MOD_ID = "beamlights";
    public static final Logger LOG = LogUtils.getLogger();

    public BeamLights(IEventBus modBus, ModContainer container) {
    }
}
