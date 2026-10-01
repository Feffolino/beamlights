package it.ratlab.beamlights.net;

import io.netty.buffer.ByteBuf;
import it.ratlab.beamlights.BeamLights;
import it.ratlab.beamlights.data.BeamDefinitions;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

/** Server to client: every valid beam definition as raw JSON per file id; replaces the client copy. */
public record BeamDefinitionsPayload(Map<ResourceLocation, String> files) implements CustomPacketPayload {
    public static final Type<BeamDefinitionsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BeamLights.MOD_ID, "beam_definitions"));
    private static final int MAX_JSON = 1 << 18;

    public static final StreamCodec<ByteBuf, BeamDefinitionsPayload> STREAM_CODEC = ByteBufCodecs
            .<ByteBuf, ResourceLocation, String, Map<ResourceLocation, String>>map(HashMap::new,
                    ResourceLocation.STREAM_CODEC, ByteBufCodecs.stringUtf8(MAX_JSON))
            .map(BeamDefinitionsPayload::new, BeamDefinitionsPayload::files);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void handle(BeamDefinitionsPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> BeamDefinitions.CLIENT.loadRaw(payload.files()));
    }

    @EventBusSubscriber(modid = BeamLights.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        // Optional: either side may lack the mod (light and spawn blocking each work alone).
        @SubscribeEvent
        static void onRegister(RegisterPayloadHandlersEvent event) {
            event.registrar("1").optional().playToClient(TYPE, STREAM_CODEC, BeamDefinitionsPayload::handle);
        }
    }
}
