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

    private static final int WIDTH_CHUNKS = 64;
    private static final int WIDTH_BLOCKS = WIDTH_CHUNKS * 16;

    private static ToroidalShape torus(int minChunk, int maxChunk) {
        AxisBounds.Looped looped = new AxisBounds.Looped(minChunk, maxChunk);
        return TestShapes.of(
                WorldFolds.of(FlatShape.torus(new WorldLoopBounds(looped, looped))));
    }

    private static ToroidalShape cylinder(int minChunk, int maxChunk) {
        AxisBounds.Looped looped = new AxisBounds.Looped(minChunk, maxChunk);
        return TestShapes.of(WorldFolds.of(
                FlatShape.torus(new WorldLoopBounds(looped, AxisBounds.Unbounded.INSTANCE))));
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
            assertEquals(WORLD, DhFold.maxExactDetailLevel(torus(0, WIDTH_CHUNKS)));
        }

        @Test
        void anOddWidthStopsAtTheLargestPowerOfTwoDividingIt() {
            assertEquals(LEAF, DhFold.maxExactDetailLevel(torus(0, 100)));
        }

        @Test
        void theUnboundedAxisNeverLimits() {
            assertEquals(WORLD, DhFold.maxExactDetailLevel(cylinder(0, WIDTH_CHUNKS)));
        }

        @Test
        void aShapeLoopingOnNoAxisNeverLimits() {
            assertEquals(Byte.MAX_VALUE, DhFold.maxExactDetailLevel(TestShapes.of(WorldFolds.NOOP)));
        }
    }

    @Nested
    class TheWidestSectionThatMayRender {
        @Test
        void aWorldOf1024BlocksRendersSectionsOf1024Blocks() {
            assertEquals(WORLD, DhFold.maxRenderableDetailLevel(torus(0, WIDTH_CHUNKS), LEAF));
        }

        @Test
        void aWorldOf1664BlocksRendersSectionsOf128Blocks() {
            assertEquals(7, DhFold.maxRenderableDetailLevel(torus(0, 104), LEAF));
        }

        @Test
        void aWorldOf1600BlocksRendersOnlyTheLeaf() {
            assertEquals(LEAF, DhFold.maxRenderableDetailLevel(torus(0, 100), LEAF));
        }

        @Test
        void aWorldOf1616BlocksNoSectionDividesStillRendersTheLeaf() {
            assertEquals(LEAF, DhFold.maxRenderableDetailLevel(torus(0, 101), LEAF));
        }

        @Test
        void aWorldOf800BlocksNoSectionDividesStillRendersTheLeaf() {
            assertEquals(LEAF, DhFold.maxRenderableDetailLevel(torus(0, 50), LEAF));
        }
    }

    @Nested
    class AlignmentCountsAsMuchAsWidth {
        @Test
        void aWorldWhoseOriginIsOffTheGridStopsAtTheLevelItsOriginAllows() {
            assertEquals(LEAF, DhFold.maxExactDetailLevel(torus(-52, 52)));
        }

        @Test
        void theSameWidthStartingOnTheGridReachesTheLevelItsWidthAllows() {
            assertEquals(7, DhFold.maxExactDetailLevel(torus(0, 104)));
        }

        @Test
        void aWorldStartingAtZeroIsLimitedByItsWidthAlone() {
            assertEquals(WORLD, DhFold.maxExactDetailLevel(torus(0, WIDTH_CHUNKS)));
        }
    }

    @Nested
    class KeysFoldOnlyWhereTheLeafDividesTheWorld {
        @Test
        void aWidthTheLeafDividesFoldsWithoutCollision() {
            assertTrue(DhFold.keysFoldWithoutCollision(torus(0, 104), LEAF));
            assertTrue(DhFold.keysFoldWithoutCollision(torus(0, WIDTH_CHUNKS), LEAF));
        }

        @Test
        void aWidthTheLeafDoesNotDivideCollides() {
            assertFalse(DhFold.keysFoldWithoutCollision(torus(0, 101), LEAF));
            assertFalse(DhFold.keysFoldWithoutCollision(torus(0, 50), LEAF));
        }

        @Test
        void theUnboundedAxisOfACylinderNeverBlocksTheFold() {
            assertTrue(DhFold.keysFoldWithoutCollision(cylinder(0, 104), LEAF));
        }
    }

    @Nested
    class KeysAboveTheExactLevelStayRaw {
        private static final byte SECTION_512 = 9;

        private final ToroidalShape tiny = torus(-16, 16);

        @Test
        void aWorldCentredOnZeroIsExactUpToItsHalfWidth() {
            assertEquals(CHUNK_16, DhFold.maxExactDetailLevel(tiny));
        }

        @Test
        void aSectionAboveTheExactLevelIsItsOwnKey() {
            assertEquals(-1, DhFold.foldSection(tiny, Direction.Axis.X, SECTION_512, -1));
            assertEquals(0, DhFold.foldSection(tiny, Direction.Axis.X, SECTION_512, 0));
        }

        @Test
        void aSectionAtTheExactLevelStillFolds() {
            assertEquals(0, DhFold.foldSection(tiny, Direction.Axis.X, CHUNK_16, -2));
            assertEquals(-1, DhFold.foldSection(tiny, Direction.Axis.X, CHUNK_16, -1));
        }
    }

    @Nested
    class ASectionIsAddressableOrItIsNotDrawn {
        private static final byte SECTION_256 = 8;

        @Test
        void aSectionInsideTheWorldIsAddressableAtEveryLevel() {
            ToroidalShape shape = torus(0, 101);
            assertTrue(DhFold.isAddressableSection(shape, LEAF, 5, 5));
            assertTrue(DhFold.isAddressableSection(shape, SECTION_256, 1, 1));
        }

        @Test
        void aSectionPastTheSeamIsAddressableWhenItsWidthDividesTheWorld() {
            assertTrue(DhFold.isAddressableSection(torus(0, 104), LEAF, 26, 26));
        }

        @Test
        void aSectionPastTheSeamIsNotAddressableWhenItsWidthDoesNot() {
            ToroidalShape shape = torus(0, 101);
            assertFalse(DhFold.foldKeepsTheGrid(shape, Direction.Axis.X, LEAF, 26));
            assertFalse(DhFold.isAddressableSection(shape, LEAF, 26, 26));
        }

        @Test
        void aSectionHangingOverTheWorldEdgeIsNotAddressable() {
            ToroidalShape shape = torus(0, 101);
            assertTrue(DhFold.foldKeepsTheGrid(shape, Direction.Axis.X, LEAF, 25));
            assertFalse(DhFold.foldKeepsTheSpan(shape, Direction.Axis.X, LEAF, 25));
            assertFalse(DhFold.isAddressableSection(shape, LEAF, 25, 25));
        }

        @Test
        void aCoarseSectionInsideAWorldItDoesNotDivideIsStillAddressable() {
            ToroidalShape shape = torus(0, 104);
            assertTrue(DhFold.isAddressableSection(shape, SECTION_256, 5, 5));
            assertFalse(DhFold.foldKeepsTheSpan(shape, Direction.Axis.X, SECTION_256, 6));
        }

        @Test
        void theUnboundedAxisOfACylinderIsAlwaysAddressable() {
            assertTrue(DhFold.foldKeepsTheGrid(cylinder(0, 101), Direction.Axis.Z, LEAF, 9999));
            assertTrue(DhFold.foldKeepsTheSpan(cylinder(0, 101), Direction.Axis.Z, LEAF, 9999));
        }
    }

    @Nested
    class TheCapReachesDhInItsOwnUnit {
        @Test
        void aWorldOf1024BlocksAllowsSectionsOf1024Blocks() {
            assertEquals(4, DhFold.maxExpectedDetailLevel(torus(0, WIDTH_CHUNKS), LEAF));
        }

        @Test
        void aWorldOf1664BlocksAllowsSectionsOf128Blocks() {
            assertEquals(1, DhFold.maxExpectedDetailLevel(torus(0, 104), LEAF));
        }

        @Test
        void aWorldOf1600BlocksAllowsOnlyTheLeaf() {
            assertEquals(0, DhFold.maxExpectedDetailLevel(torus(0, 100), LEAF));
        }

        @Test
        void aWorldNoSectionDividesStopsAtTheLeafRatherThanBelowIt() {
            assertEquals(0, DhFold.maxExpectedDetailLevel(torus(0, 101), LEAF));
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

        @Test
        void aSectionAboveTheExactLevelKeepsItsRawKey() {
            ToroidalShape tiny = torus(-16, 16);
            assertEquals(-1, DhFold.nearestSection(tiny, Direction.Axis.X, (byte) 9, 0, -1));
        }
    }

    @Nested
    class OnlyTheNearestCopyIsDrawn {
        private final ToroidalShape shape = torus(0, WIDTH_CHUNKS);

        @Test
        void aSectionJustAcrossTheSeamIsDrawnAndItsFarCopyIsNot() {
            int sectionsPerWorld = WIDTH_BLOCKS / DhFold.sectionWidthBlocks(LEAF);
            assertTrue(DhFold.isNearestSection(shape, Direction.Axis.X, LEAF, 60, 4));
            assertFalse(DhFold.isNearestSection(shape, Direction.Axis.X, LEAF, 60, 4 - sectionsPerWorld));
        }

        @Test
        void theAntipodeTieDrawsThePositiveSide() {
            assertTrue(DhFold.isNearestSection(shape, Direction.Axis.X, LEAF, 32, 8));
            assertFalse(DhFold.isNearestSection(shape, Direction.Axis.X, LEAF, 32, -8));
        }

        @Test
        void aSectionAboveTheExactLevelIsStillJudgedByItsCentre() {
            ToroidalShape tiny = torus(-16, 16);
            assertTrue(DhFold.isNearestSection(tiny, Direction.Axis.X, (byte) 9, 0, 0));
            assertFalse(DhFold.isNearestSection(tiny, Direction.Axis.X, (byte) 9, 0, -1));
        }

        @Test
        void theUnboundedAxisOfACylinderNeverCulls() {
            ToroidalShape cylinder = cylinder(0, WIDTH_CHUNKS);
            assertTrue(DhFold.isNearestSection(cylinder, Direction.Axis.Z, LEAF, 0, 5 * WIDTH_BLOCKS));
            assertFalse(DhFold.isNearestSection(cylinder, Direction.Axis.X, LEAF, 0, WIDTH_BLOCKS / 64));
        }
    }

    @Nested
    class TheReloadLandsOnTheCopyTheGateKeeps {
        private static final int SECTIONS_PER_WORLD = WIDTH_BLOCKS / DhFold.sectionWidthBlocks(LEAF);

        private final ToroidalShape shape = torus(0, WIDTH_CHUNKS);

        @Test
        void aSectionWhoseCornerIsNearButWhoseCentreIsFarIsSeatedOnTheFarSide() {
            int ref = 470;
            int section = SECTIONS_PER_WORLD - 1;
            assertFalse(DhFold.isNearestSection(shape, Direction.Axis.X, LEAF, ref, section));
            assertTrue(DhFold.isNearestSection(shape, Direction.Axis.X, LEAF, ref, section - SECTIONS_PER_WORLD));
            assertEquals(section - SECTIONS_PER_WORLD, DhFold.nearestSection(shape, Direction.Axis.X, LEAF, ref, section));
        }

        @Test
        void everyLapCopyOfASectionResolvesToTheOneCopyTheGateKeeps() {
            for (int ref = -600; ref <= 600; ref += 37) {
                for (int section = 0; section < SECTIONS_PER_WORLD; section++) {
                    int kept = DhFold.nearestSection(shape, Direction.Axis.X, LEAF, ref, section);
                    int drawn = 0;
                    for (int lap = -2; lap <= 2; lap++) {
                        int copy = section + lap * SECTIONS_PER_WORLD;
                        if (DhFold.isNearestSection(shape, Direction.Axis.X, LEAF, ref, copy)) {
                            drawn++;
                            assertEquals(kept, copy, "ref " + ref + " section " + section);
                        }
                        assertEquals(kept, DhFold.nearestSection(shape, Direction.Axis.X, LEAF, ref, copy));
                    }
                    assertEquals(1, drawn, "ref " + ref + " section " + section);
                }
            }
        }
    }

    @Nested
    class TheTreeHoldsOneLapAroundItsCentre {
        private static final int LEAF_WIDTH = DhFold.sectionWidthBlocks(LEAF);
        private static final int REF = 100;
        private static final int HALF = WIDTH_BLOCKS / 2;

        private final ToroidalShape shape = torus(0, WIDTH_CHUNKS);

        @Test
        void aSectionInsideTheLapIsHeld() {
            assertTrue(DhFold.overlapsNearestLap(shape, Direction.Axis.X, REF, 512, 4 * LEAF_WIDTH));
        }

        @Test
        void aSectionPastHalfAWorldIsRefusedAndItsNearCopyIsHeld() {
            assertFalse(DhFold.overlapsNearestLap(shape, Direction.Axis.X, REF, 640, LEAF_WIDTH));
            assertTrue(DhFold.overlapsNearestLap(shape, Direction.Axis.X, REF, 640 - WIDTH_BLOCKS, LEAF_WIDTH));
        }

        @Test
        void aSectionTouchingTheLapEdgeFromOutsideIsRefused() {
            assertFalse(DhFold.overlapsNearestLap(shape, Direction.Axis.X, REF, REF + HALF, LEAF_WIDTH));
            assertFalse(DhFold.overlapsNearestLap(shape, Direction.Axis.X, REF, REF - HALF - LEAF_WIDTH, LEAF_WIDTH));
            assertTrue(DhFold.overlapsNearestLap(shape, Direction.Axis.X, REF, REF + HALF - 1, LEAF_WIDTH));
        }

        @Test
        void aSectionWiderThanTheWorldIsHeldOnlyWhereItCoversTheLap() {
            assertTrue(DhFold.overlapsNearestLap(shape, Direction.Axis.X, REF, 0, 4 * WIDTH_BLOCKS));
            assertFalse(DhFold.overlapsNearestLap(shape, Direction.Axis.X, REF, 4 * WIDTH_BLOCKS, 4 * WIDTH_BLOCKS));
        }

        @Test
        void theLoopingAxisOfACylinderRefusesPastHalfAWorld() {
            ToroidalShape cylinder = cylinder(0, WIDTH_CHUNKS);
            assertFalse(DhFold.overlapsNearestLap(cylinder, Direction.Axis.X, REF, REF + HALF, LEAF_WIDTH));
        }

        @Test
        void theUnboundedAxisOfACylinderHoldsAnySection() {
            ToroidalShape cylinder = cylinder(0, WIDTH_CHUNKS);
            assertTrue(DhFold.overlapsNearestLap(cylinder, Direction.Axis.Z, REF, 9999 * LEAF_WIDTH, LEAF_WIDTH));
            assertTrue(DhFold.overlapsNearestLap(cylinder, Direction.Axis.Z, REF, -9999 * LEAF_WIDTH, LEAF_WIDTH));
        }
    }
}
