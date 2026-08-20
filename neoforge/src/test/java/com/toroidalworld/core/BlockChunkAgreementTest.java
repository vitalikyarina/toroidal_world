package com.toroidalworld.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;

import net.minecraft.core.SectionPos;

class BlockChunkAgreementTest {
    private static final long SEED = 0xB10CL;
    private static final int SAMPLES = 2000;

    private static final List<WorldLoopTransformer> TRANSFORMERS = List.of(
            transformer(-32, 32, -32, 32),
            transformer(-2, 3, -2, 3),
            transformer(-48, 16, 0, 16),
            transformer(0, 1, 0, 1),
            new WorldLoopTransformer(
                    new WorldLoopBounds(new AxisBounds.Looped(-32, 32), AxisBounds.Unbounded.INSTANCE)),
            WorldLoopTransformer.NOOP);

    private static WorldLoopTransformer transformer(int xChunkMin, int xChunkMax, int zChunkMin, int zChunkMax) {
        return new WorldLoopTransformer(new WorldLoopBounds(xChunkMin, xChunkMax, zChunkMin, zChunkMax));
    }

    private static int sampleBlock(Random random, WrapDomain blockDomain) {
        int reach = 3 * (blockDomain instanceof WrapDomain.Noop ? 16_000 : Math.min(blockDomain.domainLength, 16_000));
        return random.nextInt(2 * reach + 1) - reach;
    }

    private static String on(String axis, WorldLoopTransformer transformer) {
        return "on " + axis + " in " + transformer;
    }

    @Test
    void wrapThenConvertEqualsConvertThenWrap() {
        Random random = new Random(SEED);
        for (WorldLoopTransformer transformer : TRANSFORMERS) {
            for (int i = 0; i < SAMPLES; i++) {
                checkAgreement(transformer.coords.x, transformer.chunks.x,
                        sampleBlock(random, transformer.coords.x), "X", transformer);
                checkAgreement(transformer.coords.z, transformer.chunks.z,
                        sampleBlock(random, transformer.coords.z), "Z", transformer);
            }
        }
    }

    @Test
    void theBoundsAndWholeWidthsOutAgreeLikeAnyOtherCoordinate() {
        for (WorldLoopTransformer transformer : TRANSFORMERS) {
            for (int edge : edges(transformer.coords.x)) {
                checkAgreement(transformer.coords.x, transformer.chunks.x, edge, "X", transformer);
            }
            for (int edge : edges(transformer.coords.z)) {
                checkAgreement(transformer.coords.z, transformer.chunks.z, edge, "Z", transformer);
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
            String axis, WorldLoopTransformer transformer) {
        int wrappedBlock = blockDomain.wrap(blockCoord);
        assertEquals(chunkDomain.wrap(SectionPos.blockToSectionCoord(blockCoord)),
                SectionPos.blockToSectionCoord(wrappedBlock),
                () -> "wrap(" + blockCoord + ") landed in the wrong chunk " + on(axis, transformer));
        assertEquals(Math.floorMod(blockCoord, 16), Math.floorMod(wrappedBlock, 16),
                () -> "wrap(" + blockCoord + ") moved inside its chunk " + on(axis, transformer));
    }
}
