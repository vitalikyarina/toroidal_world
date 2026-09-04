package com.toroidalworld.compat.sable;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.junit.jupiter.api.Test;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.shape.FlatShape;

class SableSeamDistanceTest {
    private static final int HALF_WIDTH_CHUNKS_X = 16;
    private static final int HALF_WIDTH_CHUNKS_Z = 8;
    private static final WorldFold FOLD = WorldFolds.of(FlatShape.latticeTorus(
            new WorldLoopBounds(-HALF_WIDTH_CHUNKS_X, HALF_WIDTH_CHUNKS_X, -HALF_WIDTH_CHUNKS_Z, HALF_WIDTH_CHUNKS_Z),
            FlatShape.NO_SKEW));

    private static final Vector3dc FROM = new Vector3d(250.0, 70.0, 100.0);
    private static final Vector3dc TO = new Vector3d(-250.0, 100.0, -100.0);
    private static final Vector3dc TO_HIGH_ABOVE = new Vector3d(-250.0, 200.0, -100.0);

    private static final double SENTINEL = 1234.5;
    private static final Operation<Double> REFUSES = args -> {
        throw new AssertionError("the original was called with a fold in hand");
    };

    @Test
    void theSquaredDistanceIsMeasuredTheShortWayRoundOnBothLoopedAxes() {
        assertEquals(12.0 * 12.0 + 30.0 * 30.0 + 56.0 * 56.0, SableSeamDistance.sqr(FOLD, FROM, TO, REFUSES), 0.0,
                "x folds 500 to 12 over 512, z folds 200 to 56 over 256, y is never folded");
    }

    @Test
    void anUnwrappedLevelLeavesTheSquaredDistanceToSable() {
        Operation<Double> original = args -> {
            assertArrayEquals(new Object[] {FROM, TO}, args);
            return SENTINEL;
        };

        assertEquals(SENTINEL, SableSeamDistance.sqr((WorldFold) null, FROM, TO, original), 0.0);
    }

    @Test
    void theRectilinearDistanceIsTheWidestFoldedAxis() {
        assertEquals(56.0, SableSeamDistance.rectilinear(FOLD, FROM, TO, REFUSES), 0.0,
                "z folds to 56, wider than the folded x of 12 and the y of 30");
    }

    @Test
    void theRectilinearDistanceCountsHeightUnfolded() {
        assertEquals(130.0, SableSeamDistance.rectilinear(FOLD, FROM, TO_HIGH_ABOVE, REFUSES), 0.0,
                "y is 130 and no axis folds past it");
    }

    @Test
    void anUnwrappedLevelLeavesTheRectilinearDistanceToSable() {
        Operation<Double> original = args -> {
            assertArrayEquals(new Object[] {FROM, TO}, args);
            return SENTINEL;
        };

        assertEquals(SENTINEL, SableSeamDistance.rectilinear((WorldFold) null, FROM, TO, original), 0.0);
    }
}
