package com.toroidalworld.core;

import static com.toroidalworld.core.WorldFoldFixture.EVEN;
import static com.toroidalworld.core.WorldFoldFixture.ODD;
import static com.toroidalworld.core.WorldFoldFixture.UNEVEN;
import static com.toroidalworld.core.WorldFoldFixture.UNIT;
import static com.toroidalworld.core.WorldFoldFixture.X_ONLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;

class BlockChunkAgreementTest {
    private static final long SEED = 0xB10CL;
    private static final int SAMPLES = 2000;

    private static final int[] HEIGHTS = {-64, -1, 0, 319};

    private static final List<WorldFold> TRANSFORMERS = List.of(EVEN, ODD, UNEVEN, UNIT, X_ONLY, WorldFolds.NOOP);

    private static int sampleBlock(Random random, WrapDomain blockDomain) {
        int reach = 3 * (blockDomain instanceof WrapDomain.Noop ? 16_000 : Math.min(blockDomain.domainLength, 16_000));
        return random.nextInt(2 * reach + 1) - reach;
    }

    private static int sampleHeight(Random random) {
        return random.nextInt(384) - 64;
    }

    private static String on(String axis, WorldFold transformer) {
        return "on " + axis + " in " + transformer;
    }

    private static String in(WorldFold transformer) {
        return "in " + transformer;
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

    @Test
    void aFoldedPositionKeysOntoASectionTheWorldHas() {
        Random random = new Random(SEED);
        for (WorldFold transformer : TRANSFORMERS) {
            for (int i = 0; i < SAMPLES; i++) {
                checkKey(new BlockPos(
                        sampleBlock(random, blockX(transformer)),
                        sampleHeight(random),
                        sampleBlock(random, blockZ(transformer))), transformer);
            }
        }
    }

    @Test
    void theBoundsAndWholeWidthsOutKeyLikeAnyOtherPosition() {
        for (WorldFold transformer : TRANSFORMERS) {
            for (int x : edges(blockX(transformer))) {
                for (int z : edges(blockZ(transformer))) {
                    for (int y : HEIGHTS) {
                        checkKey(new BlockPos(x, y, z), transformer);
                    }
                }
            }
        }
    }

    private static void checkKey(BlockPos pos, WorldFold transformer) {
        BlockPos folded = transformer.fold(pos);
        long section = SectionPos.asLong(folded);

        assertFalse(chunkX(transformer).isOver(SectionPos.x(section)),
                () -> "wrap(" + pos + ") named a section past the X bounds " + in(transformer));
        assertFalse(chunkZ(transformer).isOver(SectionPos.z(section)),
                () -> "wrap(" + pos + ") named a section past the Z bounds " + in(transformer));
        assertEquals(SectionPos.sectionRelativePos(pos), SectionPos.sectionRelativePos(folded),
                () -> "wrap(" + pos + ") moved inside its section " + in(transformer));
    }
}
