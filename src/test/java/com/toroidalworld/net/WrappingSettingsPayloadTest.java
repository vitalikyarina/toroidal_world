package com.toroidalworld.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.connection.ConnectionType;

// The payload is the client's only source of a level's wrap bounds, so the wire format is a contract: every axis shape
// the bounds model allows must come back exactly, and reading must consume exactly what writing produced — a codec
// that leaves bytes behind desyncs every payload after it in the buffer.
class WrappingSettingsPayloadTest {
    private static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private static RegistryFriendlyByteBuf buffer() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), REGISTRIES, ConnectionType.OTHER);
    }

    @Test
    void everyAxisShapeRoundTripsAndDrainsTheBuffer() {
        List<WorldLoopBounds> shapes = List.of(
                new WorldLoopBounds(-32, 32, -32, 32),
                new WorldLoopBounds(-48, 16, 0, 64),
                new WorldLoopBounds(-4096, 4096, -4096, 4096),
                WorldLoopBounds.UNBOUNDED,
                new WorldLoopBounds(new AxisBounds.Looped(-16, 16), AxisBounds.Unbounded.INSTANCE),
                new WorldLoopBounds(AxisBounds.Unbounded.INSTANCE, new AxisBounds.Looped(-16, 16)));
        for (WorldLoopBounds bounds : shapes) {
            RegistryFriendlyByteBuf buffer = buffer();
            WrappingSettingsPayload.STREAM_CODEC.encode(buffer, new WrappingSettingsPayload(bounds));

            assertEquals(bounds, WrappingSettingsPayload.STREAM_CODEC.decode(buffer).bounds(), bounds.toString());
            assertEquals(0, buffer.readableBytes(), bounds.toString());
        }
    }

    @Test
    void anUnboundedAxisIsASingleByteOnTheWire() {
        RegistryFriendlyByteBuf buffer = buffer();
        WrappingSettingsPayload.STREAM_CODEC.encode(buffer, new WrappingSettingsPayload(WorldLoopBounds.UNBOUNDED));

        assertEquals(2, buffer.readableBytes());
    }
}
