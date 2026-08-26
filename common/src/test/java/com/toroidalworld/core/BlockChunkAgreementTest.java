package com.toroidalworld.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;

import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;

class BlockChunkAgreementTest {
    private static final long SEED = 0xB10CL;
    private static final int SAMPLES = 2000;

    private static final List<WorldFold> TRANSFORMERS = List.of(
            transformer(-32, 32, -32, 32),
            transformer(-2, 3, -2, 3),
            transformer(-48, 16, 0, 16),
            transformer(0, 1, 0, 1),
            new WorldLoopTransformer(
                    new WorldLoopBounds(new AxisBounds.Looped(-32, 32), AxisBounds.Unbounded.INSTANCE)),
            WorldFolds.NOOP);

    private static WorldFold transformer(int xChunkMin, int xChunkMax, int zChunkMin, int zChunkMax) {
        return new WorldLoopTransformer(new WorldLoopBounds(xChunkMin, xChunkMax, zChunkMin, zChunkMax));
    }

    private static int sampleBlock(Random random, WrapDomain blockDomain) {
        int reach = 3 * (blockDomain instanceof WrapDomain.Noop ? 16_000 : Math.min(blockDomain.domainLength, 16_000));
        return random.nextInt(2 * reach + 1) - reach;
    }

    private static String on(String axis, WorldFold transformer) {
        return "on " + axis + " in " + transformer;
    }

    private static WrapDomain blockX(WorldFold fold) {
        return fold.blockDomain(Direction.Axis.X);
    }

    private static WrapDomain blockZ(WorldFold fold) {
        return fold.blockDomain(Direction.Axis.Z);
    }

    private static WrapDomain chunkX(WorldFold fold) {
        return fold.chunkDomain(Direction.Axis.X);
    }

    private static WrapDomain chunkZ(WorldFold fold) {
        return fold.chunkDomain(Direction.Axis.Z);
    }

    @Test
    void wrapThenConvertEqualsConvertThenWrap() {
        Random random = new Random(SEED);
        for (WorldFold transformer : TRANSFORMERS) {
            for (int i = 0; i < SAMPLES; i++) {
                checkAgreement(blockX(transformer), chunkX(transformer),
                        sampleBlock(random, blockX(transformer)), "X", transformer);
                checkAgreement(blockZ(transformer), chunkZ(transformer),
                        sampleBlock(random, blockZ(transformer)), "Z", transformer);
            }
        }
    }

    @Test
    void theBoundsAndWholeWidthsOutAgreeLikeAnyOtherCoordinate() {
        for (WorldFold transformer : TRANSFORMERS) {
            for (int edge : edges(blockX(transformer))) {
                checkAgreement(blockX(transformer), chunkX(transformer), edge, "X", transformer);
            }
            for (int edge : edges(blockZ(transformer))) {
                checkAgreement(blockZ(transformer), chunkZ(transformer), edge, "Z", transformer);
            }
        }
    }

    private static int[] edges(WrapDomain blockDomain) {
        int width = blockDomain.domainLength;
        return new int[] {
                blockDomain.lowerBound - 1, blockDomain.lowerBound, blockDomain.lowerBound + 1,
                blockDomain.upperBound - 1, blockDomain.upperBound, blockDomain.upperBound + 1,
                blockDomain.lowerBound - 3 * width, blockDomain.upperBound - 1 + 3 * width};
    }

    private static void checkAgreement(WrapDomain blockDomain, WrapDomain chunkDomain, int blockCoord,
            String axis, WorldFold transformer) {
        int wrappedBlock = blockDomain.wrap(blockCoord);
        assertEquals(chunkDomain.wrap(SectionPos.blockToSectionCoord(blockCoord)),
                SectionPos.blockToSectionCoord(wrappedBlock),
                () -> "wrap(" + blockCoord + ") landed in the wrong chunk " + on(axis, transformer));
        assertEquals(Math.floorMod(blockCoord, 16), Math.floorMod(wrappedBlock, 16),
                () -> "wrap(" + blockCoord + ") moved inside its chunk " + on(axis, transformer));
    }
}
