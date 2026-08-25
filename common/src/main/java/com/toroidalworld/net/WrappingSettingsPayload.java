package com.toroidalworld.net;

import com.toroidalworld.shape.FlatShape;
import com.toroidalworld.ToroidalWorld;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WrappingSettingsPayload(FlatShape shape) implements CustomPacketPayload {
    public static final Type<WrappingSettingsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ToroidalWorld.MODID, "wrapping_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WrappingSettingsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    FlatShape.STREAM_CODEC, WrappingSettingsPayload::shape,
                    WrappingSettingsPayload::new);

    @Override
    public Type<WrappingSettingsPayload> type() {
        return TYPE;
    }
}
