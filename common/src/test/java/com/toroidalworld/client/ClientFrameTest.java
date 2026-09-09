package com.toroidalworld.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.toroidalworld.client.engine.ClientFrame;
import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldLoopBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

class ClientFrameTest {
    private static final WorldFold TORUS = WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(64)));
    private static final BlockPos CANONICAL = new BlockPos(500, 64, 500);
    private static final BlockPos SAME_LAP_ANCHOR = new BlockPos(480, 64, 500);
    private static final BlockPos LAP_AWAY_ANCHOR = new BlockPos(-500, 64, 500);
    private static final BlockPos HELD_COPY = new BlockPos(-524, 64, 500);
    private static final ChunkPos CANONICAL_CHUNK = new ChunkPos(31, 31);
    private static final ChunkPos SAME_LAP_ANCHOR_CHUNK = new ChunkPos(30, 31);
    private static final ChunkPos LAP_AWAY_ANCHOR_CHUNK = new ChunkPos(-32, 31);
    private static final ChunkPos HELD_COPY_CHUNK = new ChunkPos(-33, 31);

    @Test
    void theSpellingTheClientAlreadyHoldsComesBackUnchanged() {
        assertEquals(CANONICAL, ClientFrame.heldCopy(TORUS, SAME_LAP_ANCHOR, CANONICAL, pos -> true));
    }

    @Test
    void aCopyOneLapAwayResolvesToTheSpellingTheClientHolds() {
        assertEquals(HELD_COPY, ClientFrame.heldCopy(TORUS, LAP_AWAY_ANCHOR, CANONICAL, HELD_COPY::equals));
    }

    @Test
    void aPositionTheClientDoesNotHoldAnswersNothing() {
        assertNull(ClientFrame.heldCopy(TORUS, LAP_AWAY_ANCHOR, CANONICAL, pos -> false));
    }

    @Test
    void withoutAFoldTheArgumentComesBack() {
        assertSame(CANONICAL, ClientFrame.heldCopy(null, LAP_AWAY_ANCHOR, CANONICAL, pos -> false));
    }

    @Test
    void theChunkTheClientAlreadyHoldsComesBackUnchanged() {
        assertEquals(CANONICAL_CHUNK,
                ClientFrame.heldCopy(TORUS, SAME_LAP_ANCHOR_CHUNK, CANONICAL_CHUNK, pos -> true));
    }

    @Test
    void aChunkOneLapAwayResolvesToTheSpellingTheClientHolds() {
        assertEquals(HELD_COPY_CHUNK,
                ClientFrame.heldCopy(TORUS, LAP_AWAY_ANCHOR_CHUNK, CANONICAL_CHUNK, HELD_COPY_CHUNK::equals));
    }

    @Test
    void aChunkTheClientDoesNotHoldAnswersNothing() {
        assertNull(ClientFrame.heldCopy(TORUS, LAP_AWAY_ANCHOR_CHUNK, CANONICAL_CHUNK, pos -> false));
    }

    @Test
    void withoutAFoldTheChunkArgumentComesBack() {
        assertSame(CANONICAL_CHUNK, ClientFrame.heldCopy(null, LAP_AWAY_ANCHOR_CHUNK, CANONICAL_CHUNK, pos -> false));
    }
}
