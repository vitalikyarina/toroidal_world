package com.toroidalworld.api;

import com.toroidalworld.core.CoordinateConstants;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.core.WrapDomain;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

// The API view over the engine's transformer: every operation delegates to the wrap math the mod itself runs on, so
// the public answers can never drift from the engine's. Bounds are read from the persisted model rather than the
// domains — a Noop domain carries meaningless zeros where the contract promises an exception.
final class TransformerToroidalShape implements ToroidalShape {
    private final WorldLoopTransformer transformer;

    TransformerToroidalShape(WorldLoopTransformer transformer) {
        this.transformer = transformer;
    }

    @Override
    public boolean loops(Direction.Axis axis) {
        return switch (axis) {
            case X -> transformer.bounds.x() instanceof AxisBounds.Looped;
            case Z -> transformer.bounds.z() instanceof AxisBounds.Looped;
            case Y -> false;
        };
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
        return blockDomain(axis).wrap(coord);
    }

    @Override
    public int foldBlock(Direction.Axis axis, int coord) {
        return blockDomain(axis).wrap(coord);
    }

    @Override
    public int foldChunk(Direction.Axis axis, int chunk) {
        return chunkDomain(axis).wrap(chunk);
    }

    @Override
    public BlockPos fold(BlockPos pos) {
        return transformer.blocks.wrap(pos);
    }

    @Override
    public Vec3 fold(Vec3 pos) {
        return transformer.vectors.wrap(pos);
    }

    @Override
    public ChunkPos fold(ChunkPos pos) {
        return transformer.chunks.wrap(pos);
    }

    @Override
    public Vec3 nearestCopy(Vec3 ref, Vec3 target) {
        return transformer.vectors.nearestCopy(ref, target);
    }

    @Override
    public BlockPos nearestCopy(BlockPos ref, BlockPos target) {
        return transformer.blocks.nearestCopy(ref, target);
    }

    @Override
    public double nearestCoord(Direction.Axis axis, double ref, double coord) {
        return blockDomain(axis).unwrapAround(ref, coord);
    }

    @Override
    public Vec3 shortestDelta(Vec3 from, Vec3 to) {
        return nearestCopy(from, to).subtract(from);
    }

    private AxisBounds.Looped looped(Direction.Axis axis) {
        AxisBounds bounds = switch (axis) {
            case X -> transformer.bounds.x();
            case Z -> transformer.bounds.z();
            case Y -> AxisBounds.Unbounded.INSTANCE;
        };
        if (bounds instanceof AxisBounds.Looped loopedBounds) {
            return loopedBounds;
        }

        throw new IllegalArgumentException("Axis " + axis + " does not loop — check loops(axis) first");
    }

    private WrapDomain blockDomain(Direction.Axis axis) {
        return switch (axis) {
            case X -> transformer.coords.x;
            case Z -> transformer.coords.z;
            case Y -> Y_NOOP;
        };
    }

    private WrapDomain chunkDomain(Direction.Axis axis) {
        return switch (axis) {
            case X -> transformer.chunks.x;
            case Z -> transformer.chunks.z;
            case Y -> Y_NOOP;
        };
    }

    private static final WrapDomain Y_NOOP = new WrapDomain.Noop();
}
