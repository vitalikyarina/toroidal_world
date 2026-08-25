package com.toroidalworld.client;

import com.toroidalworld.client.shape.WorldShapeSetup;
import com.toroidalworld.net.WrappingSettingsPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ToroidalWorldFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WorldShapeSetup.registerAll();
        ClientPlayNetworking.registerGlobalReceiver(WrappingSettingsPayload.TYPE,
                (payload, context) -> context.client().execute(() -> WorldLoopClientNetwork.apply(payload.bounds())));
    }
}
