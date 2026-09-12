package com.toroidalworld.engine.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mojang.serialization.Lifecycle;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.blending.Blender;

class TerrainCeilingCutTest {
    private static final String JAGGEDNESS_PATH = "overworld/jaggedness";

    private static final int MIN_Y = 0;

    private static final int HEIGHT = 128;

    private static final int SIZE_HORIZONTAL = 1;

    private static final int SIZE_VERTICAL = 2;

    private static final int CELL_WIDTH = QuartPos.toBlock(SIZE_HORIZONTAL);

    private static final int CELL_HEIGHT = QuartPos.toBlock(SIZE_VERTICAL);

    private static final int SURFACE_Y = 44;

    private static final double SURFACE_RISE_PER_BLOCK = 0.5;

    private static final double BASE_DENSITY = 0.5;

    private static final double BASE_BLOCKS = 40.0;

    private static final double RAMP_BLOCKS = 16.0;

    private static final double PENALTY = 0.25;

    // The column walk in PreliminarySurfaceLevel: its step, and the density it reads as solid. The ceiling of
    // this line stands on one of those steps, so both numbers decide where the ramp begins.
    private static final int CEILING_SCAN_STEP = 8;

    private static final double SOLID_DENSITY = 0.390625;

    private static final double NOISE_MAX = 1.0;

    private static final int PROBE_Y = 92;

    private static final int PROBE_Z = 0;

    private static final int STEP_CELL_X = 8;

    private static final int KNEE_X = STEP_CELL_X;

    private static final int BELOW_KNEE = 2;

    private static final long SEED = 1L;

    private static final double TOLERANCE = 1.0e-9;

    private static final double DENSITY_BOUND = 1024.0;

    private static NoiseGeneratorSettings shaped;

    private static RandomState randomState;

    private static final Aquifer.FluidPicker NO_FLUID =
            (x, y, z) -> new Aquifer.FluidStatus(MIN_Y, Blocks.AIR.defaultBlockState());

    private static final DensityFunctions.BeardifierOrMarker NO_BEARDIFIER = new FlatZero();

    @BeforeAll
    static void buildTheCeilingedGenerator() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        HolderLookup.Provider worldgen = VanillaRegistries.createLookup();
        shaped = TerrainCeiling.withCeiling(settings());
        randomState = RandomState.create(shaped, worldgen.lookupOrThrow(Registries.NOISE), SEED);
    }

    @SuppressWarnings("deprecation")
    private static NoiseGeneratorSettings settings() {
        return new NoiseGeneratorSettings(
                NoiseSettings.create(MIN_Y, HEIGHT, SIZE_HORIZONTAL, SIZE_VERTICAL),
                Blocks.STONE.defaultBlockState(),
                Blocks.AIR.defaultBlockState(),
                router(),
                SurfaceRules.state(Blocks.STONE.defaultBlockState()),
                List.of(),
                MIN_Y,
                false,
                false,
                false,
                false);
    }

    private static NoiseRouter router() {
        MappedRegistry<DensityFunction> functions =
                new MappedRegistry<>(Registries.DENSITY_FUNCTION, Lifecycle.stable());
        Holder.Reference<DensityFunction> spline = functions.register(
                ResourceKey.create(Registries.DENSITY_FUNCTION, ResourceLocation.withDefaultNamespace(JAGGEDNESS_PATH)),
                DensityFunctions.zero(),
                RegistrationInfo.BUILT_IN);
        functions.freeze();
        DensityFunction jaggedness = DensityFunctions.flatCache(DensityFunctions
                .mul(new DensityFunctions.HolderHolder(spline), DensityFunctions.constant(NOISE_MAX)));
        DensityFunction zero = DensityFunctions.zero();
        return new NoiseRouter(zero, zero, zero, zero, zero, zero, zero, zero, zero, zero,
                new SolidBelowSurface(),
                DensityFunctions.add(DensityFunctions.constant(BASE_DENSITY), jaggedness),
                zero, zero, zero);
    }

    // The 26.x line reads this through NoiseBasedChunkGenerator.getInterpolatedNoiseValue, which 1.21.1 does not
    // carry: here the cell walk is driven by hand, the same one NoiseChunk performs while it fills a chunk.
    private static double densityAt(int blockX, int blockY) {
        NoiseSettings noiseSettings = shaped.noiseSettings();
        int cellWidth = noiseSettings.getCellWidth();
        int cellHeight = noiseSettings.getCellHeight();
        NoiseChunk noiseChunk = new NoiseChunk(
                1,
                randomState,
                blockX - Math.floorMod(blockX, cellWidth),
                PROBE_Z - Math.floorMod(PROBE_Z, cellWidth),
                noiseSettings,
                NO_BEARDIFIER,
                shaped,
                NO_FLUID,
                Blender.empty());
        NoiseRouter wrapped = randomState.router().mapAll(noiseChunk::wrap);
        DensityFunction interpolated =
                DensityFunctions.cacheAllInCell(wrapped.finalDensity()).mapAll(noiseChunk::wrap);
        noiseChunk.initializeForFirstCellX();
        noiseChunk.advanceCellX(0);
        noiseChunk.selectCellYZ(Math.floorDiv(blockY - MIN_Y, cellHeight), 0);
        noiseChunk.updateForY(blockY, (double) Math.floorMod(blockY - MIN_Y, cellHeight) / cellHeight);
        noiseChunk.updateForX(blockX, (double) Math.floorMod(blockX, cellWidth) / cellWidth);
        noiseChunk.updateForZ(PROBE_Z, (double) Math.floorMod(PROBE_Z, cellWidth) / cellWidth);
        return interpolated.compute(noiseChunk);
    }

    private static double surfaceY(int blockX) {
        return SURFACE_Y + SURFACE_RISE_PER_BLOCK * blockX;
    }

    private static int ceilingY(int blockX) {
        int level = CEILING_SCAN_STEP * Mth.floor((surfaceY(blockX) - SOLID_DENSITY) / CEILING_SCAN_STEP);
        return level + (int) BASE_BLOCKS;
    }

    @Test
    void noPlateauSurvivesTheCellTheCeilingStepsAcross() {
        for (int blockX = STEP_CELL_X + 1; blockX < STEP_CELL_X + CELL_WIDTH; blockX++) {
            assertNotEquals(densityAt(blockX - 1, PROBE_Y), densityAt(blockX, PROBE_Y),
                    "blockX " + blockX + " carries its neighbour's density unchanged");
        }
    }

    @Test
    void theCutRisesByTheSameStepAtEveryBlockOfThatCell() {
        double step = PENALTY * CEILING_SCAN_STEP / RAMP_BLOCKS / CELL_WIDTH;

        for (int blockX = STEP_CELL_X + 1; blockX < STEP_CELL_X + CELL_WIDTH; blockX++) {
            assertEquals(step, densityAt(blockX, PROBE_Y) - densityAt(blockX - 1, PROBE_Y), TOLERANCE,
                    "blockX " + blockX);
        }
    }

    @Test
    void theRampKeepsItsKneeWhereTheCeilingStarts() {
        int knee = ceilingY(KNEE_X);
        assertEquals(0, knee % CELL_HEIGHT,
                "the scan step and the base are both multiples of the cell height, so the knee sits on a boundary");

        assertEquals(BASE_DENSITY, densityAt(KNEE_X, knee), TOLERANCE);
        assertEquals(BASE_DENSITY, densityAt(KNEE_X, knee - BELOW_KNEE), TOLERANCE);
    }

    private record FlatZero() implements DensityFunctions.BeardifierOrMarker {
        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return 0.0;
        }

        @Override
        public double minValue() {
            return 0.0;
        }

        @Override
        public double maxValue() {
            return 0.0;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            throw new UnsupportedOperationException();
        }
    }

    private record SolidBelowSurface() implements DensityFunction.SimpleFunction {
        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return surfaceY(context.blockX()) - context.blockY();
        }

        @Override
        public double minValue() {
            return -DENSITY_BOUND;
        }

        @Override
        public double maxValue() {
            return DENSITY_BOUND;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            throw new UnsupportedOperationException();
        }
    }
}
