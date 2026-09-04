package com.toroidalworld.net;

import com.toroidalworld.shape.FlatShape;
import com.toroidalworld.ToroidalWorld;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public record WrappingSettingsPayload(ResourceKey<Level> dimension, FlatShape shape) implements CustomPacketPayload {
    public static final Type<WrappingSettingsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ToroidalWorld.MODID, "wrapping_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WrappingSettingsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceKey.streamCodec(Registries.DIMENSION), WrappingSettingsPayload::dimension,
                    FlatShape.STREAM_CODEC, WrappingSettingsPayload::shape,
                    WrappingSettingsPayload::new);

    @Override
    public Type<WrappingSettingsPayload> type() {
        return TYPE;
    }
}
