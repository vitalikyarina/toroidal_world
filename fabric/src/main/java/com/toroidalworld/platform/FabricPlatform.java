package com.toroidalworld.platform;

import java.util.function.IntFunction;

import com.toroidalworld.net.WrappingSettingsPayload;
import com.toroidalworld.options.WorldLoopBounds;

import io.netty.buffer.Unpooled;

import com.toroidalworld.ToroidalWorld;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public final class FabricPlatform implements Platform {
    // The loader publishes itself as a mod, so its version is read the same way the mod's own is.
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

    // orElseThrow on purpose: both containers asked for are the mod's own and the loader's, which cannot be absent in
    // a running game — an empty Optional here is a broken runtime, not a case to soften into a placeholder.
    private static String versionOf(String modId) {
        return FabricLoader.getInstance().getModContainer(modId).orElseThrow()
                .getMetadata().getVersion().getFriendlyString();
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
