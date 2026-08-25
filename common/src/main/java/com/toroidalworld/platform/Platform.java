package com.toroidalworld.platform;

import java.util.function.IntFunction;

import com.toroidalworld.shape.FlatShape;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public interface Platform {
    boolean isClient();

    String modVersion();

    String loaderName();

    String loaderVersion();

    void sendWorldShape(ServerPlayer player, FlatShape shape);

    IntFunction<RegistryFriendlyByteBuf> packetBuffers(ServerPlayer player);
}
