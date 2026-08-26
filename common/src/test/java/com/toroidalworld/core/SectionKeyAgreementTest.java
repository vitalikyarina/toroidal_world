package com.toroidalworld.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;

class SectionKeyAgreementTest {
    private static final long SEED = 0x5EC7L;
    private static final int SAMPLES = 2000;

    private static final int[] HEIGHTS = {-64, -1, 0, 319};

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

    private static int sampleBlock(Random random, WrapDomain blockDomain) {
        int reach = 3 * (blockDomain instanceof WrapDomain.Noop ? 16_000 : Math.min(blockDomain.domainLength, 16_000));
        return random.nextInt(2 * reach + 1) - reach;
    }

    private static int sampleHeight(Random random) {
        return random.nextInt(384) - 64;
    }

    private static int[] edges(WrapDomain blockDomain) {
        int width = blockDomain.domainLength;
        return new int[] {
                blockDomain.lowerBound - 1, blockDomain.lowerBound, blockDomain.lowerBound + 1,
                blockDomain.upperBound - 1, blockDomain.upperBound, blockDomain.upperBound + 1,
                blockDomain.lowerBound - 3 * width, blockDomain.upperBound - 1 + 3 * width};
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

    private static String in(WorldFold transformer) {
        return "in " + transformer;
    }
}
