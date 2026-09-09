package com.toroidalworld.engine.fold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.CoordinateConstants;
import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldLoopBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

class ChunkSeatTest {
    private static final int WIDTH_IN_CHUNKS = 64;
    private static final int WIDTH = WIDTH_IN_CHUNKS * CoordinateConstants.CHUNK_WIDTH;

    private static final WorldFold TORUS = WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(WIDTH_IN_CHUNKS)));
    private static final ChunkPos OWN = new ChunkPos(0, 0);

    private static final BlockPos INSIDE = new BlockPos(5, 64, 5);
    private static final BlockPos ONE_LAP_AWAY = new BlockPos(5 + WIDTH, 64, 5 + WIDTH);
    private static final BlockPos NEXT_CHUNK = new BlockPos(20, 64, 5);
    private static final BlockPos JUST_UNDER_HALF_A_LAP = new BlockPos(500, 64, 5);

    @Test
    void aPositionThisChunkAlreadyHoldsComesBackUnchanged() {
        assertSame(INSIDE, ChunkSeat.onto(TORUS, OWN, INSIDE));
    }

    @Test
    void aPositionOneLapAwaySitsDownInThisChunk() {
        assertEquals(INSIDE, ChunkSeat.onto(TORUS, OWN, ONE_LAP_AWAY));
    }

    @Test
    void aPositionInTheNextChunkIsLeftAlone() {
        assertSame(NEXT_CHUNK, ChunkSeat.onto(TORUS, OWN, NEXT_CHUNK));
    }

    @Test
    void aPositionWhoseNearestCopyMissesThisChunkIsLeftAlone() {
        assertSame(JUST_UNDER_HALF_A_LAP, ChunkSeat.onto(TORUS, OWN, JUST_UNDER_HALF_A_LAP));
    }
}
