package com.toroidalworld.platform;

import java.util.function.IntFunction;

import com.toroidalworld.net.WrappingSettingsPayload;
import com.toroidalworld.options.WorldLoopBounds;

import io.netty.buffer.Unpooled;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public final class FabricPlatform implements Platform {
    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    // canSend is the optional-payload guard: a vanilla client never announced the channel, so it is simply not sent to.
    @Override
    public void sendWrappingBounds(ServerPlayer player, WorldLoopBounds bounds) {
        if (ServerPlayNetworking.canSend(player, WrappingSettingsPayload.TYPE)) {
            ServerPlayNetworking.send(player, new WrappingSettingsPayload(bounds));
        }
    }

    @Override
    public IntFunction<RegistryFriendlyByteBuf> packetBuffers(ServerPlayer player) {
        return capacity -> new RegistryFriendlyByteBuf(Unpooled.buffer(capacity), player.registryAccess());
    }
}
