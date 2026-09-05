package com.toroidalworld.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

class ShapedDimensionsTest {
    private static final HolderLookup.Provider WORLDGEN = VanillaRegistries.createLookup();

    private static final FlatShape TORUS = FlatShape.torus(WorldLoopBounds.ofWidth(32));

    private static final FlatShape CYLINDER = FlatShape.cylinder(
            new WorldLoopBounds(new AxisBounds.Looped(-32, 32), AxisBounds.Unbounded.INSTANCE));

    @Test
    void strippingASuperflatShapeHandsBackThePlainFlatSourceOnItsOwnSettings() {
        FlatLevelGeneratorSettings settings = flatSettings();
        WorldDimensions stripped = ShapedDimensions.stripShapes(overworldOf(new LoopedFlatChunkGenerator(
                settings, TORUS)));

        ChunkGenerator generator = overworldGeneratorOf(stripped);
        assertNull(ShapedDimensions.shapeOf(stripped, LevelStem.OVERWORLD));
        assertFalse(generator instanceof ShapedChunkGenerator, generator.getClass().getName());
        assertSame(settings, ((FlatLevelSource) generator).settings());
    }

    @Test
    void strippingANoiseShapeHandsBackThePlainNoiseGeneratorOnItsOwnBiomesAndSettings() {
        BiomeSource biomes = plainsBiomeSource();
        Holder<NoiseGeneratorSettings> settings = overworldNoiseSettings();
        WorldDimensions stripped = ShapedDimensions.stripShapes(overworldOf(new LoopedChunkGenerator(
                biomes, settings, TORUS)));

        ChunkGenerator generator = overworldGeneratorOf(stripped);
        assertNull(ShapedDimensions.shapeOf(stripped, LevelStem.OVERWORLD));
        assertFalse(generator instanceof ShapedChunkGenerator, generator.getClass().getName());
        assertSame(biomes, generator.getBiomeSource());
        assertSame(settings, ((NoiseBasedChunkGenerator) generator).generatorSettings());
    }

    @Test
    void reShapingASuperflatWorldRebuildsFromTheFlatSettingsRatherThanTheOldShape() {
        FlatLevelGeneratorSettings settings = flatSettings();
        WorldDimensions reshaped = ShapedDimensions.withShape(
                overworldOf(new LoopedFlatChunkGenerator(settings, TORUS)), LevelStem.OVERWORLD, CYLINDER);

        assertEquals(CYLINDER, ShapedDimensions.shapeOf(reshaped, LevelStem.OVERWORLD));
        assertSame(settings, ((LoopedFlatChunkGenerator) overworldGeneratorOf(reshaped)).settings());
    }

    @Test
    void strippingAnUnshapedWorldHandsBackTheArgument() {
        WorldDimensions vanilla = overworldOf(new NoiseBasedChunkGenerator(
                plainsBiomeSource(), overworldNoiseSettings()));

        assertSame(vanilla, ShapedDimensions.stripShapes(vanilla));
    }

    private static WorldDimensions overworldOf(ChunkGenerator generator) {
        return new WorldDimensions(Map.of(LevelStem.OVERWORLD, new LevelStem(
                WORLDGEN.lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(BuiltinDimensionTypes.OVERWORLD),
                generator)));
    }

    private static ChunkGenerator overworldGeneratorOf(WorldDimensions dimensions) {
        return dimensions.dimensions().get(LevelStem.OVERWORLD).generator();
    }

    private static FlatLevelGeneratorSettings flatSettings() {
        return new FlatLevelGeneratorSettings(Optional.empty(),
                WORLDGEN.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS), List.of());
    }

    private static BiomeSource plainsBiomeSource() {
        return new FixedBiomeSource(WORLDGEN.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS));
    }

    private static Holder<NoiseGeneratorSettings> overworldNoiseSettings() {
        return WORLDGEN.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD);
    }
}
