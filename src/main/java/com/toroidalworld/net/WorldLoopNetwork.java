package com.toroidalworld.net;

import com.toroidalworld.client.WorldLoopClientNetwork;
import com.toroidalworld.ToroidalWorld;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

// Registers the wrap-bounds payload and its client handler — the NeoForge half of the bounds sync; deciding what to
// send and when lives in WrappingBoundsSync, and the send itself in NeoForgePlatform. The handler names a Dist.CLIENT
// class, but only inside the lambda body — which the server registers yet never runs, since the payload is clientbound
// — so the client class is never loaded there.
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

    private WorldLoopNetwork() {
    }
}
