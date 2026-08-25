package com.toroidalworld.api;

import com.toroidalworld.core.CoordinateConstants;
import com.toroidalworld.core.FoldOrientation;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

final class WorldFoldToroidalShape implements ToroidalShape {
    private final WorldFold fold;

    WorldFoldToroidalShape(WorldFold fold) {
        this.fold = fold;
    }

    @Override
    public boolean loops(Direction.Axis axis) {
        return boundsOf(axis) instanceof AxisBounds.Looped;
    }

    @Override
    public int minChunk(Direction.Axis axis) {
        return looped(axis).minChunk();
    }

    @Override
    public int maxChunk(Direction.Axis axis) {
        return looped(axis).maxChunk();
    }

    @Override
    public int widthChunks(Direction.Axis axis) {
        return looped(axis).chunkWidth();
    }

    @Override
    public int minBlock(Direction.Axis axis) {
        return looped(axis).minChunk() * CoordinateConstants.CHUNK_WIDTH;
    }

    @Override
    public int maxBlock(Direction.Axis axis) {
        return looped(axis).maxChunk() * CoordinateConstants.CHUNK_WIDTH;
    }

    @Override
    public int widthBlocks(Direction.Axis axis) {
        return looped(axis).chunkWidth() * CoordinateConstants.CHUNK_WIDTH;
    }

    @Override
    public double foldCoord(Direction.Axis axis, double coord) {
        return axis == Direction.Axis.Y ? coord : this.fold.blockDomain(axis).wrap(coord);
    }

    @Override
    public int foldBlock(Direction.Axis axis, int coord) {
        return axis == Direction.Axis.Y ? coord : this.fold.blockDomain(axis).wrap(coord);
    }

    @Override
    public int foldChunk(Direction.Axis axis, int chunk) {
        return axis == Direction.Axis.Y ? chunk : this.fold.chunkDomain(axis).wrap(chunk);
    }

    @Override
    public double nearestCoord(Direction.Axis axis, double ref, double coord) {
        return axis == Direction.Axis.Y ? coord : this.fold.blockDomain(axis).unwrapAround(ref, coord);
    }

    @Override
    public BlockPos fold(BlockPos pos) {
        return this.fold.fold(pos);
    }

    @Override
    public Vec3 fold(Vec3 pos) {
        return this.fold.fold(pos);
    }

    @Override
    public ChunkPos fold(ChunkPos pos) {
        return this.fold.fold(pos);
    }

    @Override
    public Vec3 nearestCopy(Vec3 ref, Vec3 target) {
        return this.fold.nearestCopy(ref, target);
    }

    @Override
    public BlockPos nearestCopy(BlockPos ref, BlockPos target) {
        return this.fold.nearestCopy(ref, target);
    }

    @Override
    public Vec3 shortestDelta(Vec3 from, Vec3 to) {
        return this.fold.foldDelta(from, to);
    }

    @Override
    public boolean decomposesPerAxis() {
        return this.fold.decomposesPerAxis();
    }

    @Override
    public boolean preservesLocalIndices() {
        return this.fold.preservesLocalIndices();
    }

    @Override
    public Oriented<BlockPos> foldOriented(BlockPos pos) {
        return oriented(this.fold.foldOriented(pos));
    }

    @Override
    public Oriented<Vec3> foldOriented(Vec3 pos) {
        return oriented(this.fold.foldOriented(pos));
    }

    @Override
    public Oriented<ChunkPos> foldOriented(ChunkPos pos) {
        return oriented(this.fold.foldOriented(pos));
    }

    @Override
    public Oriented<Vec3> nearestCopyOriented(Vec3 ref, Vec3 target) {
        return oriented(this.fold.nearestCopyOriented(ref, target));
    }

    @Override
    public Oriented<BlockPos> nearestCopyOriented(BlockPos ref, BlockPos target) {
        return oriented(this.fold.nearestCopyOriented(ref, target));
    }

    private static <T> Oriented<T> oriented(WorldFold.Folded<T> folded) {
        FoldOrientation orientation = folded.orientation();
        return new Oriented<>(folded.value(), new Orientation(orientation.flipsX(), orientation.flipsZ()));
    }

    private AxisBounds boundsOf(Direction.Axis axis) {
        return switch (axis) {
            case X -> this.fold.bounds().x();
            case Z -> this.fold.bounds().z();
            case Y -> AxisBounds.Unbounded.INSTANCE;
        };
    }

    private AxisBounds.Looped looped(Direction.Axis axis) {
        if (boundsOf(axis) instanceof AxisBounds.Looped loopedBounds) {
            return loopedBounds;
        }

        throw new IllegalArgumentException("Axis " + axis + " does not loop — check loops(axis) first");
    }
}
