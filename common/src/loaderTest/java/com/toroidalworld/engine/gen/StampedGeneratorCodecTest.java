package com.toroidalworld.engine.gen;

import com.toroidalworld.ToroidalWorld;
import com.toroidalworld.core.CarriedShape;
import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.GenerationOptions;
import com.toroidalworld.core.ShapedChunkGenerator;
import com.toroidalworld.shape.WorldOptionSetup;
import com.toroidalworld.shape.torus.ClimateScale;
import com.toroidalworld.shape.torus.CompactBiomes;
import static com.toroidalworld.engine.gen.BakeStampFixture.noiseGenerator;
import static com.toroidalworld.engine.gen.BakeStampFixture.squareTorus;
import static com.toroidalworld.engine.gen.BakeStampFixture.stamped;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

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
    private static final String SHAPE_KEY = ToroidalWorld.MODID + ":" + CarriedShape.WRAPPING_KEY;
    private static final String CLIMATE_SCALE_KEY =
            ToroidalWorld.MODID + ":" + CompactBiomes.KEY;

    private static final GenerationOptions UNCOMPRESSED =
            GenerationOptions.DEFAULT.with(CompactBiomes.OPTION, ClimateScale.OFF);

    private static final int STAMPED_CHUNK_WIDTH = 64;

    private static final String UNREADABLE_SHAPE = "not a shape at all";

    private static HolderLookup.Provider worldgen;

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        WorldOptionSetup.registerAll(false);
        worldgen = VanillaRegistries.createLookup();
    }

    @Test
    void aStampedShapeSurvivesTheRoundTrip() {
        FlatShape shape = squareTorus(STAMPED_CHUNK_WIDTH);
        CompoundTag encoded = encode(stamped(noiseGenerator(worldgen), shape));

        assertTrue(encoded.contains(SHAPE_KEY), "the stamped shape never reached the encoded generator");
        assertFalse(encoded.contains(CLIMATE_SCALE_KEY), "a compressed stamp wrote the choice it need not");

        CarriedShape decoded = carriedShapeOf(decode(encoded).getOrThrow());
        assertEquals(shape, decoded.shape());
        assertEquals(ClimateScale.AUTO, decoded.generationOptions().get(CompactBiomes.OPTION));
    }

    @Test
    void aStampedChoiceAgainstCompressionSurvivesTheRoundTrip() {
        FlatShape shape = squareTorus(STAMPED_CHUNK_WIDTH);
        CompoundTag encoded = encode(stamped(noiseGenerator(worldgen), shape, UNCOMPRESSED));

        assertTrue(encoded.contains(CLIMATE_SCALE_KEY), "the stamped choice never reached the encoded generator");

        CarriedShape decoded = carriedShapeOf(decode(encoded).getOrThrow());
        assertEquals(shape, decoded.shape());
        assertEquals(ClimateScale.OFF, decoded.generationOptions().get(CompactBiomes.OPTION));
    }

    @Test
    void anUnstampedGeneratorCarriesNoShapeKey() {
        CompoundTag encoded = encode(noiseGenerator(worldgen));

        assertFalse(encoded.contains(SHAPE_KEY));
        assertNull(ShapedChunkGenerator.carriedShapeOf(decode(encoded).getOrThrow()));
    }

    @Test
    void anUnreadableStoredShapeRefusesTheGenerator() {
        CompoundTag encoded = encode(noiseGenerator(worldgen));
        encoded.putString(SHAPE_KEY, UNREADABLE_SHAPE);

        assertTrue(decode(encoded).isError(), "an unreadable stored shape loaded as an ordinary generator");
    }

    private static CarriedShape carriedShapeOf(ChunkGenerator generator) {
        CarriedShape carried = ShapedChunkGenerator.carriedShapeOf(generator);
        assertNotNull(carried, "the decoded generator carries no shape");
        return carried;
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
