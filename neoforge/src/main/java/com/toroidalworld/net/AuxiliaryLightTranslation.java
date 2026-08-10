package com.toroidalworld.net;

import net.neoforged.neoforge.network.payload.AuxiliaryLightDataPayload;

// NeoForge's auxiliary light data travels beside the chunk and names the same chunk, so it moves with it. Registered
// into the translator's payload table from the loader wiring rather than sitting in the dispatch map: the payload
// class is loader API, and the translator core must stay free of it.
public final class AuxiliaryLightTranslation {
    public static void register() {
        PacketTranslator.registerPayloadRewriter(AuxiliaryLightDataPayload.class, (payload, context) ->
                new AuxiliaryLightDataPayload(
                        context.toClient(payload.pos(), ChunkTraffic.AUX_LIGHT), payload.entries()));
    }

    private AuxiliaryLightTranslation() {
    }
}
