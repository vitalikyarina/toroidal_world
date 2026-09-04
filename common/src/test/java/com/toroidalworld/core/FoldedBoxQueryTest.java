package com.toroidalworld.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.Direction;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.phys.AABB;

class FoldedBoxQueryTest {
    private static final int MIN_CHUNK = -8;
    private static final int MAX_CHUNK = 8;
    private static final int LOWER = MIN_CHUNK * CoordinateConstants.CHUNK_WIDTH;
    private static final int UPPER = MAX_CHUNK * CoordinateConstants.CHUNK_WIDTH;

    private static final int BEFORE_SEAM = 8;
    private static final int PAST_SEAM = 12;

    private static final double MIN_Y = 60.0;
    private static final double MAX_Y = 70.0;
    private static final double MIN_Z = 4.0;
    private static final double MAX_Z = 20.0;

    private static final WorldLoopBounds LOOPS_ON_X =
            new WorldLoopBounds(new AxisBounds.Looped(MIN_CHUNK, MAX_CHUNK), AxisBounds.Unbounded.INSTANCE);

    private static final WorldFold CYLINDER = WorldFolds.of(FlatShape.cylinder(LOOPS_ON_X));
    private static final WorldFold DECK_GROUP_CYLINDER = new DeckGroupFold(FlatShape.cylinder(LOOPS_ON_X));
    private static final WorldFold MOBIUS = new DeckGroupFold(FlatShape.mirrored(LOOPS_ON_X, Direction.Axis.Z, 0));

    private static final AABB INSIDE = new AABB(-10.0, MIN_Y, MIN_Z, 10.0, MAX_Y, MAX_Z);
    private static final AABB ACROSS_THE_SEAM =
            new AABB(UPPER - BEFORE_SEAM, MIN_Y, MIN_Z, UPPER + PAST_SEAM, MAX_Y, MAX_Z);

    private static final AABB NEAR_PIECE = new AABB(UPPER - BEFORE_SEAM, MIN_Y, MIN_Z, UPPER, MAX_Y, MAX_Z);
    private static final AABB LAPPED_PIECE = new AABB(LOWER, MIN_Y, MIN_Z, LOWER + PAST_SEAM, MAX_Y, MAX_Z);
    private static final AABB MIRRORED_LAPPED_PIECE =
            new AABB(LOWER, MIN_Y, -MAX_Z, LOWER + PAST_SEAM, MAX_Y, -MIN_Z);

    private static final AABB BOX = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
    private static final AABB EQUAL_BOX = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

    @Test
    void aNullFoldHandsTheArgumentBoxBack() {
        List<AABB> pieces = FoldedBoxQuery.pieces(null, ACROSS_THE_SEAM);

        assertEquals(1, pieces.size());
        assertSame(ACROSS_THE_SEAM, pieces.get(0));
    }

    @Test
    void aFoldThatWrapsNothingHandsTheArgumentBoxBack() {
        List<AABB> pieces = FoldedBoxQuery.pieces(WorldFolds.NOOP, ACROSS_THE_SEAM);

        assertEquals(1, pieces.size());
        assertSame(ACROSS_THE_SEAM, pieces.get(0));
    }

    @Test
    void aBoxInsideTheBoundsHandsTheArgumentBoxBack() {
        List<AABB> pieces = FoldedBoxQuery.pieces(CYLINDER, INSIDE);

        assertEquals(1, pieces.size());
        assertSame(INSIDE, pieces.get(0));
    }

    @Test
    void aDecomposableFoldSplitsTheBoxAtTheSeam() {
        assertEquals(List.of(NEAR_PIECE, LAPPED_PIECE), FoldedBoxQuery.pieces(CYLINDER, ACROSS_THE_SEAM));
    }

    @Test
    void aDeckGroupFoldSplitsTheBoxAtTheSameSeam() {
        assertEquals(List.of(NEAR_PIECE, LAPPED_PIECE), FoldedBoxQuery.pieces(DECK_GROUP_CYLINDER, ACROSS_THE_SEAM));
    }

    @Test
    void aMirroredLapKeepsItsOrientationInSplitAndLosesItInPieces() {
        List<WorldFold.Folded<AABB>> folded = MOBIUS.split(ACROSS_THE_SEAM);

        assertTrue(folded.stream().anyMatch(piece -> !piece.isIdentity()),
                () -> "the mobius split carried no orientation: " + folded);
        assertEquals(List.of(NEAR_PIECE, MIRRORED_LAPPED_PIECE), FoldedBoxQuery.pieces(MOBIUS, ACROSS_THE_SEAM));
    }

    @Test
    void theConsumerPassesAnInstanceOnceAndAnEqualOneAgain() {
        List<AABB> seen = new ArrayList<>();
        Consumer<AABB> collect = seen::add;
        Consumer<AABB> once = FoldedBoxQuery.deduplicating(collect);

        once.accept(BOX);
        once.accept(BOX);
        once.accept(EQUAL_BOX);

        assertEquals(2, seen.size());
        assertSame(BOX, seen.get(0));
        assertSame(EQUAL_BOX, seen.get(1));
    }

    @Test
    void theAbortableConsumerPassesAnInstanceOnceAndAnEqualOneAgain() {
        List<AABB> seen = new ArrayList<>();
        AbortableIterationConsumer<AABB> once = FoldedBoxQuery.deduplicating(box -> {
            seen.add(box);
            return AbortableIterationConsumer.Continuation.CONTINUE;
        });

        once.accept(BOX);
        once.accept(BOX);
        once.accept(EQUAL_BOX);

        assertEquals(2, seen.size());
        assertSame(BOX, seen.get(0));
        assertSame(EQUAL_BOX, seen.get(1));
    }

    @Test
    void theAbortableConsumerHandsTheAbortBackToTheCaller() {
        AbortableIterationConsumer<AABB> once =
                FoldedBoxQuery.deduplicating(box -> AbortableIterationConsumer.Continuation.ABORT);

        assertEquals(AbortableIterationConsumer.Continuation.ABORT, once.accept(BOX));
    }

    @Test
    void aRepeatedInstanceContinuesWithoutReachingTheCallee() {
        List<AABB> seen = new ArrayList<>();
        AbortableIterationConsumer<AABB> once = FoldedBoxQuery.deduplicating(box -> {
            seen.add(box);
            return AbortableIterationConsumer.Continuation.ABORT;
        });

        once.accept(BOX);

        assertEquals(AbortableIterationConsumer.Continuation.CONTINUE, once.accept(BOX));
        assertEquals(1, seen.size());
    }
}
