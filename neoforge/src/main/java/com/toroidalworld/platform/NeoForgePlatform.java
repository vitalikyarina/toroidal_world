package com.toroidalworld.platform;

import java.util.function.IntFunction;

import com.toroidalworld.net.WrappingSettingsPayload;
import com.toroidalworld.options.WorldLoopBounds;

import io.netty.buffer.Unpooled;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.internal.versions.neoforge.NeoForgeVersion;
import net.neoforged.neoforge.network.PacketDistributor;

public final class NeoForgePlatform implements Platform {
    // The entrypoint already holds the mod's own container, and the container is where FML keeps the version it read
    // off the jar — taking it here beats looking the mod up in ModList by its own id.
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

    // hasChannel is the optional-payload guard, mirroring FabricPlatform's canSend: a vanilla client never negotiated
    // the channel, and NeoForge treats sending to one as an error rather than a no-op.
    @Override
    public void sendWrappingBounds(ServerPlayer player, WorldLoopBounds bounds) {
        if (player.connection.hasChannel(WrappingSettingsPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, new WrappingSettingsPayload(bounds));
        }
    }

    @Override
    public IntFunction<RegistryFriendlyByteBuf> packetBuffers(ServerPlayer player) {
        return capacity -> new RegistryFriendlyByteBuf(
                Unpooled.buffer(capacity), player.registryAccess(), player.connection.getConnectionType());
    }
}
