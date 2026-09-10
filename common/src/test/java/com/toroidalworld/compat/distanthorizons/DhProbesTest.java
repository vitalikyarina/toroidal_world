package com.toroidalworld.compat.distanthorizons;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.toroidalworld.api.v1.TestShapes;
import com.toroidalworld.api.v1.ToroidalShape;
import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldLoopBounds;
import com.toroidalworld.core.WorldLoopBounds.AxisBounds;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos;

import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;

class DhProbesTest {
    private static final int WIDTH_CHUNKS = 64;
    private static final int WIDTH_BLOCKS = WIDTH_CHUNKS * 16;

    private static ToroidalShape cylinder() {
        AxisBounds.Looped looped = new AxisBounds.Looped(0, WIDTH_CHUNKS);
        return TestShapes.of(WorldFolds.of(
                FlatShape.torus(new WorldLoopBounds(looped, AxisBounds.Unbounded.INSTANCE))));
    }

    @BeforeEach
    void openTheGates() {
        DhProbes.resetKeyGates();
    }

    @Test
    void theKeyPeriodLineSurvivesACylinder() {
        assertDoesNotThrow(() -> DhProbes.keyPeriod(cylinder(), DhKeys.LEAF));
    }

    @Test
    void theLoopingAxisNamesItsWidthAndTheOtherSaysNone() {
        ToroidalShape shape = cylinder();
        assertEquals(String.valueOf(WIDTH_BLOCKS), DhProbes.widthValue(shape, Direction.Axis.X));
        assertEquals("none", DhProbes.widthValue(shape, Direction.Axis.Z));
    }

    @Test
    void theFirstFoldedKeyOfATypeIsLoggedAndTheSecondIsNot() {
        ToroidalShape shape = cylinder();
        DhKeys.foldChunk(shape, new ChunkPos(WIDTH_CHUNKS, 1));
        DhKeys.foldChunk(shape, new ChunkPos(WIDTH_CHUNKS + 3, 2));
        assertEquals(1, DhProbes.foldedKeyLines(DhProbes.Key.CHUNK));
    }

    @Test
    void aKeyInsideTheWorldIsCountedAndNeverLogged() {
        ToroidalShape shape = cylinder();
        DhKeys.foldChunk(shape, new ChunkPos(1, 1));
        DhKeys.foldChunk(shape, new ChunkPos(2, 2));
        assertEquals(2, DhProbes.unchangedKeys(DhProbes.Key.CHUNK));
        assertEquals(0, DhProbes.foldedKeyLines(DhProbes.Key.CHUNK));
    }

    @Test
    void theUnchangedCountRidesTheFoldedLine() {
        ToroidalShape shape = cylinder();
        DhKeys.foldChunk(shape, new ChunkPos(1, 1));
        DhKeys.foldChunk(shape, new ChunkPos(2, 2));
        DhKeys.foldChunk(shape, new ChunkPos(WIDTH_CHUNKS, 1));
        assertEquals("[dh-compat] folded_key key_type=chunk raw=64,1 folded=0,1 unchanged_keys=2",
                DhProbes.foldedKeyLine(DhProbes.Key.CHUNK, "64,1", "0,1"));
    }

    @Test
    void eachKeyTypeKeepsItsOwnGate() {
        ToroidalShape shape = cylinder();
        int sectionsPerWorld = WIDTH_BLOCKS / DhFold.sectionWidthBlocks(DhKeys.LEAF);
        DhKeys.foldSection(shape, DhSectionPos.encode(DhKeys.LEAF, sectionsPerWorld, 1));
        assertEquals(1, DhProbes.foldedKeyLines(DhProbes.Key.SECTION));
        assertEquals(0, DhProbes.foldedKeyLines(DhProbes.Key.CHUNK));
        assertEquals(0, DhProbes.foldedKeyLines(DhProbes.Key.BEACON));
    }

    @Test
    void aBeaconKeyIsNamedInBlocks() {
        assertEquals("1024,64,1", DhProbes.beaconValue(new DhBlockPos(WIDTH_BLOCKS, 64, 1)));
    }

    @Test
    void aChunkKeyIsNamedInChunks() {
        assertEquals("64,1", DhProbes.chunkValue(WIDTH_CHUNKS, 1));
    }
}
