package com.toroidalworld.platform;

import java.util.function.IntFunction;

import com.toroidalworld.net.WrappingSettingsPayload;
import com.toroidalworld.shape.FlatShape;

import io.netty.buffer.Unpooled;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.internal.versions.neoforge.NeoForgeVersion;
import net.neoforged.neoforge.network.PacketDistributor;

public final class NeoForgePlatform implements Platform {
    private final ModContainer modContainer;

    public NeoForgePlatform(ModContainer modContainer) {
        this.modContainer = modContainer;
    }

    @Override
    public boolean isClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    @Override
    public String modVersion() {
        return modContainer.getModInfo().getVersion().toString();
    }

    @Override
    public String loaderName() {
        return "neoforge";
    }

    @Override
    public String loaderVersion() {
        return NeoForgeVersion.getVersion();
    }

    @Override
    public void sendWorldShape(ServerPlayer player, FlatShape shape) {
        if (player.connection.hasChannel(WrappingSettingsPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, new WrappingSettingsPayload(shape));
        }
    }

    @Override
    public IntFunction<RegistryFriendlyByteBuf> packetBuffers(ServerPlayer player) {
        return capacity -> new RegistryFriendlyByteBuf(
                Unpooled.buffer(capacity), player.registryAccess(), player.connection.getConnectionType());
    }
}
