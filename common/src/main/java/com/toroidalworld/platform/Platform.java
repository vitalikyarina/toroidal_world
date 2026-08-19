package com.toroidalworld.platform;

import java.util.function.IntFunction;

import com.toroidalworld.options.WorldLoopBounds;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public interface Platform {
    boolean isClient();

    String modVersion();

    String loaderName();

    String loaderVersion();

    void sendWrappingBounds(ServerPlayer player, WorldLoopBounds bounds);

    IntFunction<RegistryFriendlyByteBuf> packetBuffers(ServerPlayer player);
}
