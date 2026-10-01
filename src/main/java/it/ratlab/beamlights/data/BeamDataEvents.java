package it.ratlab.beamlights.data;

import it.ratlab.beamlights.BeamLights;
import it.ratlab.beamlights.net.BeamDefinitionsPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Game bus: datapack loading, sync to clients (join and /reload), tag cache invalidation. */
@EventBusSubscriber(modid = BeamLights.MOD_ID)
public final class BeamDataEvents {
    private BeamDataEvents() {
    }

    @SubscribeEvent
    static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new BeamDefinitionLoader());
    }

    // Clients without the mod (or an older one without the channel) are skipped.
    @SubscribeEvent
    static void onDatapackSync(OnDatapackSyncEvent event) {
        BeamDefinitionsPayload payload = new BeamDefinitionsPayload(BeamDefinitions.SERVER.raw());
        event.getRelevantPlayers().forEach((ServerPlayer p) -> {
            if (p.connection.hasChannel(BeamDefinitionsPayload.TYPE)) PacketDistributor.sendToPlayer(p, payload);
        });
    }

    @SubscribeEvent
    static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED) {
            BeamDefinitions.CLIENT.invalidateTags();
        } else {
            BeamDefinitions.SERVER.invalidateTags();
        }
    }
}
