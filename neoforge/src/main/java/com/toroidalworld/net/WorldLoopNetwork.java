package com.toroidalworld.net;

import com.toroidalworld.client.WorldLoopClientNetwork;
import com.toroidalworld.ToroidalWorld;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

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
