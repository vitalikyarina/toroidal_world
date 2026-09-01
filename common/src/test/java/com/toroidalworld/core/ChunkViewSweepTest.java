package com.toroidalworld.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.world.level.ChunkPos;

class ChunkViewSweepTest {
    private static final int MIN_CHUNK = -16;
    private static final int MAX_CHUNK = 15;

    private static final WorldLoopBounds LOOPS_ON_X =
            new WorldLoopBounds(new AxisBounds.Looped(MIN_CHUNK, MAX_CHUNK + 1), AxisBounds.Unbounded.INSTANCE);
    private static final WorldFold CYLINDER = WorldFolds.of(FlatShape.cylinder(LOOPS_ON_X));
    private static final WorldFold TORUS =
            WorldFolds.of(FlatShape.latticeTorus(WorldLoopBounds.ofWidth(MAX_CHUNK - MIN_CHUNK + 1), 0));

    private static final int VIEW_DISTANCE = 8;
    private static final int VIEW_SPAN = 2 * (VIEW_DISTANCE + 1) + 1;

    @Test
    void aPairTheFoldCannotRelateSweepsTheSameAtAnyDistance() {
        long near = sweepAlongTheUnboundedAxis(1_250_000).positions();
        long far = sweepAlongTheUnboundedAxis(2_500_000).positions();

        assertEquals(near, far, () -> "swept " + near + " positions at one distance and " + far + " at twice it");
    }

    @Test
    void aPairTheFoldCannotRelateStaysInsideViewDistance() {
        long swept = sweepAlongTheUnboundedAxis(1_250_000).positions();
        long bound = 2L * VIEW_SPAN * VIEW_SPAN;

        assertTrue(swept <= bound, () -> "swept " + swept + " positions where " + bound + " covers both views");
    }

    @Test
    void aMoveAcrossTheSeamPlansTheBoxItAlwaysDid() {
        ChunkViewSweep sweep = ChunkViewSweep.between(
                TORUS, new ChunkPos(15, 0), VIEW_DISTANCE, new ChunkPos(-15, 0), VIEW_DISTANCE);

        assertEquals(new ChunkViewSweep.OverBoth(6, -9, 26, 9), sweep);
    }

    @Test
    void noPairOfCentresInsideTheWorldEverSplitsTheSweep() {
        ChunkPos seamCorner = new ChunkPos(MAX_CHUNK, MAX_CHUNK);

        for (int x = MIN_CHUNK; x <= MAX_CHUNK; x++) {
            for (int z = MIN_CHUNK; z <= MAX_CHUNK; z++) {
                ChunkPos next = new ChunkPos(x, z);
                ChunkViewSweep sweep = ChunkViewSweep.between(TORUS, seamCorner, VIEW_DISTANCE, next, VIEW_DISTANCE);

                assertTrue(sweep instanceof ChunkViewSweep.OverBoth, () -> "split the sweep at " + next);
            }
        }
    }

    private static ChunkViewSweep sweepAlongTheUnboundedAxis(int chunksAway) {
        return ChunkViewSweep.between(
                CYLINDER, new ChunkPos(0, 0), VIEW_DISTANCE, new ChunkPos(0, chunksAway), VIEW_DISTANCE);
    }
}
