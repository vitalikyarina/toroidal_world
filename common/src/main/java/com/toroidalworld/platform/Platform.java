package com.toroidalworld.platform;

import java.util.function.IntFunction;

import com.toroidalworld.shape.FlatShape;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public interface Platform {
    boolean isClient();

    String modVersion();

    String loaderName();

    String loaderVersion();

    void sendWorldShape(ServerPlayer player, ResourceKey<Level> dimension, FlatShape shape);

    IntFunction<RegistryFriendlyByteBuf> packetBuffers(ServerPlayer player);
}
