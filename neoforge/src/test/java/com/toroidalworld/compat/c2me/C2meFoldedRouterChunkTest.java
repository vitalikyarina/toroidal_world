package com.toroidalworld.compat.c2me;

import static com.toroidalworld.noise.DensityFunctionFixture.SQUARE;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.toroidalworld.noise.GenerationTransformerContext;
import com.ishland.c2me.opts.dfc.common.gen.jvm.AbstractCompiledDensityFunction;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class C2meFoldedRouterChunkTest {
    private static final long SEED = 305L;

    private static final int CHUNK_BLOCKS = 16;

    private static final int PROBE_CHUNK_X = 3;
    private static final int PROBE_CHUNK_Z = 5;

    private static final int PROBE_BLOCK_Y = 64;

    private static final int LAVA_LEVEL = -54;

    private static final int TERRAIN_HASH_FACTOR = 31;

    private static HolderLookup.Provider holders;
    private static NoiseGeneratorSettings overworld;
    private static HolderGetter<NormalNoise.NoiseParameters> noises;

    private record Terrain(int placed, int hash) {
    }

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        holders = VanillaRegistries.createLookup();
        noises = holders.lookupOrThrow(Registries.NOISE);
        overworld = holders.lookupOrThrow(Registries.NOISE_SETTINGS)
                .getOrThrow(NoiseGeneratorSettings.OVERWORLD).value();
    }

    @Test
    void c2meCompilesTheRealOverworldRouterOnBothKindsOfLevel() {
        assertInstanceOf(AbstractCompiledDensityFunction.class, foldedState().router().finalDensity(),
                "C2ME did not compile the wrapped router, so the fold never reaches the emitted path");
        assertInstanceOf(AbstractCompiledDensityFunction.class, unfoldedState().router().finalDensity(),
                "C2ME did not compile the unwrapped router, so the two levels do not run the same machinery");
    }

    @Test
    void theRealOverworldRouterAnswersDifferentlyOnAWrappedLevel() {
        DensityFunction.FunctionContext at = new DensityFunction.SinglePointContext(
                PROBE_CHUNK_X * CHUNK_BLOCKS, PROBE_BLOCK_Y, PROBE_CHUNK_Z * CHUNK_BLOCKS);

        RandomState folded = foldedState();
        double wrapped = GenerationTransformerContext.withTransformer(SQUARE,
                () -> folded.router().finalDensity().compute(at));
        double plain = unfoldedState().router().finalDensity().compute(at);

        assertNotEquals(plain, wrapped,
                "the wrapped and unwrapped routers agree at the probe point, so the fold changed nothing");
    }

    @Test
    void aWholeChunkOfNoiseComesOutDifferentOnAWrappedLevel() {
        RandomState folded = foldedState();
        Terrain wrapped = GenerationTransformerContext.withTransformer(SQUARE, () -> fillOneChunk(folded));
        Terrain plain = fillOneChunk(unfoldedState());

        assertTrue(wrapped.placed() > 0, "the wrapped chunk came out empty, so no folded sample reached a block");
        assertTrue(plain.placed() > 0, "the control chunk came out empty, so there is nothing to compare against");
        assertNotEquals(plain.hash(), wrapped.hash(),
                "a whole chunk of folded noise matches the unwrapped one, so the fold reaches no block of it");
    }

    private static RandomState foldedState() {
        return GenerationTransformerContext.withRouterBuild(SQUARE, () -> RandomState.create(overworld, noises, SEED));
    }

    private static RandomState unfoldedState() {
        return GenerationTransformerContext.withRouterBuild(null, () -> RandomState.create(overworld, noises, SEED));
    }

    private static Terrain fillOneChunk(RandomState state) {
        NoiseSettings noiseSettings = overworld.noiseSettings();
        int cellWidth = noiseSettings.getCellWidth();
        int cellHeight = noiseSettings.getCellHeight();
        int cellCountXZ = CHUNK_BLOCKS / cellWidth;
        int cellCountY = Math.floorDiv(noiseSettings.height(), cellHeight);
        int cellMinY = Math.floorDiv(noiseSettings.minY(), cellHeight);
        int chunkStartBlockX = PROBE_CHUNK_X * CHUNK_BLOCKS;
        int chunkStartBlockZ = PROBE_CHUNK_Z * CHUNK_BLOCKS;

        InterpolatedNoiseChunk noiseChunk = new InterpolatedNoiseChunk(cellCountXZ, state,
                chunkStartBlockX, chunkStartBlockZ, noiseSettings, Beardifier.EMPTY, overworld,
                globalFluidPicker(), Blender.empty());

        int placed = 0;
        int hash = 1;
        noiseChunk.initializeForFirstCellX();
        for (int cellXIndex = 0; cellXIndex < cellCountXZ; cellXIndex++) {
            noiseChunk.advanceCellX(cellXIndex);

            for (int cellZIndex = 0; cellZIndex < cellCountXZ; cellZIndex++) {
                for (int cellYIndex = cellCountY - 1; cellYIndex >= 0; cellYIndex--) {
                    noiseChunk.selectCellYZ(cellYIndex, cellZIndex);

                    for (int yInCell = cellHeight - 1; yInCell >= 0; yInCell--) {
                        int posY = (cellMinY + cellYIndex) * cellHeight + yInCell;
                        noiseChunk.updateForY(posY, (double) yInCell / cellHeight);

                        for (int xInCell = 0; xInCell < cellWidth; xInCell++) {
                            int posX = chunkStartBlockX + cellXIndex * cellWidth + xInCell;
                            noiseChunk.updateForX(posX, (double) xInCell / cellWidth);

                            for (int zInCell = 0; zInCell < cellWidth; zInCell++) {
                                int posZ = chunkStartBlockZ + cellZIndex * cellWidth + zInCell;
                                noiseChunk.updateForZ(posZ, (double) zInCell / cellWidth);

                                BlockState state1 = noiseChunk.interpolatedState();
                                hash = TERRAIN_HASH_FACTOR * hash
                                        + (state1 == null ? 0 : System.identityHashCode(state1));
                                if (state1 != null) {
                                    placed++;
                                }
                            }
                        }
                    }
                }
            }
        }

        noiseChunk.stopInterpolation();
        return new Terrain(placed, hash);
    }

    private static Aquifer.FluidPicker globalFluidPicker() {
        Aquifer.FluidStatus lava = new Aquifer.FluidStatus(LAVA_LEVEL, Blocks.LAVA.defaultBlockState());
        int seaLevel = overworld.seaLevel();
        Aquifer.FluidStatus sea = new Aquifer.FluidStatus(seaLevel, overworld.defaultFluid());
        Aquifer.FluidStatus empty =
                new Aquifer.FluidStatus(DimensionType.MIN_Y * 2, Blocks.AIR.defaultBlockState());

        return (x, y, z) -> {
            if (SharedConstants.DEBUG_DISABLE_FLUID_GENERATION) {
                return empty;
            }

            return y < Math.min(LAVA_LEVEL, seaLevel) ? lava : sea;
        };
    }

    private static final class InterpolatedNoiseChunk extends NoiseChunk {
        InterpolatedNoiseChunk(int cellCountXZ, RandomState randomState, int chunkMinBlockX, int chunkMinBlockZ,
                NoiseSettings noiseSettings, DensityFunctions.BeardifierOrMarker beardifier,
                NoiseGeneratorSettings settings, Aquifer.FluidPicker globalFluidPicker, Blender blender) {
            super(cellCountXZ, randomState, chunkMinBlockX, chunkMinBlockZ, noiseSettings, beardifier, settings,
                    globalFluidPicker, blender);
        }

        @Nullable BlockState interpolatedState() {
            return this.getInterpolatedState();
        }
    }
}
