package com.toroidalworld.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

// Storage keyed by section is written by folding the position it was addressed with and letting vanilla name the
// section from that — which is what the POI manager does with the positions it is handed. Two things have to hold for
// that fold to be a rename rather than a move: the section it names must be one the world actually has, or the storage
// grows a section for ground that does not exist and then saves it; and the slot the position takes inside that section
// must be the one it arrived with, or a folded record lands on top of another one.
//
// Both go through vanilla's own SectionPos packing rather than arithmetic of our own, so a change to how a section is
// named, or to how a position is packed inside one, fails here rather than in a world. The per-axis fold underneath is
// BlockChunkAgreementTest's; what this test alone guards is what vanilla makes of a whole BlockPos after it.
class SectionKeyAgreementTest {
    private static final long SEED = 0x5EC7L;
    private static final int SAMPLES = 2000;

    // Heights either side of a section floor and at both ends of the vanilla column — Y carries no seam and must come
    // through the fold untouched, packing included.
    private static final int[] HEIGHTS = {-64, -1, 0, 319};

    // The suite's standard shapes — even centered, odd, uneven split with unequal axis widths, one chunk wide, a
    // looped axis beside an unbounded one, and the fully disabled transformer.
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

    @Test
    void aFoldedPositionKeysOntoASectionTheWorldHas() {
        Random random = new Random(SEED);
        for (WorldLoopTransformer transformer : TRANSFORMERS) {
            for (int i = 0; i < SAMPLES; i++) {
                checkKey(new BlockPos(
                        sampleBlock(random, transformer.coords.x),
                        sampleHeight(random),
                        sampleBlock(random, transformer.coords.z)), transformer);
            }
        }
    }

    @Test
    void theBoundsAndWholeWidthsOutKeyLikeAnyOtherPosition() {
        for (WorldLoopTransformer transformer : TRANSFORMERS) {
            for (int x : edges(transformer.coords.x)) {
                for (int z : edges(transformer.coords.z)) {
                    for (int y : HEIGHTS) {
                        checkKey(new BlockPos(x, y, z), transformer);
                    }
                }
            }
        }
    }

    private static void checkKey(BlockPos pos, WorldLoopTransformer transformer) {
        BlockPos folded = transformer.blocks.wrap(pos);
        long section = SectionPos.asLong(folded);

        assertFalse(transformer.chunks.x.isOver(SectionPos.x(section)),
                () -> "wrap(" + pos + ") named a section past the X bounds " + in(transformer));
        assertFalse(transformer.chunks.z.isOver(SectionPos.z(section)),
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

    // The block edges with their neighbours, and the same spots whole worlds out — where a lap miscount or an
    // off-by-one lands first.
    private static int[] edges(WrapDomain blockDomain) {
        int width = blockDomain.domainLength;
        return new int[] {
                blockDomain.lowerBound - 1, blockDomain.lowerBound, blockDomain.lowerBound + 1,
                blockDomain.upperBound - 1, blockDomain.upperBound, blockDomain.upperBound + 1,
                blockDomain.lowerBound - 3 * width, blockDomain.upperBound - 1 + 3 * width};
    }

    private static String in(WorldLoopTransformer transformer) {
        return "in " + transformer;
    }
}
