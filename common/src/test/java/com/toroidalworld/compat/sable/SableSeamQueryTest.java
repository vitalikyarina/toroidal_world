package com.toroidalworld.compat.sable;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.shape.FlatShape;

import dev.ryanhcode.sable.companion.math.BoundingBox3d;

class SableSeamQueryTest {
    private static final int HALF_WIDTH_CHUNKS = 16;
    private static final WorldFold FOLD = WorldFolds.of(FlatShape.latticeTorus(
            new WorldLoopBounds(-HALF_WIDTH_CHUNKS, HALF_WIDTH_CHUNKS, -HALF_WIDTH_CHUNKS, HALF_WIDTH_CHUNKS),
            FlatShape.NO_SKEW));

    private static final BoundingBox3d QUERY_ACROSS_THE_X_SEAM = new BoundingBox3d(250.0, 64.0, 0.0, 262.0, 70.0, 8.0);
    private static final BoundingBox3d SUB_LEVEL_PAST_THE_X_SEAM = new BoundingBox3d(-255.0, 64.0, 0.0, -250.0, 70.0, 8.0);

    private static final BoundingBox3d QUERY_ACROSS_THE_Z_SEAM = new BoundingBox3d(0.0, 64.0, 250.0, 8.0, 70.0, 262.0);
    private static final BoundingBox3d SUB_LEVEL_PAST_THE_Z_SEAM = new BoundingBox3d(0.0, 64.0, -255.0, 8.0, 70.0, -250.0);

    private static final BoundingBox3d QUERY_INSIDE_THE_NEAR_HALF = new BoundingBox3d(245.0, 64.0, 0.0, 255.0, 70.0, 8.0);
    private static final BoundingBox3d SUB_LEVEL_TOUCHING_IT = new BoundingBox3d(240.0, 64.0, 0.0, 250.0, 70.0, 8.0);
    private static final BoundingBox3d SUB_LEVEL_ELSEWHERE = new BoundingBox3d(0.0, 64.0, 0.0, 8.0, 70.0, 8.0);

    @Test
    void aSubLevelPastTheXSeamIsReadFromTheOtherSide() {
        assertFalse(SUB_LEVEL_PAST_THE_X_SEAM.intersects(QUERY_ACROSS_THE_X_SEAM),
                "the fixture already intersects without the fold, so it proves nothing");
        assertTrue(SableSeamQuery.intersects(FOLD, SUB_LEVEL_PAST_THE_X_SEAM, QUERY_ACROSS_THE_X_SEAM));
    }

    @Test
    void aSubLevelPastTheZSeamIsReadFromTheOtherSide() {
        assertFalse(SUB_LEVEL_PAST_THE_Z_SEAM.intersects(QUERY_ACROSS_THE_Z_SEAM),
                "the fixture already intersects without the fold, so it proves nothing");
        assertTrue(SableSeamQuery.intersects(FOLD, SUB_LEVEL_PAST_THE_Z_SEAM, QUERY_ACROSS_THE_Z_SEAM));
    }

    @Test
    void aSubLevelTheQueryAlreadyReachesNeedsNoShift() {
        assertTrue(SableSeamQuery.intersects(FOLD, SUB_LEVEL_TOUCHING_IT, QUERY_INSIDE_THE_NEAR_HALF));
    }

    @Test
    void aSubLevelNeitherSideReachesIsStillAMiss() {
        assertFalse(SableSeamQuery.intersects(FOLD, SUB_LEVEL_ELSEWHERE, QUERY_INSIDE_THE_NEAR_HALF));
    }

    @Test
    void aFoldThatWrapsNothingLeavesSableItsOwnAnswer() {
        assertFalse(SableSeamQuery.intersects(WorldFolds.NOOP, SUB_LEVEL_PAST_THE_X_SEAM, QUERY_ACROSS_THE_X_SEAM));
        assertTrue(SableSeamQuery.intersects(WorldFolds.NOOP, SUB_LEVEL_TOUCHING_IT, QUERY_INSIDE_THE_NEAR_HALF));
    }
}
