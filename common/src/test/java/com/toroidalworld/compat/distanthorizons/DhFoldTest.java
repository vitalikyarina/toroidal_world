package com.toroidalworld.compat.distanthorizons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
    private static final byte SNAP = 6;

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
    class KeysFoldByThePeriodOfTheirLevel {
        private static final int ODD_CHUNKS = 101;
        private static final int ODD_BLOCKS = ODD_CHUNKS * 16;
        private static final int ODD_LEAF_SECTIONS = 101;

        private final ToroidalShape odd = torus(0, ODD_CHUNKS);

        @Test
        void thePeriodIsTheSmallestWholeNumberOfLapsTheSectionGridDivides() {
            assertEquals(4 * ODD_BLOCKS, DhFold.periodBlocks(odd, Direction.Axis.X, LEAF));
            assertEquals(2 * 800, DhFold.periodBlocks(torus(0, 50), Direction.Axis.X, LEAF));
            assertEquals(1664, DhFold.periodBlocks(torus(0, 104), Direction.Axis.X, LEAF));
            assertEquals(ODD_LEAF_SECTIONS, DhFold.periodSections(odd, Direction.Axis.X, LEAF));
            assertEquals(25, DhFold.periodSections(torus(0, 50), Direction.Axis.X, LEAF));
            assertEquals(26, DhFold.periodSections(torus(0, 104), Direction.Axis.X, LEAF));
        }

        @Test
        void aLapThatLandsOffTheSectionGridKeepsItsOwnKeys() {
            assertEquals(26, DhFold.foldSection(odd, Direction.Axis.X, LEAF, 26));
            assertEquals(75, DhFold.foldSection(odd, Direction.Axis.X, LEAF, 75));
            assertEquals(100, DhFold.foldSection(odd, Direction.Axis.X, LEAF, -1));
            assertEquals(13, DhFold.foldSection(torus(0, 50), Direction.Axis.X, LEAF, 13));
        }

        @Test
        void theLapThatLandsOnTheGridFoldsOntoTheFirst() {
            assertEquals(0, DhFold.foldSection(odd, Direction.Axis.X, LEAF, ODD_LEAF_SECTIONS));
            assertEquals(5, DhFold.foldSection(odd, Direction.Axis.X, LEAF, 5 + 2 * ODD_LEAF_SECTIONS));
            assertEquals(0, DhFold.foldSection(torus(0, 50), Direction.Axis.X, LEAF, 25));
            assertEquals(0, DhFold.foldSection(torus(0, 104), Direction.Axis.X, LEAF, 26));
        }

        @Test
        void twoSectionsShareAKeyOnlyAWholeNumberOfWorldWidthsApart() {
            for (int section = -300; section <= 300; section++) {
                int key = DhFold.foldSection(odd, Direction.Axis.X, LEAF, section);
                assertTrue(0 <= key && key < ODD_LEAF_SECTIONS, "section " + section);
                assertEquals(key, DhFold.foldSection(odd, Direction.Axis.X, LEAF, section + ODD_LEAF_SECTIONS));
                for (int apart = 1; apart < ODD_LEAF_SECTIONS; apart++) {
                    assertNotEquals(key, DhFold.foldSection(odd, Direction.Axis.X, LEAF, section + apart),
                            "section " + section + " apart " + apart);
                }
            }
        }

        @Test
        void theChunkFoldLandsInTheFoldedSectionOfItsRawSection() {
            for (ToroidalShape shape : new ToroidalShape[] {odd, torus(-50, 50), torus(-16, 16)}) {
                for (int chunk = -500; chunk <= 500; chunk++) {
                    int folded = DhFold.foldChunk(shape, Direction.Axis.X, LEAF, chunk);
                    assertEquals(chunk & 3, folded & 3, "chunk " + chunk);
                    assertEquals(DhFold.foldSection(shape, Direction.Axis.X, LEAF, Math.floorDiv(chunk, 4)),
                            DhFold.foldSection(shape, Direction.Axis.X, LEAF, Math.floorDiv(folded, 4)),
                            "chunk " + chunk);
                }
            }
        }

        @Test
        void theUnboundedAxisOfACylinderHasNoPeriodToFoldBy() {
            ToroidalShape cylinder = cylinder(0, ODD_CHUNKS);
            assertEquals(9999, DhFold.foldSection(cylinder, Direction.Axis.Z, LEAF, 9999));
            assertEquals(9999, DhFold.foldChunk(cylinder, Direction.Axis.Z, LEAF, 9999));
            assertEquals(26, DhFold.foldSection(cylinder, Direction.Axis.X, LEAF, 26));
            assertEquals(0, DhFold.foldSection(cylinder, Direction.Axis.X, LEAF, ODD_LEAF_SECTIONS));
        }
    }

    @Nested
    class KeysAboveTheExactLevelFoldByAWiderPeriod {
        private static final byte SECTION_128 = 7;
        private static final byte SECTION_512 = 9;

        private final ToroidalShape tiny = torus(-16, 16);

        @Test
        void aWorldCentredOnZeroIsExactUpToItsHalfWidth() {
            assertEquals(CHUNK_16, DhFold.maxExactDetailLevel(tiny));
        }

        @Test
        void aSectionAsWideAsTheWorldFoldsOntoOneKeyWhateverItsLap() {
            assertEquals(512, DhFold.periodBlocks(tiny, Direction.Axis.X, SECTION_512));
            assertEquals(0, DhFold.foldSection(tiny, Direction.Axis.X, SECTION_512, -1));
            assertEquals(0, DhFold.foldSection(tiny, Direction.Axis.X, SECTION_512, 0));
            assertEquals(0, DhFold.foldSection(tiny, Direction.Axis.X, SECTION_512, 7));
        }

        @Test
        void aSectionAtTheExactLevelStillFolds() {
            assertEquals(0, DhFold.foldSection(tiny, Direction.Axis.X, CHUNK_16, -2));
            assertEquals(-1, DhFold.foldSection(tiny, Direction.Axis.X, CHUNK_16, -1));
        }

        @Test
        void aWorldTheSectionDoesNotDivideFoldsByWholeLapsOfIt() {
            ToroidalShape odd = torus(0, 101);
            assertEquals(8 * 1616, DhFold.periodBlocks(odd, Direction.Axis.X, SECTION_128));
            assertEquals(101, DhFold.periodSections(odd, Direction.Axis.X, SECTION_128));
            assertEquals(50, DhFold.foldSection(odd, Direction.Axis.X, SECTION_128, 50 + 101));
            assertEquals(2 * 1600, DhFold.periodBlocks(torus(-50, 50), Direction.Axis.X, SECTION_128));
        }
    }

    @Nested
    class ASectionIsCompleteOrItIsNotDrawn {
        private static final byte SECTION_128 = 7;
        private static final byte SECTION_256 = 8;

        private final ToroidalShape odd = torus(0, 101);

        @Test
        void aLeafIsCompleteWhereverItSits() {
            assertTrue(DhFold.isCompleteSection(odd, LEAF, LEAF, 25, 25));
            assertTrue(DhFold.isCompleteSection(odd, LEAF, LEAF, 26, 26));
            assertTrue(DhFold.isCompleteSection(odd, LEAF, LEAF, -1, 300));
        }

        @Test
        void aCoarseSectionInsideTheFirstLapIsComplete() {
            assertTrue(DhFold.isCompleteSection(odd, LEAF, SECTION_256, 1, 1));
            assertTrue(DhFold.isCompleteSection(odd, LEAF, SECTION_256, 5, 0));
            assertTrue(DhFold.isCompleteSection(torus(0, 104), LEAF, SECTION_256, 5, 5));
        }

        @Test
        void aCoarseSectionHangingOverTheWorldEdgeIsNot() {
            assertFalse(DhFold.foldedSpanInsideTheWorld(odd, Direction.Axis.X, SECTION_256, 6));
            assertFalse(DhFold.isCompleteSection(odd, LEAF, SECTION_256, 6, 1));
            assertFalse(DhFold.foldedSpanInsideTheWorld(torus(0, 104), Direction.Axis.X, SECTION_256, 6));
        }

        @Test
        void aCoarseSectionInAFarLapIsCompleteOnlyWhereItFoldsIntoTheFirst() {
            assertFalse(DhFold.foldedSpanInsideTheWorld(odd, Direction.Axis.X, SECTION_256, 7));
            assertFalse(DhFold.foldedSpanInsideTheWorld(odd, Direction.Axis.X, SECTION_256, 6 + 101));
            assertTrue(DhFold.foldedSpanInsideTheWorld(odd, Direction.Axis.X, SECTION_256, 1 + 101));
            assertTrue(DhFold.foldedSpanInsideTheWorld(torus(0, 104), Direction.Axis.X, SECTION_256, 13));
        }

        @Test
        void aStraddlerOfAnOffGridWorldEdgeIsIncompleteOnBothSides() {
            ToroidalShape shifted = torus(-50, 50);
            assertFalse(DhFold.foldedSpanInsideTheWorld(shifted, Direction.Axis.X, SECTION_128, 6));
            assertFalse(DhFold.foldedSpanInsideTheWorld(shifted, Direction.Axis.X, SECTION_128, -7));
            assertTrue(DhFold.foldedSpanInsideTheWorld(shifted, Direction.Axis.X, SECTION_128, 0));
            assertTrue(DhFold.foldedSpanInsideTheWorld(shifted, Direction.Axis.X, SECTION_128, -6));
        }

        @Test
        void theUnboundedAxisOfACylinderIsAlwaysComplete() {
            assertTrue(DhFold.foldedSpanInsideTheWorld(cylinder(0, 101), Direction.Axis.Z, SECTION_256, 9999));
            assertTrue(DhFold.isCompleteSection(cylinder(0, 101), LEAF, SECTION_256, 1, 9999));
        }
    }

    @Nested
    class ASectionContainsACopyOfAGenerationPosition {
        private static final byte SECTION_128 = 7;

        private final ToroidalShape odd = torus(0, 101);

        @Test
        void theCopyInsideTheSectionIsFoundWhicheverLapTheSectionIsIn() {
            assertTrue(DhFold.containsACopy(odd, Direction.Axis.X, SECTION_128, 114, LEAF, 26));
            assertTrue(DhFold.containsACopy(odd, Direction.Axis.X, SECTION_128, 114, LEAF, 27));
            assertFalse(DhFold.containsACopy(odd, Direction.Axis.X, SECTION_128, 114, LEAF, 28));
            assertFalse(DhFold.containsACopy(odd, Direction.Axis.X, SECTION_128, 114, LEAF, 25));
        }

        @Test
        void aSectionContainsItsOwnCopiesAndNoNeighbour() {
            assertTrue(DhFold.containsACopy(odd, Direction.Axis.X, LEAF, 5, LEAF, 5 + 3 * 101));
            assertFalse(DhFold.containsACopy(odd, Direction.Axis.X, LEAF, 5, LEAF, 6));
        }

        @Test
        void theUnboundedAxisOfACylinderContainsOnlyWhatLiesInside() {
            ToroidalShape cylinder = cylinder(0, 101);
            assertTrue(DhFold.containsACopy(cylinder, Direction.Axis.Z, SECTION_128, 3, LEAF, 7));
            assertFalse(DhFold.containsACopy(cylinder, Direction.Axis.Z, SECTION_128, 3, LEAF, 7 + 101));
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
            assertEquals(-1, DhFold.nearestSection(shape, Direction.Axis.X, SNAP, LEAF, 10, sectionsPerWorld - 1));
            assertEquals(sectionsPerWorld, DhFold.nearestSection(shape, Direction.Axis.X, SNAP, LEAF, WIDTH_BLOCKS - 10, 0));
        }

        @Test
        void aSectionAlreadyNearTheReferenceStays() {
            assertEquals(4, DhFold.nearestSection(shape, Direction.Axis.X, SNAP, LEAF, 300, 4));
        }

        @Test
        void aSectionAboveTheExactLevelIsSeatedByItsOwnPeriod() {
            ToroidalShape tiny = torus(-16, 16);
            assertEquals(0, DhFold.nearestSection(tiny, Direction.Axis.X, SNAP, (byte) 9, 0, -1));
            assertEquals(-1, DhFold.nearestSection(tiny, Direction.Axis.X, SNAP, (byte) 9, -100, -1));
        }

        @Test
        void aKeyOfAWiderPeriodIsSeatedOnTheCopyNearestTheReference() {
            ToroidalShape odd = torus(0, 101);
            int ref = 4 * 1616 + 10;
            assertEquals(106, DhFold.nearestSection(odd, Direction.Axis.X, SNAP, LEAF, ref, 5));
            assertEquals(106, DhFold.nearestSection(odd, Direction.Axis.X, SNAP, LEAF, ref, 106 + 101));
        }
    }

    @Nested
    class OnlyTheNearestCopyIsDrawn {
        private final ToroidalShape shape = torus(0, WIDTH_CHUNKS);

        @Test
        void aSectionJustAcrossTheSeamIsDrawnAndItsFarCopyIsNot() {
            int sectionsPerWorld = WIDTH_BLOCKS / DhFold.sectionWidthBlocks(LEAF);
            assertTrue(DhFold.isNearestSection(shape, Direction.Axis.X, SNAP, LEAF, 60, 4));
            assertFalse(DhFold.isNearestSection(shape, Direction.Axis.X, SNAP, LEAF, 60, 4 - sectionsPerWorld));
        }

        @Test
        void theAntipodeTieDrawsThePositiveSide() {
            assertTrue(DhFold.isNearestSection(shape, Direction.Axis.X, SNAP, LEAF, 32, 8));
            assertFalse(DhFold.isNearestSection(shape, Direction.Axis.X, SNAP, LEAF, 32, -8));
        }

        @Test
        void aSectionAsWideAsTheWorldStraddlesTheWindowAndNeverDrawsWhole() {
            ToroidalShape tiny = torus(-16, 16);
            assertFalse(DhFold.isNearestSection(tiny, Direction.Axis.X, SNAP, (byte) 9, 0, 0));
            assertFalse(DhFold.isNearestSection(tiny, Direction.Axis.X, SNAP, (byte) 9, 0, -1));
            assertTrue(DhFold.overlapsNearestWindow(tiny, Direction.Axis.X, SNAP, (byte) 9, 0, 0));
            assertTrue(DhFold.overlapsNearestWindow(tiny, Direction.Axis.X, SNAP, (byte) 9, 0, -1));
        }

        @Test
        void theUnboundedAxisOfACylinderNeverCulls() {
            ToroidalShape cylinder = cylinder(0, WIDTH_CHUNKS);
            assertTrue(DhFold.isNearestSection(cylinder, Direction.Axis.Z, SNAP, LEAF, 0, 5 * WIDTH_BLOCKS));
            assertFalse(DhFold.isNearestSection(cylinder, Direction.Axis.X, SNAP, LEAF, 0, WIDTH_BLOCKS / 64));
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
            assertFalse(DhFold.isNearestSection(shape, Direction.Axis.X, SNAP, LEAF, ref, section));
            assertTrue(DhFold.isNearestSection(shape, Direction.Axis.X, SNAP, LEAF, ref, section - SECTIONS_PER_WORLD));
            assertEquals(section - SECTIONS_PER_WORLD, DhFold.nearestSection(shape, Direction.Axis.X, SNAP, LEAF, ref, section));
        }

        @Test
        void everyLapCopyOfASectionResolvesToTheOneCopyTheGateKeeps() {
            for (int ref = -600; ref <= 600; ref += 37) {
                for (int section = 0; section < SECTIONS_PER_WORLD; section++) {
                    int kept = DhFold.nearestSection(shape, Direction.Axis.X, SNAP, LEAF, ref, section);
                    int drawn = 0;
                    for (int lap = -2; lap <= 2; lap++) {
                        int copy = section + lap * SECTIONS_PER_WORLD;
                        if (DhFold.isNearestSection(shape, Direction.Axis.X, SNAP, LEAF, ref, copy)) {
                            drawn++;
                            assertEquals(kept, copy, "ref " + ref + " section " + section);
                        }
                        assertEquals(kept, DhFold.nearestSection(shape, Direction.Axis.X, SNAP, LEAF, ref, copy));
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

    @Nested
    class TheLapIsDecidedAtTheSnapLevelAndInherited {
        private static final byte CHUNK_32 = 9;

        private final ToroidalShape shape = torus(-32, 32);

        @Test
        void theSnapCellIsASixteenthOfTheNarrowestLoopAndNeverBelowTheLeaf() {
            assertEquals(6, DhFold.snapDetailLevel(torus(-32, 32), LEAF));
            assertEquals(8, DhFold.snapDetailLevel(torus(-160, 160), LEAF));
            assertEquals(6, DhFold.snapDetailLevel(torus(-16, 16), LEAF));
            assertEquals(6, DhFold.snapDetailLevel(cylinder(0, WIDTH_CHUNKS), LEAF));
            assertEquals(7, DhFold.snapDetailLevel(torus(0, 2 * WIDTH_CHUNKS), LEAF));
        }

        @Test
        void aParentInTheWindowHasEveryChildInIt() {
            for (int ref = -600; ref <= 600; ref += 37) {
                for (byte level = LEAF + 1; level <= WORLD; level++) {
                    for (int section = -6; section <= 6; section++) {
                        if (!DhFold.isNearestSection(shape, Direction.Axis.X, SNAP, level, ref, section)) {
                            continue;
                        }

                        byte child = (byte) (level - 1);
                        String where = "ref " + ref + " level " + level + " section " + section;
                        assertTrue(DhFold.isNearestSection(shape, Direction.Axis.X, SNAP, child, ref, 2 * section),
                                where + " first child");
                        assertTrue(DhFold.isNearestSection(shape, Direction.Axis.X, SNAP, child, ref, 2 * section + 1),
                                where + " last child");
                    }
                }
            }
        }

        @Test
        void aParentOutsideTheWindowHasNoChildInIt() {
            for (int ref = -600; ref <= 600; ref += 37) {
                for (byte level = LEAF + 1; level <= WORLD; level++) {
                    for (int section = -6; section <= 6; section++) {
                        if (DhFold.overlapsNearestWindow(shape, Direction.Axis.X, SNAP, level, ref, section)) {
                            continue;
                        }

                        byte child = (byte) (level - 1);
                        String where = "ref " + ref + " level " + level + " section " + section;
                        assertFalse(DhFold.overlapsNearestWindow(shape, Direction.Axis.X, SNAP, child, ref, 2 * section),
                                where + " first child");
                        assertFalse(DhFold.overlapsNearestWindow(shape, Direction.Axis.X, SNAP, child, ref, 2 * section + 1),
                                where + " last child");
                    }
                }
            }
        }

        @Test
        void aHalfWorldSectionStraddlesTheWindowEdgeOnOneSideAndLiesWholeOnTheOther() {
            assertTrue(DhFold.isNearestSection(shape, Direction.Axis.X, SNAP, CHUNK_32, 200, 0));
            assertFalse(DhFold.isNearestSection(shape, Direction.Axis.X, SNAP, CHUNK_32, 200, -1));
            assertTrue(DhFold.overlapsNearestWindow(shape, Direction.Axis.X, SNAP, CHUNK_32, 200, -1));
            assertTrue(DhFold.overlapsNearestWindow(shape, Direction.Axis.X, SNAP, CHUNK_32, 200, 1));
            assertFalse(DhFold.overlapsNearestWindow(shape, Direction.Axis.X, SNAP, CHUNK_32, 200, 2));
        }

        @Test
        void theWindowAtTheSnapLevelIsOneWorldWide() {
            int cellsPerWorld = WIDTH_BLOCKS / DhFold.sectionWidthBlocks(SNAP);
            for (int ref = -600; ref <= 600; ref += 37) {
                int inside = 0;
                for (int cell = -3 * cellsPerWorld; cell < 3 * cellsPerWorld; cell++) {
                    if (DhFold.isNearestSection(shape, Direction.Axis.X, SNAP, SNAP, ref, cell)) {
                        inside++;
                    }
                }

                assertEquals(cellsPerWorld, inside, "ref " + ref);
            }
        }
    }
}
