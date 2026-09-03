package com.toroidalworld.compat.distanthorizons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.toroidalworld.api.ToroidalShape;
import com.toroidalworld.api.TestShapes;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.Direction;

class DhFoldTest {
    private static final byte LEAF = 6;
    private static final byte CHUNK_16 = 8;
    private static final byte WORLD = 10;
    private static final byte OVER_WORLD = 11;

    private static final int WIDTH_CHUNKS = 64;
    private static final int WIDTH_BLOCKS = WIDTH_CHUNKS * 16;

    private static ToroidalShape torus(int minChunk, int maxChunk) {
        AxisBounds.Looped looped = new AxisBounds.Looped(minChunk, maxChunk);
        return TestShapes.of(
                WorldFolds.of(FlatShape.latticeTorus(new WorldLoopBounds(looped, looped), FlatShape.NO_SKEW)));
    }

    private static ToroidalShape cylinder(int minChunk, int maxChunk) {
        AxisBounds.Looped looped = new AxisBounds.Looped(minChunk, maxChunk);
        return TestShapes.of(WorldFolds.of(
                FlatShape.latticeTorus(new WorldLoopBounds(looped, AxisBounds.Unbounded.INSTANCE), FlatShape.NO_SKEW)));
    }

    @Nested
    class SectionsFoldByTheirMinCorner {
        private final ToroidalShape shape = torus(0, WIDTH_CHUNKS);

        @Test
        void aLeafPastTheSeamLandsOnTheFirstLeafOfTheWorld() {
            int sectionsPerWorld = WIDTH_BLOCKS / DhFold.sectionWidthBlocks(LEAF);
            assertEquals(0, DhFold.foldSection(shape, Direction.Axis.X, LEAF, sectionsPerWorld));
            assertEquals(sectionsPerWorld - 1, DhFold.foldSection(shape, Direction.Axis.X, LEAF, -1));
            assertEquals(3, DhFold.foldSection(shape, Direction.Axis.Z, LEAF, 3 + 2 * sectionsPerWorld));
        }

        @Test
        void aSectionInsideTheWorldIsItsOwnKey() {
            assertEquals(5, DhFold.foldSection(shape, Direction.Axis.X, LEAF, 5));
            assertEquals(3, DhFold.foldSection(shape, Direction.Axis.X, CHUNK_16, 3));
            assertEquals(0, DhFold.foldSection(shape, Direction.Axis.X, WORLD, 0));
        }

        @Test
        void aSectionAsWideAsTheWorldFoldsOntoTheOne() {
            assertEquals(0, DhFold.foldSection(shape, Direction.Axis.X, WORLD, 1));
            assertEquals(0, DhFold.foldSection(shape, Direction.Axis.X, WORLD, -1));
        }

        @Test
        void theUnboundedAxisOfACylinderPassesThrough() {
            ToroidalShape cylinder = cylinder(0, WIDTH_CHUNKS);
            assertEquals(-7, DhFold.foldSection(cylinder, Direction.Axis.Z, LEAF, -7));
            assertEquals(0, DhFold.foldSection(cylinder, Direction.Axis.X, LEAF, WIDTH_BLOCKS / 64));
        }

        @Test
        void aWorldNotStartingAtZeroFoldsIntoItsOwnSpan() {
            ToroidalShape shifted = torus(-32, 32);
            int sectionsPerWorld = WIDTH_BLOCKS / DhFold.sectionWidthBlocks(LEAF);
            assertEquals(-sectionsPerWorld / 2, DhFold.foldSection(shifted, Direction.Axis.X, LEAF, sectionsPerWorld / 2));
            assertEquals(sectionsPerWorld / 2 - 1, DhFold.foldSection(shifted, Direction.Axis.X, LEAF, -sectionsPerWorld / 2 - 1));
        }
    }

    @Nested
    class ExactnessIsDivisibility {
        @Test
        void aPowerOfTwoWorldFoldsEveryLevelUpToItsOwnWidth() {
            ToroidalShape shape = torus(0, WIDTH_CHUNKS);
            assertTrue(DhFold.foldsExactly(shape, Direction.Axis.X, LEAF));
            assertTrue(DhFold.foldsExactly(shape, Direction.Axis.X, WORLD));
            assertFalse(DhFold.foldsExactly(shape, Direction.Axis.X, OVER_WORLD));
            assertEquals(WORLD, DhFold.maxExactDetailLevel(shape));
        }

        @Test
        void anOddWidthStopsAtTheLargestPowerOfTwoDividingIt() {
            ToroidalShape shape = torus(0, 100);
            assertTrue(DhFold.foldsExactly(shape, Direction.Axis.X, LEAF));
            assertFalse(DhFold.foldsExactly(shape, Direction.Axis.X, (byte) 7));
            assertEquals(6, DhFold.maxExactDetailLevel(shape));
        }

        @Test
        void theUnboundedAxisNeverLimits() {
            ToroidalShape cylinder = cylinder(0, WIDTH_CHUNKS);
            assertTrue(DhFold.foldsExactly(cylinder, Direction.Axis.Z, OVER_WORLD));
            assertEquals(WORLD, DhFold.maxExactDetailLevel(cylinder));
        }
    }

    @Nested
    class TheNearestSectionFollowsTheReference {
        private final ToroidalShape shape = torus(0, WIDTH_CHUNKS);

        @Test
        void aCanonicalSectionBehindTheSeamIsSeatedBesideTheReference() {
            int sectionsPerWorld = WIDTH_BLOCKS / DhFold.sectionWidthBlocks(LEAF);
            assertEquals(-1, DhFold.nearestSection(shape, Direction.Axis.X, LEAF, 10, sectionsPerWorld - 1));
            assertEquals(sectionsPerWorld, DhFold.nearestSection(shape, Direction.Axis.X, LEAF, WIDTH_BLOCKS - 10, 0));
        }

        @Test
        void aSectionAlreadyNearTheReferenceStays() {
            assertEquals(4, DhFold.nearestSection(shape, Direction.Axis.X, LEAF, 300, 4));
        }
    }

    @Nested
    class OnlyTheNearestCopyIsDrawn {
        private final ToroidalShape shape = torus(0, WIDTH_CHUNKS);

        @Test
        void aSectionJustAcrossTheSeamIsDrawnAndItsFarCopyIsNot() {
            assertTrue(DhFold.isNearestCopy(shape, 60, 0, 288, 32));
            assertFalse(DhFold.isNearestCopy(shape, 60, 0, 288 - WIDTH_BLOCKS, 32));
        }

        @Test
        void theAntipodeTieDrawsExactlyOneCopy() {
            boolean plus = DhFold.isNearestCopy(shape, 0, 0, WIDTH_BLOCKS / 2, 0);
            boolean minus = DhFold.isNearestCopy(shape, 0, 0, -WIDTH_BLOCKS / 2, 0);
            assertTrue(plus != minus, "both copies of the antipode section were drawn, or neither");
        }

        @Test
        void theUnboundedAxisOfACylinderNeverCulls() {
            ToroidalShape cylinder = cylinder(0, WIDTH_CHUNKS);
            assertTrue(DhFold.isNearestCopy(cylinder, 0, 0, 32, 5 * WIDTH_BLOCKS));
            assertFalse(DhFold.isNearestCopy(cylinder, 0, 0, 32 + WIDTH_BLOCKS, 0));
        }
    }

    @Test
    void theRadiusCapIsHalfTheNarrowestLoopingAxis() {
        assertEquals(WIDTH_CHUNKS / 2, DhFold.radiusCapChunks(torus(0, WIDTH_CHUNKS)));
        assertEquals(WIDTH_CHUNKS / 2, DhFold.radiusCapChunks(cylinder(0, WIDTH_CHUNKS)));
        AxisBounds.Looped narrow = new AxisBounds.Looped(0, 32);
        AxisBounds.Looped wide = new AxisBounds.Looped(0, 128);
        ToroidalShape uneven = TestShapes.of(
                WorldFolds.of(FlatShape.latticeTorus(new WorldLoopBounds(narrow, wide), FlatShape.NO_SKEW)));
        assertEquals(16, DhFold.radiusCapChunks(uneven));
    }
}
