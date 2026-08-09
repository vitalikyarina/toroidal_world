package com.toroidalworld.net;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.ToroidalWorld;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// The mod's first custom packet. A client's own transformer is NOOP by construction — it is told the world is
// infinite, which is what keeps rendering and physics vanilla — so it cannot know a level's wrap bounds on its own.
// This hands them over. The debug overlay is the only reader today; the client-side transformer it seeds is the
// plumbing wrapped rendering will build on.
public record WrappingSettingsPayload(WorldLoopBounds bounds) implements CustomPacketPayload {
    public static final Type<WrappingSettingsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ToroidalWorld.MODID, "wrapping_settings"));

    // One axis on the wire: a looped flag, then the two chunk bounds only when the axis has any. An unbounded axis is
    // a single bit — there are no numbers that could stand for it.
    private static final StreamCodec<ByteBuf, AxisBounds> AXIS_STREAM_CODEC = StreamCodec.of(
            (buffer, axis) -> {
                switch (axis) {
                    case AxisBounds.Looped looped -> {
                        buffer.writeBoolean(true);
                        VarInt.write(buffer, looped.minChunk());
                        VarInt.write(buffer, looped.maxChunk());
                    }
                    case AxisBounds.Unbounded() -> buffer.writeBoolean(false);
                }
            },
            buffer -> buffer.readBoolean()
                    ? new AxisBounds.Looped(VarInt.read(buffer), VarInt.read(buffer))
                    : AxisBounds.Unbounded.INSTANCE);

    public static final StreamCodec<RegistryFriendlyByteBuf, WrappingSettingsPayload> STREAM_CODEC = StreamCodec.composite(
            AXIS_STREAM_CODEC, payload -> payload.bounds().x(),
            AXIS_STREAM_CODEC, payload -> payload.bounds().z(),
            (x, z) -> new WrappingSettingsPayload(new WorldLoopBounds(x, z)));

    @Override
    public Type<WrappingSettingsPayload> type() {
        return TYPE;
    }
}
