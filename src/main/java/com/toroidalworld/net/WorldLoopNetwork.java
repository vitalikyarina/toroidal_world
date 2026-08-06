package com.toroidalworld.net;

import com.toroidalworld.client.WorldLoopClientNetwork;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.toroidalworld.ToroidalWorld;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

// Registers the wrap-bounds payload and hands out the server side of it. The handler names a Dist.CLIENT class, but only
// inside the lambda body — which the server registers yet never runs, since the payload is clientbound — so the client
// class is never loaded there.
@EventBusSubscriber(modid = ToroidalWorld.MODID)
public final class WorldLoopNetwork {
    private static final String PROTOCOL_VERSION = "1";

    @SubscribeEvent
    static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION).optional().playToClient(
                WrappingSettingsPayload.TYPE,
                WrappingSettingsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> WorldLoopClientNetwork.apply(payload.bounds())));
    }

    // Only when the level actually wraps: an unwrapped one leaves the client's transformer NOOP, which is the vanilla
    // path, and a vanilla client (the payload is optional) simply never receives it. The two wrapped dimensions carry
    // different widths, so this is re-sent on every space change, not only login.
    public static void sendTo(ServerPlayer player) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(player.level());
        if (transformer != null) {
            PacketDistributor.sendToPlayer(player, new WrappingSettingsPayload(transformer.bounds));
        }
    }

    private WorldLoopNetwork() {
    }
}
