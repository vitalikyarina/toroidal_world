package com.toroidalworld.platform;

import java.util.function.IntFunction;

import com.toroidalworld.config.WorldLoopConfig;
import com.toroidalworld.net.WrappingSettingsPayload;
import com.toroidalworld.options.WorldLoopBounds;

import io.netty.buffer.Unpooled;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;

public final class NeoForgePlatform implements Platform {
    @Override
    public boolean isClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    // hasChannel is the optional-payload guard, mirroring FabricPlatform's canSend: a vanilla client never negotiated
    // the channel, and NeoForge treats sending to one as an error rather than a no-op.
    @Override
    public void sendWrappingBounds(ServerPlayer player, WorldLoopBounds bounds) {
        if (player.connection.hasChannel(WrappingSettingsPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, new WrappingSettingsPayload(bounds));
        }
    }

    @Override
    public boolean showRawF3Coordinates() {
        return WorldLoopConfig.SHOW_RAW_F3_COORDINATES.get();
    }

    @Override
    public IntFunction<RegistryFriendlyByteBuf> packetBuffers(ServerPlayer player) {
        return capacity -> new RegistryFriendlyByteBuf(
                Unpooled.buffer(capacity), player.registryAccess(), player.connection.getConnectionType());
    }
}
