package com.toroidalworld.compat.sable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.world.level.ChunkPos;

class SableChunkKeysTest {
    private static final int HALF_WIDTH_CHUNKS = 16;
    private static final int WIDTH_CHUNKS = HALF_WIDTH_CHUNKS * 2;
    private static final WorldFold FOLD = WorldFolds.of(FlatShape.latticeTorus(
            new WorldLoopBounds(-HALF_WIDTH_CHUNKS, HALF_WIDTH_CHUNKS, -HALF_WIDTH_CHUNKS, HALF_WIDTH_CHUNKS),
            FlatShape.NO_SKEW));

    private static final ChunkPos INSIDE = new ChunkPos(-10, 3);
    private static final ChunkPos ONE_LAP_ON = new ChunkPos(-10 + WIDTH_CHUNKS, 3 - WIDTH_CHUNKS);

    @Test
    void aChunkOneLapOnIsKeyedByThePhysicalChunk() {
        assertEquals(INSIDE, SableChunkKeys.physical(FOLD, ONE_LAP_ON));
    }

    @Test
    void aChunkInsideTheBoundsKeepsItsName() {
        assertEquals(INSIDE, SableChunkKeys.physical(FOLD, INSIDE));
    }

    @Test
    void anUnwrappedLevelKeysByTheRawChunk() {
        assertSame(ONE_LAP_ON, SableChunkKeys.physical((WorldFold) null, ONE_LAP_ON));
    }
}
