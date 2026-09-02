package com.toroidalworld.compat.create.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.DeckGroupFold;
import com.toroidalworld.core.DeckTransformation;
import com.toroidalworld.core.SeamTransform;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.map.MapSurfaceCopies.Copies;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

class TrainMapViewFoldTest {
    private static final int WORLD_CHUNKS = 16;
    private static final int WORLD_BLOCKS = WORLD_CHUNKS * 2 * 16;
    private static final int WORLD_MIN = -WORLD_CHUNKS * 16;
    private static final int WORLD_MAX = WORLD_CHUNKS * 16 - 1;
    private static final int SKEW_CHUNKS = 4;
    private static final int MIRROR_LINE_CHUNK = 3;
    private static final int MIRROR_LINE_BLOCKS = MIRROR_LINE_CHUNK * 16;
    private static final int VIEW_BLOCKS = 20;
    private static final int SURFACE_REACH = 5;
    private static final int MANY_WORLDS = 40;
    private static final int CLEAR_OF_THE_BOUND_BLOCKS = 100;

    private static final WorldLoopBounds BOUNDS =
            new WorldLoopBounds(-WORLD_CHUNKS, WORLD_CHUNKS, -WORLD_CHUNKS, WORLD_CHUNKS);
    private static final WorldLoopBounds X_ONLY =
            new WorldLoopBounds(new AxisBounds.Looped(-WORLD_CHUNKS, WORLD_CHUNKS), AxisBounds.Unbounded.INSTANCE);

    private static final WorldFold PER_AXIS = WorldFolds.of(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));
    private static final WorldFold DECK_TORUS = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));
    private static final WorldFold SKEWED = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, SKEW_CHUNKS));
    private static final WorldFold MIRRORED =
            new DeckGroupFold(FlatShape.mirrored(BOUNDS, Direction.Axis.Z, MIRROR_LINE_CHUNK));
    private static final WorldFold CYLINDER = new DeckGroupFold(FlatShape.cylinder(X_ONLY));

    private static final List<WorldFold> TORI = List.of(PER_AXIS, DECK_TORUS, SKEWED, MIRRORED);
    private static final List<WorldFold> PLAIN_TORI = List.of(PER_AXIS, DECK_TORUS);

    private static final Copies EVERYWHERE = new Copies(SURFACE_REACH, BoundingBox.infinite());
    private static final Rect2i INSIDE = new Rect2i(-100, -100, 200, 200);
    private static final Rect2i ACROSS_THE_X_SEAM =
            new Rect2i(WORLD_MAX - VIEW_BLOCKS / 2 + 1, 0, VIEW_BLOCKS, VIEW_BLOCKS);
    private static final BlockPos PIXEL_BEYOND_THE_X_SEAM = new BlockPos(300, 0, 10);
    private static final DeckTransformation ONE_LAP_ALONG_X =
            new DeckTransformation(SeamTransform.translation(WORLD_BLOCKS, 0));
    private static final DeckTransformation ONE_LAP_BACK_ALONG_X =
            new DeckTransformation(SeamTransform.translation(-WORLD_BLOCKS, 0));
    private static final Rect2i ENDING_ON_THE_X_BOUND = new Rect2i(0, 0, WORLD_BLOCKS / 2, VIEW_BLOCKS);
    private static final Rect2i STARTING_ON_THE_X_BOUND = new Rect2i(WORLD_MIN, 0, WORLD_BLOCKS / 2, VIEW_BLOCKS);

    @Test
    void aPixelPastTheXBoundLandsOneWorldWidthBack() {
        for (WorldFold fold : List.of(PER_AXIS, DECK_TORUS, SKEWED)) {
            long pixel = TrainMapViewFold.foldPixelNode(fold, 300, 10);

            assertEquals(BlockPos.asLong(300 - WORLD_BLOCKS, 0, 10), pixel, "in " + fold);
        }
    }

    @Test
    void aPixelPastTheGlideSeamOfAMirroredWorldLandsOnTheMirroredColumn() {
        long pixel = TrainMapViewFold.foldPixelNode(MIRRORED, 300, 10);

        assertEquals(BlockPos.asLong(300 - WORLD_BLOCKS, 0, 2 * MIRROR_LINE_BLOCKS - 10 - 1), pixel);
    }

    @Test
    void aPixelInsideTheBoundsKeepsItsCoordinates() {
        for (WorldFold fold : TORI) {
            long pixel = TrainMapViewFold.foldPixelNode(fold, 30, 10);

            assertEquals(BlockPos.asLong(30, 0, 10), pixel, "in " + fold);
        }
    }

    @Test
    void aViewInsideTheBoundsIsTheIdentityAlone() {
        for (WorldFold fold : TORI) {
            List<DeckTransformation> copies = TrainMapViewFold.copies(fold, EVERYWHERE, INSIDE);

            assertEquals(1, copies.size(), "in " + fold);
            assertSame(DeckTransformation.IDENTITY, copies.getFirst(), "in " + fold);
        }
    }

    @Test
    void aViewAcrossTheSeamAddsTheCopyBeyondIt() {
        for (WorldFold fold : PLAIN_TORI) {
            List<DeckTransformation> copies = TrainMapViewFold.copies(fold, EVERYWHERE, ACROSS_THE_X_SEAM);

            assertEquals(List.of(DeckTransformation.IDENTITY, ONE_LAP_ALONG_X), copies, "in " + fold);
        }
    }

    @Test
    void theCopyBeyondTheSeamShowsThePixelTheFoldSaysItShows() {
        for (WorldFold fold : TORI) {
            List<DeckTransformation> copies = TrainMapViewFold.copies(fold, EVERYWHERE, ACROSS_THE_X_SEAM);
            assertEquals(2, copies.size(), "in " + fold);

            DeckTransformation beyond = copies.get(1);
            BlockPos canonical = TrainMapViewFold.canonicalPixel(beyond,
                    PIXEL_BEYOND_THE_X_SEAM.getX(), PIXEL_BEYOND_THE_X_SEAM.getZ());

            assertEquals(fold.fold(PIXEL_BEYOND_THE_X_SEAM), canonical, "in " + fold);
            assertEquals(PIXEL_BEYOND_THE_X_SEAM, beyond.apply(canonical), "in " + fold);
        }
    }

    @Test
    void theCopyBeyondAMirroredSeamFlipsHandedness() {
        DeckTransformation beyond = TrainMapViewFold.copies(MIRRORED, EVERYWHERE, ACROSS_THE_X_SEAM).get(1);

        assertFalse(beyond.orientation().preservesHandedness());
        assertTrue(beyond.orientation().flipsZ());
        for (WorldFold fold : List.of(PER_AXIS, DECK_TORUS, SKEWED)) {
            assertTrue(TrainMapViewFold.copies(fold, EVERYWHERE, ACROSS_THE_X_SEAM).get(1).orientation().isIdentity(),
                    "in " + fold);
        }
    }

    @Test
    void theCanonicalViewIsTheViewAsTheCopyDrawsIt() {
        for (WorldFold fold : TORI) {
            for (DeckTransformation copy : TrainMapViewFold.copies(fold, EVERYWHERE, ACROSS_THE_X_SEAM)) {
                Rect2i canonical = TrainMapViewFold.canonicalView(copy, ACROSS_THE_X_SEAM);

                assertEquals(box(ACROSS_THE_X_SEAM), copy.apply(box(canonical)), copy + " in " + fold);
            }
        }
    }

    @Test
    void aViewEndingOnTheBoundDoesNotReachPastIt() {
        Rect2i wholeWorld = new Rect2i(WORLD_MIN, 0, WORLD_BLOCKS, VIEW_BLOCKS);
        for (WorldFold fold : TORI) {
            List<DeckTransformation> copies = TrainMapViewFold.copies(fold, EVERYWHERE, wholeWorld);

            assertEquals(List.of(DeckTransformation.IDENTITY), copies, "in " + fold);
        }
    }

    @Test
    void aViewEndingOnTheBoundStillDrawsTheCopyPastIt() {
        for (WorldFold fold : TORI) {
            assertEquals(List.of(DeckTransformation.IDENTITY),
                    TrainMapViewFold.copies(fold, EVERYWHERE, ENDING_ON_THE_X_BOUND), "in " + fold);
            assertEquals(2, TrainMapViewFold.copiesDrawnFor(fold, EVERYWHERE, ENDING_ON_THE_X_BOUND).size(),
                    "in " + fold);
        }

        for (WorldFold fold : PLAIN_TORI) {
            assertEquals(List.of(DeckTransformation.IDENTITY, ONE_LAP_ALONG_X),
                    TrainMapViewFold.copiesDrawnFor(fold, EVERYWHERE, ENDING_ON_THE_X_BOUND), "in " + fold);
        }
    }

    @Test
    void aViewStartingOnTheBoundStillDrawsTheCopyBeforeIt() {
        for (WorldFold fold : TORI) {
            assertEquals(List.of(DeckTransformation.IDENTITY),
                    TrainMapViewFold.copies(fold, EVERYWHERE, STARTING_ON_THE_X_BOUND), "in " + fold);
            assertEquals(2, TrainMapViewFold.copiesDrawnFor(fold, EVERYWHERE, STARTING_ON_THE_X_BOUND).size(),
                    "in " + fold);
        }

        for (WorldFold fold : PLAIN_TORI) {
            assertEquals(List.of(DeckTransformation.IDENTITY, ONE_LAP_BACK_ALONG_X),
                    TrainMapViewFold.copiesDrawnFor(fold, EVERYWHERE, STARTING_ON_THE_X_BOUND), "in " + fold);
        }
    }

    @Test
    void aViewClearOfTheBoundDrawsTheIdentityAlone() {
        Rect2i clearOfTheBound = new Rect2i(0, 0, WORLD_BLOCKS / 2 - CLEAR_OF_THE_BOUND_BLOCKS, VIEW_BLOCKS);
        for (WorldFold fold : TORI) {
            assertEquals(List.of(DeckTransformation.IDENTITY),
                    TrainMapViewFold.copiesDrawnFor(fold, EVERYWHERE, clearOfTheBound), "in " + fold);
        }
    }

    @Test
    void aViewOfManyWorldsIsCutAtTheSurfaceReach() {
        Rect2i manyWorlds = new Rect2i(WORLD_MIN - MANY_WORLDS * WORLD_BLOCKS, 0, 2 * MANY_WORLDS * WORLD_BLOCKS,
                VIEW_BLOCKS);
        for (WorldFold fold : PLAIN_TORI) {
            List<DeckTransformation> copies = TrainMapViewFold.copies(fold, EVERYWHERE, manyWorlds);

            assertEquals(2 * SURFACE_REACH + 1, copies.size(), "in " + fold);
            assertSame(DeckTransformation.IDENTITY, copies.getFirst(), "in " + fold);
        }
    }

    @Test
    void aSurfaceThatDoesNotRepeatGetsTheIdentityAlone() {
        for (WorldFold fold : TORI) {
            List<DeckTransformation> copies = TrainMapViewFold.copies(fold, Copies.NONE, ACROSS_THE_X_SEAM);

            assertEquals(List.of(DeckTransformation.IDENTITY), copies, "in " + fold);
        }
    }

    @Test
    void theViewIsCutToWhatTheSurfacePainted() {
        Rect2i threeWorlds = new Rect2i(WORLD_MIN - WORLD_BLOCKS, 0, 3 * WORLD_BLOCKS, VIEW_BLOCKS);
        Copies oneLapEachSide = new Copies(SURFACE_REACH,
                new BoundingBox(WORLD_MIN - WORLD_BLOCKS, 0, WORLD_MIN, WORLD_MAX + WORLD_BLOCKS, 0, WORLD_MAX));
        Copies oneLapBeyondAlone = new Copies(SURFACE_REACH,
                new BoundingBox(WORLD_MIN, 0, WORLD_MIN, WORLD_MAX + WORLD_BLOCKS, 0, WORLD_MAX));
        for (WorldFold fold : PLAIN_TORI) {
            assertEquals(3, TrainMapViewFold.copies(fold, oneLapEachSide, threeWorlds).size(), "in " + fold);
            assertEquals(List.of(DeckTransformation.IDENTITY, ONE_LAP_ALONG_X),
                    TrainMapViewFold.copies(fold, oneLapBeyondAlone, threeWorlds), "in " + fold);
        }
    }

    @Test
    void aViewBeyondEverythingPaintedGetsNothing() {
        Rect2i farAway = new Rect2i(10 * WORLD_BLOCKS, 0, 100, VIEW_BLOCKS);
        Copies oneLapAround = new Copies(1, BoundingBox.infinite());
        Copies insidePainted = new Copies(SURFACE_REACH, box(INSIDE));
        for (WorldFold fold : TORI) {
            assertEquals(List.of(), TrainMapViewFold.copies(fold, oneLapAround, farAway), "in " + fold);
            assertEquals(List.of(), TrainMapViewFold.copies(fold, insidePainted, farAway), "in " + fold);
        }
    }

    @Test
    void anEmptyViewGetsNothing() {
        Rect2i empty = new Rect2i(WORLD_MAX, 0, 0, VIEW_BLOCKS);
        for (WorldFold fold : TORI) {
            assertEquals(List.of(), TrainMapViewFold.copies(fold, EVERYWHERE, empty), "in " + fold);
        }
    }

    @Test
    void anUnboundedAxisIsOneCopy() {
        Rect2i alongTheUnboundedAxis = new Rect2i(-100, -1000, 200, 5000);

        List<DeckTransformation> copies = TrainMapViewFold.copies(CYLINDER, EVERYWHERE, alongTheUnboundedAxis);

        assertEquals(List.of(DeckTransformation.IDENTITY), copies);
    }

    @Test
    void theDrawnMarginHoldsPerAxis() {
        Rect2i alongTheUnboundedAxis = new Rect2i(-100, -1000, 200, 5000);

        assertEquals(List.of(DeckTransformation.IDENTITY, ONE_LAP_ALONG_X),
                TrainMapViewFold.copiesDrawnFor(CYLINDER, EVERYWHERE, ENDING_ON_THE_X_BOUND));
        assertEquals(List.of(DeckTransformation.IDENTITY),
                TrainMapViewFold.copiesDrawnFor(CYLINDER, EVERYWHERE, alongTheUnboundedAxis));
    }

    private static BoundingBox box(Rect2i view) {
        return new BoundingBox(view.getX(), 0, view.getY(),
                view.getX() + view.getWidth() - 1, 0, view.getY() + view.getHeight() - 1);
    }
}
