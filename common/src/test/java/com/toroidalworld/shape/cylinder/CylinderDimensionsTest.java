package com.toroidalworld.shape.cylinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.toroidalworld.gen.ShapedDimensions;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.shape.FlatShape;
import com.toroidalworld.shape.torus.TorusDimensions;
import com.toroidalworld.shape.torus.TorusSettings;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldDimensions;

class CylinderDimensionsTest {
    private static final HolderLookup.Provider WORLDGEN = VanillaRegistries.createLookup();

    private static final CylinderSettings X_32 = new CylinderSettings(
            WorldLoopBounds.ofWidth(Direction.Axis.X, 32), 2, WorldLoopBounds.ofWidth(Direction.Axis.X, 256));

    private static final CylinderSettings Z_64 = new CylinderSettings(
            WorldLoopBounds.ofWidth(Direction.Axis.Z, 64), 4, WorldLoopBounds.ofWidth(Direction.Axis.Z, 320));

    @Test
    void anXCylinderRoundTripsThroughItsThreeGenerators() {
        WorldDimensions created = CylinderDimensions.apply(vanillaDimensions(), X_32);

        assertEquals(FlatShape.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.X, 32)),
                ShapedDimensions.shapeOf(created, LevelStem.OVERWORLD));
        assertEquals(FlatShape.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.X, 16)),
                ShapedDimensions.shapeOf(created, LevelStem.NETHER));
        assertEquals(FlatShape.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.X, 256)),
                ShapedDimensions.shapeOf(created, LevelStem.END));
        assertEquals(X_32, CylinderDimensions.read(created));
    }

    @Test
    void aZCylinderRoundTripsThroughItsThreeGenerators() {
        WorldDimensions created = CylinderDimensions.apply(vanillaDimensions(), Z_64);

        assertEquals(FlatShape.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.Z, 64)),
                ShapedDimensions.shapeOf(created, LevelStem.OVERWORLD));
        assertEquals(FlatShape.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.Z, 16)),
                ShapedDimensions.shapeOf(created, LevelStem.NETHER));
        assertEquals(FlatShape.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.Z, 320)),
                ShapedDimensions.shapeOf(created, LevelStem.END));
        assertEquals(Z_64, CylinderDimensions.read(created));
    }

    @Test
    void theDefaultNetherScaleFallsToWhatTheWidthAdmits() {
        WorldDimensions created = CylinderDimensions.apply(vanillaDimensions(), CylinderSettings.DEFAULT);

        assertEquals(FlatShape.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.X, 16)),
                ShapedDimensions.shapeOf(created, LevelStem.NETHER));
        assertEquals(2, CylinderDimensions.read(created).netherScale());
    }

    @Test
    void aWorldWithOnlyAnOverworldReadsBackTheDefaultsOnItsAxis() {
        WorldDimensions created = CylinderDimensions.apply(overworldOnly(), Z_64);

        assertNull(ShapedDimensions.shapeOf(created, LevelStem.NETHER));
        assertEquals(new CylinderSettings(WorldLoopBounds.ofWidth(Direction.Axis.Z, 64), 4,
                WorldLoopBounds.ofWidth(Direction.Axis.Z, 256)), CylinderDimensions.read(created));
    }

    @Test
    void aTorusAndACylinderNeverClaimEachOther() {
        WorldDimensions torus = TorusDimensions.apply(vanillaDimensions(), TorusSettings.DEFAULT);
        WorldDimensions cylinder = CylinderDimensions.apply(vanillaDimensions(), X_32);

        assertNull(CylinderDimensions.read(torus));
        assertNull(TorusDimensions.read(cylinder));
        assertNull(CylinderDimensions.read(vanillaDimensions()));
    }

    @Test
    void settingsRefuseATorusOverworldAndAnEndOffTheAxis() {
        assertThrows(IllegalArgumentException.class, () -> new CylinderSettings(
                WorldLoopBounds.ofWidth(32), 2, WorldLoopBounds.ofWidth(Direction.Axis.X, 256)));
        assertThrows(IllegalArgumentException.class, () -> new CylinderSettings(
                WorldLoopBounds.ofWidth(Direction.Axis.X, 32), 2, WorldLoopBounds.ofWidth(Direction.Axis.Z, 256)));
        assertThrows(IllegalArgumentException.class, () -> new CylinderSettings(
                WorldLoopBounds.ofWidth(Direction.Axis.X, 32), 2, WorldLoopBounds.ofWidth(256)));
    }

    private static WorldDimensions vanillaDimensions() {
        Map<ResourceKey<LevelStem>, LevelStem> stems = new HashMap<>();
        stems.put(LevelStem.OVERWORLD, stem(BuiltinDimensionTypes.OVERWORLD, NoiseGeneratorSettings.OVERWORLD));
        stems.put(LevelStem.NETHER, stem(BuiltinDimensionTypes.NETHER, NoiseGeneratorSettings.NETHER));
        stems.put(LevelStem.END, stem(BuiltinDimensionTypes.END, NoiseGeneratorSettings.END));
        return new WorldDimensions(stems);
    }

    private static WorldDimensions overworldOnly() {
        return new WorldDimensions(Map.of(LevelStem.OVERWORLD,
                stem(BuiltinDimensionTypes.OVERWORLD, NoiseGeneratorSettings.OVERWORLD)));
    }

    private static LevelStem stem(ResourceKey<DimensionType> type, ResourceKey<NoiseGeneratorSettings> noise) {
        NoiseBasedChunkGenerator generator = new NoiseBasedChunkGenerator(
                new FixedBiomeSource(WORLDGEN.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS)),
                WORLDGEN.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(noise));
        return new LevelStem(WORLDGEN.lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(type), generator);
    }
}
