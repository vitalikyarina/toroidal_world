package com.toroidalworld.platform;

import java.util.function.IntFunction;

import com.toroidalworld.net.WrappingSettingsPayload;
import com.toroidalworld.shape.FlatShape;

import io.netty.buffer.Unpooled;

import com.toroidalworld.ToroidalWorld;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public final class FabricPlatform implements Platform {
    private static final String LOADER_MOD_ID = "fabricloader";

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public String modVersion() {
        return versionOf(ToroidalWorld.MODID);
    }

    @Override
    public String loaderName() {
        return "fabric";
    }

    @Override
    public String loaderVersion() {
        return versionOf(LOADER_MOD_ID);
    }

    private static String versionOf(String modId) {
        return FabricLoader.getInstance().getModContainer(modId).orElseThrow()
                .getMetadata().getVersion().getFriendlyString();
    }

    @Override
    public void sendWorldShape(ServerPlayer player, FlatShape shape) {
        if (ServerPlayNetworking.canSend(player, WrappingSettingsPayload.TYPE)) {
            ServerPlayNetworking.send(player, new WrappingSettingsPayload(shape));
        }
    }

    @Override
    public IntFunction<RegistryFriendlyByteBuf> packetBuffers(ServerPlayer player) {
        return capacity -> new RegistryFriendlyByteBuf(Unpooled.buffer(capacity), player.registryAccess());
    }
}
