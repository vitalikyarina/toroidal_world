package com.toroidalworld.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.toroidalworld.options.WorldLoopBounds;

import net.minecraft.world.level.ChunkPos;

// Proves the harness, not the math: WrapDomain compiles as plain Java, so the ChunkPos round-trip is the part that
// shows the Minecraft classes are on the test classpath. Real coverage lives in the sibling test classes.
class WrapDomainSmokeTest {
    @Test
    void wrapFoldsPastTheUpperBound() {
        WrapDomain domain = new WrapDomain(-32, 32);

        assertEquals(-32, domain.wrap(32));
        assertEquals(31, domain.wrap(-33));
    }

    @Test
    void transformerWrapsChunkPosAcrossTheSeam() {
        WorldLoopTransformer transformer = new WorldLoopTransformer(WorldLoopBounds.ofWidth(4));

        assertEquals(new ChunkPos(-2, 0), transformer.chunks.wrap(new ChunkPos(2, 0)));
    }
}
