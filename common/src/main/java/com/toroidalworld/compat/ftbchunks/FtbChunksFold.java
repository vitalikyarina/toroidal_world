package com.toroidalworld.compat.ftbchunks;

import java.util.Arrays;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.api.v1.ToroidalShape;
import com.toroidalworld.compat.AxisCopies;
import com.toroidalworld.compat.ClientShapes;
import com.toroidalworld.compat.FullscreenZoomFloor;
import com.toroidalworld.compat.MapCopyBudget;
import com.toroidalworld.engine.seam.MapSurfaceCopies.Copies;

import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.client.map.MapRegion;
import dev.ftb.mods.ftbchunks.client.map.MapRegionData;
import dev.ftb.mods.ftblibrary.math.XZ;

public final class FtbChunksFold {
    static final int REGION_CHUNKS = 32;
    private static final int REGION_BLOCKS = 512;
    private static final int CHUNK_BLOCKS = 16;

    private static final int MINIMAP_CHUNKS = 15;
    private static final int MINIMAP_CENTRE_CHUNK = 7;

    private static int largeMapRangeX;
    private static int largeMapRangeZ;

    public static int foldChunk(Direction.Axis axis, int chunk) {
        ToroidalShape shape = ClientShapes.current();
        return shape == null ? chunk : shape.foldChunk(axis, chunk);
    }

    public static int foldBlock(Direction.Axis axis, int coord) {
        ToroidalShape shape = ClientShapes.current();
        return shape == null ? coord : shape.foldBlock(axis, coord);
    }

    public static XZ chunkOf(int chunkX, int chunkZ) {
        return XZ.of(foldChunk(Direction.Axis.X, chunkX), foldChunk(Direction.Axis.Z, chunkZ));
    }

    public static XZ chunkOf(ChunkPos pos) {
        return chunkOf(pos.x, pos.z);
    }

    public static XZ chunkOf(XZ chunk) {
        return chunkOf(chunk.x(), chunk.z());
    }

    public static XZ regionOfChunk(int chunkX, int chunkZ) {
        return XZ.regionFromChunk(foldChunk(Direction.Axis.X, chunkX), foldChunk(Direction.Axis.Z, chunkZ));
    }

    public static XZ regionOfChunk(ChunkPos pos) {
        return regionOfChunk(pos.x, pos.z);
    }

    public static XZ regionOfBlock(int x, int z) {
        return XZ.regionFromBlock(foldBlock(Direction.Axis.X, x), foldBlock(Direction.Axis.Z, z));
    }

    public static int loopedAxes() {
        ToroidalShape shape = ClientShapes.current();
        if (shape == null) {
            return 0;
        }

        return (shape.loops(Direction.Axis.X) ? 1 : 0) + (shape.loops(Direction.Axis.Z) ? 1 : 0);
    }

    public static double worldPixelPeriod(Direction.Axis axis, int regionTilePixels) {
        return worldPixelPeriod(ClientShapes.current(), axis, regionTilePixels);
    }

    static double worldPixelPeriod(@Nullable ToroidalShape shape, Direction.Axis axis, int regionTilePixels) {
        if (shape == null || !shape.loops(axis)) {
            return 0.0;
        }

        return shape.widthBlocks(axis) * (regionTilePixels / (double) REGION_BLOCKS);
    }

    public static int[] seamPixels(Direction.Axis axis, int originPixel, int regionTilePixels, int viewFrom, int viewTo) {
        return seamPixels(copies(axis), originPixel, regionTilePixels, viewFrom, viewTo);
    }

    static int[] seamPixels(AxisCopies copies, int originPixel, int regionTilePixels, int viewFrom, int viewTo) {
        double pixelsPerBlock = regionTilePixels / (double) REGION_BLOCKS;
        int[] seams = copies.seams((int) Math.floor((viewFrom - originPixel) / pixelsPerBlock),
                (int) Math.ceil((viewTo - originPixel) / pixelsPerBlock));
        int[] pixels = new int[seams.length];
        for (int i = 0; i < seams.length; i++) {
            pixels[i] = originPixel + (int) Math.round(seams[i] * pixelsPerBlock);
        }

        return pixels;
    }

    public static int zoomFloor() {
        ToroidalShape shape = ClientShapes.current();
        return shape == null ? 0 : FullscreenZoomFloor.ftbChunksZoom(shape);
    }

    public static void recordLargeMapCopyRange(int rangeX, int rangeZ) {
        largeMapRangeX = rangeX;
        largeMapRangeZ = rangeZ;
    }

    public static Copies largeMapCopies() {
        return MapCopyBudget.painted(copies(Direction.Axis.X), largeMapRangeX,
                copies(Direction.Axis.Z), largeMapRangeZ);
    }

    public static Vec3 foldPosition(Vec3 position) {
        ToroidalShape shape = ClientShapes.current();
        if (shape == null) {
            return position;
        }

        return new Vec3(shape.foldCoord(Direction.Axis.X, position.x), position.y,
                shape.foldCoord(Direction.Axis.Z, position.z));
    }

    public record TileBlit(int x, int y, int width, int height, float u0, float v0, float u1, float v1) {
    }

    public record SeamView(int originX, int originY, int tilePixels, int x, int y, int width, int height) {
    }

    // The part of a region tile that lies inside the world, as a screen rectangle and its texture window. The rest
    // of the region image is opaque black, so a tile blitted whole paints over whatever copy lies beside it.
    public static @Nullable TileBlit worldPartOf(int regionX, int regionZ, int x, int y, int width, int height) {
        int[] spanX = worldSpanInRegion(Direction.Axis.X, regionX);
        int[] spanZ = worldSpanInRegion(Direction.Axis.Z, regionZ);
        if (spanX[0] >= spanX[1] || spanZ[0] >= spanZ[1]) {
            return null;
        }

        double pixelsPerBlockX = width / (double) REGION_BLOCKS;
        double pixelsPerBlockY = height / (double) REGION_BLOCKS;
        return new TileBlit(
                x + (int) Math.floor(spanX[0] * pixelsPerBlockX),
                y + (int) Math.floor(spanZ[0] * pixelsPerBlockY),
                (int) Math.ceil((spanX[1] - spanX[0]) * pixelsPerBlockX),
                (int) Math.ceil((spanZ[1] - spanZ[0]) * pixelsPerBlockY),
                textureEdge(spanX[0]), textureEdge(spanZ[0]), textureEdge(spanX[1]), textureEdge(spanZ[1]));
    }

    private static float textureEdge(int blockOffset) {
        return blockOffset / (float) REGION_BLOCKS;
    }

    static int[] worldSpanInRegion(Direction.Axis axis, int region) {
        ToroidalShape shape = ClientShapes.current();
        int regionStart = region * REGION_BLOCKS;
        if (shape == null || !shape.loops(axis)) {
            return new int[] {0, REGION_BLOCKS};
        }

        int from = Math.max(shape.minBlock(axis), regionStart) - regionStart;
        int to = Math.min(shape.maxBlock(axis), regionStart + REGION_BLOCKS) - regionStart;
        return new int[] {from, Math.max(from, to)};
    }

    // FTB shades a pixel against its north and west neighbours inside the same region image, so the world's west
    // and north edge columns would read an unexplored neighbour on every copy. The edge column of each world side is
    // mirrored one block past the opposite side, into the region that holds that block; the halo lies outside
    // worldSpanInRegion and is never blitted.
    public static void mirrorSeamEdges(MapDimension dimension, int foldedChunkX, int foldedChunkZ) {
        ToroidalShape shape = ClientShapes.current();
        if (shape == null) {
            return;
        }

        if (shape.loops(Direction.Axis.X)) {
            if (foldedChunkX == shape.maxChunk(Direction.Axis.X) - 1) {
                mirrorColumn(dimension, shape.maxBlock(Direction.Axis.X) - 1, shape.minBlock(Direction.Axis.X) - 1,
                        foldedChunkZ);
            }

            if (foldedChunkX == shape.minChunk(Direction.Axis.X)) {
                mirrorColumn(dimension, shape.minBlock(Direction.Axis.X), shape.maxBlock(Direction.Axis.X),
                        foldedChunkZ);
            }
        }

        if (shape.loops(Direction.Axis.Z)) {
            if (foldedChunkZ == shape.maxChunk(Direction.Axis.Z) - 1) {
                mirrorRow(dimension, shape.maxBlock(Direction.Axis.Z) - 1, shape.minBlock(Direction.Axis.Z) - 1,
                        foldedChunkX);
            }

            if (foldedChunkZ == shape.minChunk(Direction.Axis.Z)) {
                mirrorRow(dimension, shape.minBlock(Direction.Axis.Z), shape.maxBlock(Direction.Axis.Z),
                        foldedChunkX);
            }
        }
    }

    private static void mirrorColumn(MapDimension dimension, int sourceBlockX, int haloBlockX, int chunkZ) {
        int regionZ = Math.floorDiv(chunkZ, REGION_CHUNKS);
        MapRegionData source = dimension.getRegion(XZ.of(Math.floorDiv(sourceBlockX, REGION_BLOCKS), regionZ))
                .getDataBlocking();
        MapRegion halo = dimension.getRegion(XZ.of(Math.floorDiv(haloBlockX, REGION_BLOCKS), regionZ));
        MapRegionData target = halo.getDataBlocking();
        int sourceX = Math.floorMod(sourceBlockX, REGION_BLOCKS);
        int haloX = Math.floorMod(haloBlockX, REGION_BLOCKS);
        int firstRow = Math.floorMod(chunkZ, REGION_CHUNKS) * CHUNK_BLOCKS;
        for (int row = firstRow; row < firstRow + CHUNK_BLOCKS; row++) {
            copyPixel(source, sourceX + row * REGION_BLOCKS, target, haloX + row * REGION_BLOCKS);
        }

        halo.update(true);
    }

    private static void mirrorRow(MapDimension dimension, int sourceBlockZ, int haloBlockZ, int chunkX) {
        int regionX = Math.floorDiv(chunkX, REGION_CHUNKS);
        MapRegionData source = dimension.getRegion(XZ.of(regionX, Math.floorDiv(sourceBlockZ, REGION_BLOCKS)))
                .getDataBlocking();
        MapRegion halo = dimension.getRegion(XZ.of(regionX, Math.floorDiv(haloBlockZ, REGION_BLOCKS)));
        MapRegionData target = halo.getDataBlocking();
        int sourceRow = Math.floorMod(sourceBlockZ, REGION_BLOCKS) * REGION_BLOCKS;
        int haloRow = Math.floorMod(haloBlockZ, REGION_BLOCKS) * REGION_BLOCKS;
        int firstColumn = Math.floorMod(chunkX, REGION_CHUNKS) * CHUNK_BLOCKS;
        for (int column = firstColumn; column < firstColumn + CHUNK_BLOCKS; column++) {
            copyPixel(source, sourceRow + column, target, haloRow + column);
        }

        halo.update(true);
    }

    private static void copyPixel(MapRegionData source, int from, MapRegionData target, int to) {
        target.height[to] = source.height[from];
        target.waterLightAndBiome[to] = source.waterLightAndBiome[from];
        target.foliage[to] = source.foliage[from];
        target.grass[to] = source.grass[from];
        target.water[to] = source.water[from];
        target.setBlockIndex(to, source.getBlockIndex(from));
    }

    public static AxisCopies copies(Direction.Axis axis) {
        ToroidalShape shape = ClientShapes.current();
        return shape == null ? AxisCopies.UNBOUNDED : AxisCopies.of(shape, axis);
    }

    public static int[] minimapSplits(Direction.Axis axis, int centreChunk, int[] unfolded) {
        return minimapSplits(ClientShapes.current(), axis, centreChunk, unfolded);
    }

    // FTB's own loop reads any number of cuts, so a seam adds one wherever the folded window stops being one run
    // inside one region image.
    static int[] minimapSplits(@Nullable ToroidalShape shape, Direction.Axis axis, int centreChunk, int[] unfolded) {
        if (shape == null || !shape.loops(axis)) {
            return unfolded;
        }

        int[] cuts = new int[MINIMAP_CHUNKS + 1];
        int count = 0;
        cuts[count++] = 0;
        int previous = shape.foldChunk(axis, centreChunk - MINIMAP_CENTRE_CHUNK);
        for (int chunk = 1; chunk < MINIMAP_CHUNKS; chunk++) {
            int folded = shape.foldChunk(axis, centreChunk + chunk - MINIMAP_CENTRE_CHUNK);
            if (folded != previous + 1 || regionOf(folded) != regionOf(previous)) {
                cuts[count++] = chunk;
            }

            previous = folded;
        }

        cuts[count++] = MINIMAP_CHUNKS;
        return Arrays.copyOf(cuts, count);
    }

    private static int regionOf(int chunk) {
        return Math.floorDiv(chunk, REGION_CHUNKS);
    }

    private FtbChunksFold() {
    }
}
