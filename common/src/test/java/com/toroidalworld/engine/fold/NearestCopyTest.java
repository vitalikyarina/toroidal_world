package com.toroidalworld.engine.fold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.CoordinateConstants;
import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldLoopBounds;
import com.toroidalworld.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

class NearestCopyTest {
    private static final int MIN_CHUNK = -8;
    private static final int MAX_CHUNK = 8;
    private static final int WIDTH_IN_CHUNKS = MAX_CHUNK - MIN_CHUNK;
    private static final int UPPER = MAX_CHUNK * CoordinateConstants.CHUNK_WIDTH;
    private static final int WIDTH = WIDTH_IN_CHUNKS * CoordinateConstants.CHUNK_WIDTH;

    private static final int BEFORE_SEAM = 8;
    private static final int HEIGHT = 64;
    private static final int DEPTH = 4;

    private static final WorldLoopBounds LOOPS_ON_X =
            new WorldLoopBounds(new AxisBounds.Looped(MIN_CHUNK, MAX_CHUNK), AxisBounds.Unbounded.INSTANCE);

    private static final WorldFold CYLINDER = WorldFolds.of(FlatShape.cylinder(LOOPS_ON_X));

    private static final Vec3 TARGET = new Vec3(UPPER - BEFORE_SEAM, HEIGHT, DEPTH);
    private static final Vec3 ANCHOR_BESIDE_THE_TARGET = new Vec3(UPPER - BEFORE_SEAM / 2.0, HEIGHT, DEPTH);
    private static final Vec3 ANCHOR_ACROSS_THE_SEAM = new Vec3(BEFORE_SEAM - UPPER, HEIGHT, DEPTH);

    private static final BlockPos TARGET_BLOCK = BlockPos.containing(TARGET);
    private static final BlockPos ANCHOR_BLOCK_BESIDE_THE_TARGET = BlockPos.containing(ANCHOR_BESIDE_THE_TARGET);
    private static final BlockPos ANCHOR_BLOCK_ACROSS_THE_SEAM = BlockPos.containing(ANCHOR_ACROSS_THE_SEAM);

    private static final ChunkPos TARGET_CHUNK = new ChunkPos(MAX_CHUNK - 1, 0);
    private static final ChunkPos ANCHOR_CHUNK_BESIDE_THE_TARGET = new ChunkPos(MAX_CHUNK - 2, 0);
    private static final ChunkPos ANCHOR_CHUNK_ACROSS_THE_SEAM = new ChunkPos(MIN_CHUNK + 1, 0);

    @Test
    void towardANullFoldHandsTheArgumentPointBack() {
        assertSame(TARGET, NearestCopy.toward(null, ANCHOR_ACROSS_THE_SEAM, TARGET));
        assertSame(TARGET_BLOCK, NearestCopy.toward(null, ANCHOR_BLOCK_ACROSS_THE_SEAM, TARGET_BLOCK));
        assertSame(TARGET_CHUNK, NearestCopy.toward(null, ANCHOR_CHUNK_ACROSS_THE_SEAM, TARGET_CHUNK));
    }

    @Test
    void towardAFoldThatWrapsNothingHandsTheArgumentPointBack() {
        assertSame(TARGET, NearestCopy.toward(WorldFolds.NOOP, ANCHOR_ACROSS_THE_SEAM, TARGET));
        assertSame(TARGET_BLOCK, NearestCopy.toward(WorldFolds.NOOP, ANCHOR_BLOCK_ACROSS_THE_SEAM, TARGET_BLOCK));
        assertSame(TARGET_CHUNK, NearestCopy.toward(WorldFolds.NOOP, ANCHOR_CHUNK_ACROSS_THE_SEAM, TARGET_CHUNK));
    }

    @Test
    void towardAnAnchorBesideTheTargetHandsTheArgumentPointBack() {
        assertSame(TARGET, NearestCopy.toward(CYLINDER, ANCHOR_BESIDE_THE_TARGET, TARGET));
        assertSame(TARGET_BLOCK, NearestCopy.toward(CYLINDER, ANCHOR_BLOCK_BESIDE_THE_TARGET, TARGET_BLOCK));
        assertSame(TARGET_CHUNK, NearestCopy.toward(CYLINDER, ANCHOR_CHUNK_BESIDE_THE_TARGET, TARGET_CHUNK));
    }

    @Test
    void towardAnAnchorAcrossTheSeamSeatsThePointInTheLappedCopy() {
        assertEquals(TARGET.subtract(WIDTH, 0.0, 0.0),
                NearestCopy.toward(CYLINDER, ANCHOR_ACROSS_THE_SEAM, TARGET));
        assertEquals(TARGET_BLOCK.offset(-WIDTH, 0, 0),
                NearestCopy.toward(CYLINDER, ANCHOR_BLOCK_ACROSS_THE_SEAM, TARGET_BLOCK));
        assertEquals(new ChunkPos(TARGET_CHUNK.x - WIDTH_IN_CHUNKS, TARGET_CHUNK.z),
                NearestCopy.toward(CYLINDER, ANCHOR_CHUNK_ACROSS_THE_SEAM, TARGET_CHUNK));
    }

    @Test
    void towardANullFoldHandsTheCoordinateBack() {
        assertEquals(TARGET.x, NearestCopy.toward(null, Direction.Axis.X, ANCHOR_ACROSS_THE_SEAM.x, TARGET.x));
    }

    @Test
    void towardAFoldThatWrapsNothingHandsTheCoordinateBack() {
        assertEquals(TARGET.x,
                NearestCopy.toward(WorldFolds.NOOP, Direction.Axis.X, ANCHOR_ACROSS_THE_SEAM.x, TARGET.x));
    }

    @Test
    void towardAnAnchorAcrossTheSeamSeatsTheCoordinateInTheLappedCopy() {
        assertEquals(TARGET.x - WIDTH,
                NearestCopy.toward(CYLINDER, Direction.Axis.X, ANCHOR_ACROSS_THE_SEAM.x, TARGET.x));
    }
}
