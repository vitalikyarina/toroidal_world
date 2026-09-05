package com.toroidalworld.compat.sable;

import static com.toroidalworld.compat.CompatFoldFixture.PER_AXIS;
import static com.toroidalworld.compat.CompatFoldFixture.WORLD_CHUNKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;

import net.minecraft.world.level.ChunkPos;

class SableChunkKeysTest {
    private static final int WIDTH_CHUNKS = WORLD_CHUNKS * 2;

    private static final ChunkPos INSIDE = new ChunkPos(-10, 3);
    private static final ChunkPos ONE_LAP_ON = new ChunkPos(-10 + WIDTH_CHUNKS, 3 - WIDTH_CHUNKS);

    @Test
    void aChunkOneLapOnIsKeyedByThePhysicalChunk() {
        assertEquals(INSIDE, SableChunkKeys.physical(PER_AXIS, ONE_LAP_ON));
    }

    @Test
    void aChunkInsideTheBoundsKeepsItsName() {
        assertEquals(INSIDE, SableChunkKeys.physical(PER_AXIS, INSIDE));
    }

    @Test
    void anUnwrappedLevelKeysByTheRawChunk() {
        assertSame(ONE_LAP_ON, SableChunkKeys.physical((WorldFold) null, ONE_LAP_ON));
    }
}
