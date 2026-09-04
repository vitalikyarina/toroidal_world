package com.toroidalworld.gen;

import static com.toroidalworld.gen.BakeStampFixture.noiseGenerator;
import static com.toroidalworld.gen.BakeStampFixture.stamped;
import static com.toroidalworld.gen.BakeStampFixture.squareTorus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.toroidalworld.ToroidalWorld;
import com.toroidalworld.shape.FlatShape;
import com.mojang.serialization.DataResult;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.chunk.ChunkGenerator;

@Timeout(60)
class StampedGeneratorCodecTest {
    private static final String SHAPE_KEY = ToroidalWorld.MODID + ":" + ShapedChunkGenerator.WRAPPING_KEY;

    private static final int STAMPED_CHUNK_WIDTH = 64;

    private static final String UNREADABLE_SHAPE = "not a shape at all";

    private static HolderLookup.Provider worldgen;

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        worldgen = VanillaRegistries.createLookup();
    }

    @Test
    void aStampedShapeSurvivesTheRoundTrip() {
        FlatShape shape = squareTorus(STAMPED_CHUNK_WIDTH);
        CompoundTag encoded = encode(stamped(noiseGenerator(worldgen), shape));

        assertTrue(encoded.contains(SHAPE_KEY), "the stamped shape never reached the encoded generator");
        assertEquals(shape, ShapedChunkGenerator.wrappedShapeOf(decode(encoded).getOrThrow()));
    }

    @Test
    void anUnstampedGeneratorCarriesNoShapeKey() {
        CompoundTag encoded = encode(noiseGenerator(worldgen));

        assertFalse(encoded.contains(SHAPE_KEY));
        assertNull(ShapedChunkGenerator.wrappedShapeOf(decode(encoded).getOrThrow()));
    }

    @Test
    void anUnreadableStoredShapeRefusesTheGenerator() {
        CompoundTag encoded = encode(noiseGenerator(worldgen));
        encoded.putString(SHAPE_KEY, UNREADABLE_SHAPE);

        assertTrue(decode(encoded).isError(), "an unreadable stored shape loaded as an ordinary generator");
    }

    private static CompoundTag encode(ChunkGenerator generator) {
        return assertInstanceOf(CompoundTag.class,
                ChunkGenerator.CODEC.encodeStart(ops(), generator).getOrThrow());
    }

    private static DataResult<ChunkGenerator> decode(CompoundTag encoded) {
        return ChunkGenerator.CODEC.parse(ops(), encoded);
    }

    private static RegistryOps<Tag> ops() {
        return worldgen.createSerializationContext(NbtOps.INSTANCE);
    }
}
