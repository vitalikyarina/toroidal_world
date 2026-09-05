package com.toroidalworld.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

class FoldedBoxQueryTest {
    private static final int MIN_CHUNK = -8;
    private static final int MAX_CHUNK = 8;
    private static final int LOWER = MIN_CHUNK * CoordinateConstants.CHUNK_WIDTH;
    private static final int UPPER = MAX_CHUNK * CoordinateConstants.CHUNK_WIDTH;
    private static final int WIDTH = UPPER - LOWER;

    private static final int BEFORE_SEAM = 8;

    private static final double MIN_Y = 60.0;
    private static final double MAX_Y = 70.0;
    private static final double MIN_Z = 4.0;
    private static final double MAX_Z = 20.0;

    private static final WorldLoopBounds LOOPS_ON_X =
            new WorldLoopBounds(new AxisBounds.Looped(MIN_CHUNK, MAX_CHUNK), AxisBounds.Unbounded.INSTANCE);

    private static final WorldFold CYLINDER = WorldFolds.of(FlatShape.cylinder(LOOPS_ON_X));

    private static final AABB NEAR_PIECE = new AABB(UPPER - BEFORE_SEAM, MIN_Y, MIN_Z, UPPER, MAX_Y, MAX_Z);

    private static final Vec3 ANCHOR_BESIDE_THE_NEAR_PIECE = new Vec3(UPPER - BEFORE_SEAM / 2.0, MIN_Y, MIN_Z);
    private static final Vec3 ANCHOR_ACROSS_THE_SEAM = new Vec3(LOWER + BEFORE_SEAM, MIN_Y, MIN_Z);

    @Test
    void towardANullFoldHandsTheArgumentBoxBack() {
        assertSame(NEAR_PIECE, FoldedBoxQuery.toward(null, ANCHOR_ACROSS_THE_SEAM, NEAR_PIECE));
    }

    @Test
    void towardAFoldThatWrapsNothingHandsTheArgumentBoxBack() {
        assertSame(NEAR_PIECE, FoldedBoxQuery.toward(WorldFolds.NOOP, ANCHOR_ACROSS_THE_SEAM, NEAR_PIECE));
    }

    @Test
    void towardAnAnchorBesideTheBoxHandsTheArgumentBoxBack() {
        assertSame(NEAR_PIECE, FoldedBoxQuery.toward(CYLINDER, ANCHOR_BESIDE_THE_NEAR_PIECE, NEAR_PIECE));
    }

    @Test
    void towardAnAnchorAcrossTheSeamSeatsTheBoxInTheLappedCopy() {
        assertEquals(NEAR_PIECE.move(-WIDTH, 0.0, 0.0),
                FoldedBoxQuery.toward(CYLINDER, ANCHOR_ACROSS_THE_SEAM, NEAR_PIECE));
    }
}
