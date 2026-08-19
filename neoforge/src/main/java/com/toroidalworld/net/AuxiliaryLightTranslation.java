package com.toroidalworld.net;

import net.neoforged.neoforge.network.payload.AuxiliaryLightDataPayload;

public final class AuxiliaryLightTranslation {
    public static void register() {
        PacketTranslator.registerPayloadRewriter(AuxiliaryLightDataPayload.class, (payload, context) ->
                new AuxiliaryLightDataPayload(
                        context.toClient(payload.pos(), ChunkTraffic.AUX_LIGHT), payload.entries()));
    }

    private AuxiliaryLightTranslation() {
    }
}
