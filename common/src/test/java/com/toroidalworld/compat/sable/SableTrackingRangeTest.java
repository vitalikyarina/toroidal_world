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

class SableTrackingRangeTest {
    private static final int HALF_WIDTH_CHUNKS_X = 16;
    private static final int HALF_WIDTH_CHUNKS_Z = 8;
    private static final WorldFold FOLD = WorldFolds.of(FlatShape.torus(
            new WorldLoopBounds(-HALF_WIDTH_CHUNKS_X, HALF_WIDTH_CHUNKS_X, -HALF_WIDTH_CHUNKS_Z, HALF_WIDTH_CHUNKS_Z)));

    private static final Vector3dc POSE = new Vector3d(250.0, 70.0, 100.0);
    private static final double TRACKED_X = -250.0;
    private static final double TRACKED_Y = 100.0;
    private static final double TRACKED_Z = -100.0;

    private static final double SENTINEL = 1234.5;

    @Test
    void aSubLevelPastTheSeamIsTrackedAtItsFoldedDistance() {
        Operation<Double> refuses = args -> {
            throw new AssertionError("the original was called with a fold in hand");
        };

        assertEquals(12.0 * 12.0 + 30.0 * 30.0 + 56.0 * 56.0,
                SableTrackingRange.sqrDistance(FOLD, POSE, TRACKED_X, TRACKED_Y, TRACKED_Z, refuses), 0.0,
                "x folds 500 to 12 over 512, z folds 200 to 56 over 256, y is never folded");
    }

    @Test
    void anUnwrappedLevelLeavesTheTrackingRangeToSable() {
        Operation<Double> original = args -> {
            assertArrayEquals(new Object[] {POSE, TRACKED_X, TRACKED_Y, TRACKED_Z}, args);
            return SENTINEL;
        };

        assertEquals(SENTINEL,
                SableTrackingRange.sqrDistance((WorldFold) null, POSE, TRACKED_X, TRACKED_Y, TRACKED_Z, original), 0.0);
    }
}
