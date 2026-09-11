package com.toroidalworld.compat.ftbchunks;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.toroidalworld.api.v1.TestShapes;
import com.toroidalworld.api.v1.ToroidalShape;
import com.toroidalworld.compat.AxisCopies;
import com.toroidalworld.core.CoordinateConstants;
import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldLoopBounds;
import com.toroidalworld.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.Direction;

import dev.ftb.mods.ftblibrary.math.XZ;

class FtbChunksFoldTest {
    private static final int[] ONE_PIECE = {0, 15};

    @Test
    void anUnboundedAxisKeepsTheCutsFtbMade() {
        int[] ftbCuts = {0, 5, 15};
        assertSame(ftbCuts, FtbChunksFold.minimapSplits(cylinder(512), Direction.Axis.X, 34, ftbCuts),
                "an unbounded axis was re-cut");
        assertSame(ftbCuts, FtbChunksFold.minimapSplits(null, Direction.Axis.X, 34, ftbCuts),
                "an unwrapped world was re-cut");
    }

    @Test
    void aWindowInsideOneRegionIsOnePiece() {
        assertArrayEquals(ONE_PIECE, FtbChunksFold.minimapSplits(torus(1024), Direction.Axis.X, 7, ONE_PIECE),
                "chunks 0..14 of a 64-chunk world sit in region 0");
    }

    @Test
    void aRegionBoundaryCutsWhereFtbWouldCutItself() {
        assertArrayEquals(new int[] {0, 5, 15}, FtbChunksFold.minimapSplits(torus(1024), Direction.Axis.X, 34, ONE_PIECE),
                "chunks 27..41 cross chunk 32, five in");
    }

    @Test
    void theSeamCutsEvenWhereTheRegionDoesNotChange() {
        assertArrayEquals(new int[] {0, 9, 15}, FtbChunksFold.minimapSplits(torus(512), Direction.Axis.X, 30, ONE_PIECE),
                "a 32-chunk world is one region, and chunk 32 wraps to 0 nine chunks in");
        assertArrayEquals(new int[] {0, 11, 15}, FtbChunksFold.minimapSplits(torus(256), Direction.Axis.X, 12, ONE_PIECE),
                "a 16-chunk world wraps at chunk 16, eleven chunks in");
    }

    @Test
    void theCopyPeriodIsTheWorldMeasuredInTilePixels() {
        assertEquals(64.0, FtbChunksFold.worldPixelPeriod(torus(512), Direction.Axis.X, 64), 1e-12,
                "512 blocks at 64 px per 512-block region is 64 px");
        assertEquals(64.0, FtbChunksFold.worldPixelPeriod(torus(256), Direction.Axis.X, 128), 1e-12,
                "256 blocks at 128 px per region is 64 px");
        assertEquals(0.0, FtbChunksFold.worldPixelPeriod(cylinder(512), Direction.Axis.X, 64), 1e-12,
                "an unbounded axis has no period");
        assertEquals(0.0, FtbChunksFold.worldPixelPeriod(null, Direction.Axis.X, 64), 1e-12,
                "an unwrapped world has no period");
    }

    @Test
    void everySeamInTheViewGetsItsPixel() {
        AxisCopies world = AxisCopies.looped(-256, 512);
        assertArrayEquals(new int[] {68, 132, 196}, FtbChunksFold.seamPixels(world, 100, 64, 68, 196),
                "block 0 at px 100 and 64 px per lap: the view 68..196 holds laps 0 and 1, outlined by three seams");
        assertArrayEquals(new int[] {68, 132}, FtbChunksFold.seamPixels(world, 100, 64, 80, 120),
                "a view inside lap 0 still gets both of its seams, off-view or not");
        assertArrayEquals(new int[0], FtbChunksFold.seamPixels(AxisCopies.UNBOUNDED, 100, 64, 68, 196),
                "an unbounded axis has no seam");
    }

    @Test
    void aPlayerALapOutComesBackBesideTheSelection() {
        assertEquals(15, FtbChunksFold.nearestChunk(torus(512), Direction.Axis.X, 14, 47),
                "chunk 47 is chunk 15 one lap out of a 32-chunk world, and 15 is what sits beside 14");
        assertEquals(15, FtbChunksFold.nearestChunk(torus(512), Direction.Axis.X, 14, 15),
                "a chunk already nearest the reference is itself");
    }

    @Test
    void theCopyAcrossTheSeamIsTheNearOne() {
        assertEquals(-17, FtbChunksFold.nearestChunk(torus(512), Direction.Axis.X, -16, 15),
                "chunk 15 is one chunk west of chunk -16 through the seam, so it reads as -17 beside it");
    }

    @Test
    void anAxisThatDoesNotLoopKeepsTheChunk() {
        assertEquals(47, FtbChunksFold.nearestChunk(cylinder(512), Direction.Axis.X, 14, 47),
                "an unbounded axis has no other copy to come back from");
        assertEquals(47, FtbChunksFold.nearestChunk(null, Direction.Axis.X, 14, 47),
                "an unwrapped world has no other copy to come back from");
    }

    @Test
    void aRegionInsideTheWorldIsItself() {
        assertArrayEquals(new int[] {0}, FtbChunksFold.canonicalRegions(torus(1024), Direction.Axis.X, 0),
                "chunks 0..31 of a 64-chunk world are region 0");
        assertArrayEquals(new int[] {1}, FtbChunksFold.canonicalRegions(torus(1024), Direction.Axis.X, 1),
                "chunks 32..63 of a 64-chunk world are region 1");
    }

    @Test
    void aRegionAWholeLapOutFoldsOntoTheOneItMirrors() {
        assertArrayEquals(new int[] {0}, FtbChunksFold.canonicalRegions(torus(1024), Direction.Axis.X, 2),
                "chunks 64..95 wrap onto 0..31");
        assertArrayEquals(new int[] {1}, FtbChunksFold.canonicalRegions(torus(1024), Direction.Axis.X, -1),
                "chunks -32..-1 wrap onto 32..63");
    }

    @Test
    void aRegionSplitsWhereTheWorldIsNotAWholeNumberOfRegions() {
        assertArrayEquals(new int[] {1, 0}, FtbChunksFold.canonicalRegions(torus(768), Direction.Axis.X, 1),
                "a 48-chunk world leaves chunks 32..47 in region 1 and wraps 48..63 onto region 0");
        assertArrayEquals(new int[] {0}, FtbChunksFold.canonicalRegions(torus(768), Direction.Axis.X, 0),
                "chunks 0..31 of a 48-chunk world stay in region 0");
    }

    @Test
    void anAxisThatDoesNotLoopKeepsTheRegion() {
        assertArrayEquals(new int[] {7}, FtbChunksFold.canonicalRegions(cylinder(512), Direction.Axis.X, 7),
                "an unbounded axis has nothing to fold onto");
        assertArrayEquals(new int[] {7}, FtbChunksFold.canonicalRegions(null, Direction.Axis.X, 7),
                "an unwrapped world has nothing to fold onto");
    }

    @Test
    void aSelectionInsideTheWorldKeepsItsChunks() {
        assertEquals(Set.of(XZ.of(3, 4), XZ.of(5, 6)),
                FtbChunksFold.foldedChunks(torus(1024), Set.of(XZ.of(3, 4), XZ.of(5, 6))),
                "chunks 0..63 of a 64-chunk world are already canonical");
    }

    @Test
    void aSelectionAcrossTheSeamFoldsChunkByChunk() {
        assertEquals(Set.of(XZ.of(63, 0), XZ.of(0, 0), XZ.of(63, 5)),
                FtbChunksFold.foldedChunks(torus(1024), Set.of(XZ.of(63, 0), XZ.of(64, 0), XZ.of(-1, 5))),
                "a window straddling chunk 64 wraps its far half onto 0, one chunk at a time");
    }

    @Test
    void aSelectionOnALappedClientComesBackInsideTheWorld() {
        assertEquals(Set.of(XZ.of(7, 9)),
                FtbChunksFold.foldedChunks(torus(1024), Set.of(XZ.of(7 + 64 * 3, 9 - 64 * 2))),
                "three laps out on x and two back on z is the same chunk");
    }

    @Test
    void anAxisThatDoesNotLoopKeepsItsHalfOfTheKey() {
        assertEquals(Set.of(XZ.of(9000, 1)),
                FtbChunksFold.foldedChunks(cylinder(512), Set.of(XZ.of(9000, 33))),
                "an unbounded x is carried through while z folds onto a 32-chunk world");
    }

    @Test
    void anUnwrappedWorldKeepsTheSelectionItself() {
        Set<XZ> selection = Set.of(XZ.of(9000, 33));
        assertSame(selection, FtbChunksFold.foldedChunks(null, selection), "an unwrapped world was rebuilt");
    }

    private static ToroidalShape torus(int widthBlocks) {
        return shape(looped(widthBlocks), looped(widthBlocks));
    }

    private static ToroidalShape cylinder(int widthBlocksZ) {
        return shape(AxisBounds.Unbounded.INSTANCE, looped(widthBlocksZ));
    }

    private static ToroidalShape shape(AxisBounds x, AxisBounds z) {
        return TestShapes.of(WorldFolds.of(FlatShape.torus(new WorldLoopBounds(x, z))));
    }

    private static AxisBounds looped(int widthBlocks) {
        return new AxisBounds.Looped(0, widthBlocks / CoordinateConstants.CHUNK_WIDTH);
    }
}
