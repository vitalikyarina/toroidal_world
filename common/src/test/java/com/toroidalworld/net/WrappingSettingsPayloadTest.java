package com.toroidalworld.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.shape.FlatShape;

import io.netty.buffer.Unpooled;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

class WrappingSettingsPayloadTest {
    private static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private static final WorldLoopBounds SQUARE = WorldLoopBounds.ofWidth(32);
    private static final WorldLoopBounds X_ONLY =
            new WorldLoopBounds(new AxisBounds.Looped(-16, 16), AxisBounds.Unbounded.INSTANCE);
    private static final WorldLoopBounds Z_ONLY =
            new WorldLoopBounds(AxisBounds.Unbounded.INSTANCE, new AxisBounds.Looped(-16, 16));

    private static final ResourceKey<Level> DATAPACK_DIMENSION =
            ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("cject", "hollow"));
    private static final List<ResourceKey<Level>> DIMENSIONS =
            List.of(Level.OVERWORLD, Level.NETHER, Level.END, DATAPACK_DIMENSION);

    private static RegistryFriendlyByteBuf buffer() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), REGISTRIES);
    }

    @Test
    void everyShapeTheEpicPlansRoundTripsInEveryDimensionAndDrainsTheBuffer() {
        List<FlatShape> shapes = List.of(
                FlatShape.rectangle(),
                FlatShape.cylinder(X_ONLY),
                FlatShape.cylinder(Z_ONLY),
                FlatShape.latticeTorus(SQUARE, 0),
                FlatShape.latticeTorus(SQUARE, 5),
                FlatShape.latticeTorus(new WorldLoopBounds(-4096, 4096, -4096, 4096), 0),
                FlatShape.mirrored(Z_ONLY, Direction.Axis.X, 3),
                FlatShape.mirrored(SQUARE, Direction.Axis.Z, -7));
        for (ResourceKey<Level> dimension : DIMENSIONS) {
            for (FlatShape shape : shapes) {
                RegistryFriendlyByteBuf buffer = buffer();
                WrappingSettingsPayload.STREAM_CODEC.encode(buffer, new WrappingSettingsPayload(dimension, shape));

                WrappingSettingsPayload decoded = WrappingSettingsPayload.STREAM_CODEC.decode(buffer);
                String label = dimension + " " + shape;
                assertEquals(dimension, decoded.dimension(), label);
                assertEquals(shape, decoded.shape(), label);
                assertEquals(0, buffer.readableBytes(), label);
            }
        }
    }

    @Test
    void anUnmirroredShapeLaysDownItsDimensionThenItsExactBytes() {
        assertWire(Level.OVERWORLD, FlatShape.cylinder(X_ONLY), new byte[] {
                0x01, (byte) 0xF0, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x0F, 0x10,
                0x00,
                0x00,
                0x00});
    }

    @Test
    void aMirroredShapeLaysDownItsDimensionThenItsExactBytes() {
        assertWire(Level.NETHER, FlatShape.mirrored(Z_ONLY, Direction.Axis.X, 3), new byte[] {
                0x00,
                0x01, (byte) 0xF0, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x0F, 0x10,
                0x00,
                0x01, 0x00, 0x03});
    }

    @Test
    void anUnwrappedShapeIsFourBytesAfterItsDimension() {
        RegistryFriendlyByteBuf buffer = buffer();
        WrappingSettingsPayload.STREAM_CODEC.encode(buffer,
                new WrappingSettingsPayload(Level.OVERWORLD, FlatShape.rectangle()));

        assertEquals(dimensionWire(Level.OVERWORLD).length + 4, buffer.readableBytes());
    }

    private static void assertWire(ResourceKey<Level> dimension, FlatShape shape, byte[] shapeWire) {
        byte[] dimensionWire = dimensionWire(dimension);
        byte[] wire = new byte[dimensionWire.length + shapeWire.length];
        System.arraycopy(dimensionWire, 0, wire, 0, dimensionWire.length);
        System.arraycopy(shapeWire, 0, wire, dimensionWire.length, shapeWire.length);

        RegistryFriendlyByteBuf encoded = buffer();
        WrappingSettingsPayload.STREAM_CODEC.encode(encoded, new WrappingSettingsPayload(dimension, shape));
        byte[] written = new byte[encoded.readableBytes()];
        encoded.readBytes(written);
        assertArrayEquals(wire, written);

        RegistryFriendlyByteBuf incoming = buffer();
        incoming.writeBytes(wire);
        WrappingSettingsPayload decoded = WrappingSettingsPayload.STREAM_CODEC.decode(incoming);
        assertEquals(dimension, decoded.dimension());
        assertEquals(shape, decoded.shape());
        assertEquals(0, incoming.readableBytes());
    }

    private static byte[] dimensionWire(ResourceKey<Level> dimension) {
        byte[] name = dimension.identifier().toString().getBytes(StandardCharsets.UTF_8);
        byte[] wire = new byte[name.length + 1];
        wire[0] = (byte) name.length;
        System.arraycopy(name, 0, wire, 1, name.length);
        return wire;
    }
}
