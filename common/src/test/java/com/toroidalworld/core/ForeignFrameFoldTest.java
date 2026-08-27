package com.toroidalworld.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

class ForeignFrameFoldTest {
    private static final int WORLD_CHUNKS = 32;
    private static final int PLOT_ORIGIN = 10000;
    private static final int LOG_PLOT_SIZE = 7;
    private static final int LOG_SIDE_LENGTH = 7;
    private static final int PLOT_MIN_CHUNK = PLOT_ORIGIN << LOG_PLOT_SIZE;
    private static final int PLOT_MAX_CHUNK = (PLOT_ORIGIN + (1 << LOG_SIDE_LENGTH)) << LOG_PLOT_SIZE;
    private static final int PLOT_CHUNK = PLOT_MIN_CHUNK + 64;
    private static final int PLOT_BLOCK = PLOT_CHUNK * CoordinateConstants.CHUNK_WIDTH;
    private static final int SEA_LEVEL = 64;

    private static final FlatShape SHAPE = FlatShape.latticeTorus(WorldLoopBounds.ofWidth(WORLD_CHUNKS), FlatShape.NO_SKEW);
    private static final ForeignSpan PLOT_CHUNKS = new ForeignSpan(PLOT_MIN_CHUNK, PLOT_MAX_CHUNK);
    private static final WorldFold FRAMED = WorldFolds.of(SHAPE, List.of(new ForeignFrame(PLOT_CHUNKS, PLOT_CHUNKS)));
    private static final WorldFold BARE = WorldFolds.of(SHAPE);

    private static final ChunkPos PLOT_CHUNK_POS = new ChunkPos(PLOT_CHUNK, PLOT_CHUNK);
    private static final BlockPos PLOT_BLOCK_POS = new BlockPos(PLOT_BLOCK, SEA_LEVEL, PLOT_BLOCK);
    private static final Vec3 PLOT_VEC = new Vec3(PLOT_BLOCK + 0.5, SEA_LEVEL, PLOT_BLOCK + 0.5);
    private static final BlockPos WORLD_BLOCK_POS = new BlockPos(10, SEA_LEVEL, -10);
    private static final Vec3 WORLD_VEC = new Vec3(10.5, SEA_LEVEL, -9.5);

    @Test
    void thePlotGeometryIsTheOneTheCardNames() {
        assertEquals(1_280_064, PLOT_CHUNK);
        assertEquals(20_481_024, PLOT_BLOCK);
    }

    @Test
    void aBareFoldPullsThePlotIntoTheWorld() {
        assertNotEquals(PLOT_CHUNK_POS, BARE.fold(PLOT_CHUNK_POS));
        assertNotEquals(PLOT_BLOCK_POS, BARE.fold(PLOT_BLOCK_POS));
        assertTrue(BARE.isOver(PLOT_BLOCK_POS));
    }

    @Test
    void wholePositionFoldsHandThePlotPositionBack() {
        assertSame(PLOT_CHUNK_POS, FRAMED.fold(PLOT_CHUNK_POS));
        assertSame(PLOT_BLOCK_POS, FRAMED.fold(PLOT_BLOCK_POS));
        assertSame(PLOT_VEC, FRAMED.fold(PLOT_VEC));

        SectionPos section = SectionPos.of(PLOT_CHUNK, 4, PLOT_CHUNK);
        assertSame(section, FRAMED.fold(section));
    }

    @Test
    void packedKeysOfAPlotPositionAreUntouched() {
        long chunkKey = ChunkPos.asLong(PLOT_CHUNK, PLOT_CHUNK);
        long blockNode = BlockPos.asLong(PLOT_BLOCK, SEA_LEVEL, PLOT_BLOCK);
        long sectionNode = SectionPos.asLong(PLOT_CHUNK, 4, PLOT_CHUNK);

        assertEquals(chunkKey, FRAMED.foldChunkKey(chunkKey));
        assertEquals(blockNode, FRAMED.foldBlockNode(blockNode));
        assertEquals(sectionNode, FRAMED.foldSectionNode(sectionNode));

        assertNotEquals(chunkKey, BARE.foldChunkKey(chunkKey));
        assertNotEquals(blockNode, BARE.foldBlockNode(blockNode));
    }

    @Test
    void aPlotPositionIsNotOverTheBounds() {
        assertFalse(FRAMED.isOver(PLOT_CHUNK_POS));
        assertFalse(FRAMED.isOver(PLOT_BLOCK_POS));
        assertFalse(FRAMED.isOver(PLOT_VEC));
        assertEquals(0, FRAMED.chunkOvershoot(PLOT_CHUNK_POS));
    }

    @Test
    void nearestCopyLeavesEitherOperandInThePlotWhereItIs() {
        assertSame(PLOT_BLOCK_POS, FRAMED.nearestCopy(WORLD_BLOCK_POS, PLOT_BLOCK_POS));
        assertSame(WORLD_BLOCK_POS, FRAMED.nearestCopy(PLOT_BLOCK_POS, WORLD_BLOCK_POS));
        assertSame(PLOT_VEC, FRAMED.nearestCopy(WORLD_VEC, PLOT_VEC));
        assertSame(WORLD_VEC, FRAMED.nearestCopy(PLOT_VEC, WORLD_VEC));
        assertSame(PLOT_CHUNK_POS, FRAMED.nearestCopy(new ChunkPos(0, 0), PLOT_CHUNK_POS));

        assertNotEquals(PLOT_BLOCK_POS, BARE.nearestCopy(WORLD_BLOCK_POS, PLOT_BLOCK_POS));
    }

    @Test
    void deltaAndDistanceToThePlotAreReadRaw() {
        Vec3 delta = FRAMED.foldDelta(WORLD_VEC, PLOT_VEC);
        assertEquals(PLOT_VEC.subtract(WORLD_VEC), delta);

        double raw = WORLD_VEC.distanceToSqr(PLOT_VEC);
        assertEquals(raw, FRAMED.sqrDistance(WORLD_VEC, PLOT_VEC), 0.0);
        assertEquals(raw, FRAMED.sqrDistance(PLOT_VEC, WORLD_VEC), 0.0);
    }

    @Test
    void aPlotBoxIsNeitherMovedNorSplit() {
        AABB plotBox = new AABB(PLOT_BLOCK_POS).inflate(3.0);
        WorldFold.Folded<AABB> folded = FRAMED.foldBox(WORLD_VEC, plotBox);
        assertSame(plotBox, folded.value());

        assertFalse(FRAMED.crossesBounds(plotBox));
        List<WorldFold.Folded<AABB>> pieces = FRAMED.split(plotBox);
        assertEquals(1, pieces.size());
        assertSame(plotBox, pieces.get(0).value());

        assertTrue(BARE.crossesBounds(plotBox));
    }

    @Test
    void oneLapOutStillFolds() {
        int width = WorldLoopBounds.ofWidth(WORLD_CHUNKS).chunkWidth(Direction.Axis.X);
        ChunkPos oneLapOut = new ChunkPos(8 + width, -3);
        assertEquals(new ChunkPos(8, -3), FRAMED.fold(oneLapOut));

        BlockPos oneLapOutBlock = new BlockPos(10 + width * CoordinateConstants.CHUNK_WIDTH, SEA_LEVEL, -10);
        assertEquals(WORLD_BLOCK_POS, FRAMED.fold(oneLapOutBlock));
        assertEquals(WORLD_BLOCK_POS, FRAMED.nearestCopy(WORLD_BLOCK_POS, oneLapOutBlock));
    }

    @Test
    void theMirrorImageOfThePlotBelongsToNoFrameAndFolds() {
        ChunkPos negated = new ChunkPos(-PLOT_CHUNK, -PLOT_CHUNK);
        assertEquals(BARE.fold(negated), FRAMED.fold(negated));
        assertNotEquals(negated, FRAMED.fold(negated));

        BlockPos negatedBlock = new BlockPos(-PLOT_BLOCK, SEA_LEVEL, -PLOT_BLOCK);
        assertEquals(BARE.fold(negatedBlock), FRAMED.fold(negatedBlock));
        assertTrue(FRAMED.isOver(negatedBlock));
    }

    @Test
    void theFrameHoldsPerAxis() {
        ChunkPos mixed = new ChunkPos(PLOT_CHUNK, 8 + WORLD_CHUNKS);
        assertEquals(new ChunkPos(PLOT_CHUNK, 8), FRAMED.fold(mixed));
    }

    @Test
    void theFrameEdgesAreHalfOpen() {
        assertEquals(PLOT_MIN_CHUNK, FRAMED.fold(new ChunkPos(PLOT_MIN_CHUNK, 0)).x);
        assertEquals(PLOT_MAX_CHUNK - 1, FRAMED.fold(new ChunkPos(PLOT_MAX_CHUNK - 1, 0)).x);
        assertNotEquals(PLOT_MIN_CHUNK - 1, FRAMED.fold(new ChunkPos(PLOT_MIN_CHUNK - 1, 0)).x);
        assertNotEquals(PLOT_MAX_CHUNK, FRAMED.fold(new ChunkPos(PLOT_MAX_CHUNK, 0)).x);
    }

    @Test
    void framesLeaveTheWorldsOwnBoundsAlone() {
        assertEquals(BARE.bounds(), FRAMED.bounds());
        assertTrue(FRAMED.isWrapped());
        assertEquals(BARE.maxViewDistance(), FRAMED.maxViewDistance());
    }
}
