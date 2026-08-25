package com.toroidalworld.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.shape.FlatShape;

import io.netty.buffer.Unpooled;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;

class WrappingSettingsPayloadTest {
    private static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private static final WorldLoopBounds SQUARE = WorldLoopBounds.ofWidth(32);
    private static final WorldLoopBounds X_ONLY =
            new WorldLoopBounds(new AxisBounds.Looped(-16, 16), AxisBounds.Unbounded.INSTANCE);
    private static final WorldLoopBounds Z_ONLY =
            new WorldLoopBounds(AxisBounds.Unbounded.INSTANCE, new AxisBounds.Looped(-16, 16));

    private static RegistryFriendlyByteBuf buffer() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), REGISTRIES);
    }

    @Test
    void everyShapeTheEpicPlansRoundTripsAndDrainsTheBuffer() {
        List<FlatShape> shapes = List.of(
                FlatShape.rectangle(),
                FlatShape.cylinder(X_ONLY),
                FlatShape.cylinder(Z_ONLY),
                FlatShape.latticeTorus(SQUARE, 0),
                FlatShape.latticeTorus(SQUARE, 5),
                FlatShape.latticeTorus(new WorldLoopBounds(-4096, 4096, -4096, 4096), 0),
                FlatShape.mirrored(Z_ONLY, Direction.Axis.X, 3),
                FlatShape.mirrored(SQUARE, Direction.Axis.Z, -7));
        for (FlatShape shape : shapes) {
            RegistryFriendlyByteBuf buffer = buffer();
            WrappingSettingsPayload.STREAM_CODEC.encode(buffer, new WrappingSettingsPayload(shape));

            assertEquals(shape, WrappingSettingsPayload.STREAM_CODEC.decode(buffer).shape(), shape.toString());
            assertEquals(0, buffer.readableBytes(), shape.toString());
        }
    }

    @Test
    void anUnmirroredShapeLaysDownItsExactBytes() {
        assertWire(FlatShape.cylinder(X_ONLY), new byte[] {
                0x01, (byte) 0xF0, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x0F, 0x10,
                0x00,
                0x00,
                0x00});
    }

    @Test
    void aMirroredShapeLaysDownItsExactBytes() {
        assertWire(FlatShape.mirrored(Z_ONLY, Direction.Axis.X, 3), new byte[] {
                0x00,
                0x01, (byte) 0xF0, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x0F, 0x10,
                0x00,
                0x01, 0x00, 0x03});
    }

    @Test
    void anUnwrappedShapeIsFourBytesOnTheWire() {
        RegistryFriendlyByteBuf buffer = buffer();
        WrappingSettingsPayload.STREAM_CODEC.encode(buffer, new WrappingSettingsPayload(FlatShape.rectangle()));

        assertEquals(4, buffer.readableBytes());
    }

    private static void assertWire(FlatShape shape, byte[] wire) {
        RegistryFriendlyByteBuf encoded = buffer();
        WrappingSettingsPayload.STREAM_CODEC.encode(encoded, new WrappingSettingsPayload(shape));
        byte[] written = new byte[encoded.readableBytes()];
        encoded.readBytes(written);
        assertArrayEquals(wire, written);

        RegistryFriendlyByteBuf incoming = buffer();
        incoming.writeBytes(wire);
        assertEquals(shape, WrappingSettingsPayload.STREAM_CODEC.decode(incoming).shape());
        assertEquals(0, incoming.readableBytes());
    }
}
