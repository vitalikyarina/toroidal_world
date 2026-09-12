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
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceRules;

class TerrainCeilingCutTest {
    private static final String JAGGEDNESS_PATH = "overworld/jaggedness";

    private static final int MIN_Y = 0;

    private static final int HEIGHT = 128;

    private static final int SIZE_HORIZONTAL = 1;

    private static final int SIZE_VERTICAL = 2;

    private static final int CELL_HEIGHT = QuartPos.toBlock(SIZE_VERTICAL);

    private static final int SURFACE_Y = 40;

    private static final double SURFACE_RISE_PER_BLOCK = 0.5;

    private static final double BASE_DENSITY = 0.5;

    private static final double BASE_BLOCKS = 40.0;

    private static final double RAMP_BLOCKS = 16.0;

    private static final double PENALTY = 0.25;

    private static final double NOISE_MAX = 1.0;

    private static final int PROBE_Y = 92;

    private static final int PROBE_Z = 0;

    private static final int COLUMNS = 16;

    private static final int KNEE_X = 8;

    private static final int BELOW_KNEE = 2;

    private static final long SEED = 1L;

    private static final double TOLERANCE = 1.0e-9;

    private static NoiseBasedChunkGenerator generator;

    private static RandomState randomState;

    @BeforeAll
    static void buildTheCeilingedGenerator() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        HolderLookup.Provider worldgen = VanillaRegistries.createLookup();
        NoiseGeneratorSettings shaped = TerrainCeiling.withCeiling(settings());
        generator = new NoiseBasedChunkGenerator(
                new FixedBiomeSource(worldgen.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS)),
                Holder.direct(shaped));
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
                ResourceKey.create(Registries.DENSITY_FUNCTION, Identifier.withDefaultNamespace(JAGGEDNESS_PATH)),
                DensityFunctions.zero(),
                RegistrationInfo.BUILT_IN);
        functions.freeze();
        DensityFunction jaggedness = DensityFunctions.flatCache(DensityFunctions
                .mul(new DensityFunctions.HolderHolder(spline), DensityFunctions.constant(NOISE_MAX)));
        DensityFunction zero = DensityFunctions.zero();
        return new NoiseRouter(zero, zero, zero, zero, zero, zero, zero, zero, zero, zero,
                new RisingSurface(),
                DensityFunctions.add(DensityFunctions.constant(BASE_DENSITY), jaggedness),
                zero, zero, zero);
    }

    private static double densityAt(int blockX, int blockY) {
        return generator.getInterpolatedNoiseValue(randomState,
                new DensityFunction.SinglePointContext(blockX, blockY, PROBE_Z));
    }

    private static int ceilingY(int blockX) {
        return (int) (SURFACE_Y + BASE_BLOCKS + SURFACE_RISE_PER_BLOCK * blockX);
    }

    @Test
    void noFourBlockPlateauSurvivesOnTheCut() {
        for (int blockX = 1; blockX < COLUMNS; blockX++) {
            assertNotEquals(densityAt(blockX - 1, PROBE_Y), densityAt(blockX, PROBE_Y),
                    "blockX " + blockX + " carries its neighbour's density unchanged");
        }
    }

    @Test
    void theCutRisesByTheSameStepAtEveryBlock() {
        double step = PENALTY * SURFACE_RISE_PER_BLOCK / RAMP_BLOCKS;

        for (int blockX = 1; blockX < COLUMNS; blockX++) {
            assertEquals(step, densityAt(blockX, PROBE_Y) - densityAt(blockX - 1, PROBE_Y), TOLERANCE,
                    "blockX " + blockX);
        }
    }

    @Test
    void theRampKeepsItsKneeWhereTheCeilingStarts() {
        int knee = ceilingY(KNEE_X);
        assertNotEquals(0, knee % CELL_HEIGHT,
                "a knee on a cell boundary holds wherever the marker sits, and grades nothing");

        assertEquals(BASE_DENSITY, densityAt(KNEE_X, knee), TOLERANCE);
        assertEquals(BASE_DENSITY, densityAt(KNEE_X, knee - BELOW_KNEE), TOLERANCE);
    }

    private record RisingSurface() implements DensityFunction.SimpleFunction {
        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return SURFACE_Y + Mth.clamp(context.blockX() * SURFACE_RISE_PER_BLOCK, 0.0, HEIGHT);
        }

        @Override
        public double minValue() {
            return SURFACE_Y;
        }

        @Override
        public double maxValue() {
            return SURFACE_Y + HEIGHT;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            throw new UnsupportedOperationException();
        }
    }
}
