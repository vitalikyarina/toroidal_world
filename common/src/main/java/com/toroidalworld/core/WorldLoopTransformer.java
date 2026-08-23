package com.toroidalworld.core;

import java.util.ArrayList;
import java.util.List;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class WorldLoopTransformer {
    public static final WorldLoopTransformer NOOP = new WorldLoopTransformer(WorldLoopBounds.UNBOUNDED);

    public final CoordOps coords;
    public final ChunkOps chunks;
    public final VectorOps vectors;
    public final BlockOps blocks;

    public final int xWidth;
    public final int zWidth;

    public final WorldLoopBounds bounds;

    private final boolean wrapped;

    private final int maxViewDistance;

    public WorldLoopTransformer(WorldLoopBounds bounds) {
        this.bounds = bounds;
        this.wrapped = bounds.x() instanceof AxisBounds.Looped || bounds.z() instanceof AxisBounds.Looped;

        WrapDomain xChunk = chunkDomain(bounds.x());
        WrapDomain zChunk = chunkDomain(bounds.z());

        this.xWidth = xChunk.domainLength;
        this.zWidth = zChunk.domainLength;
        this.maxViewDistance = Math.min(viewDistanceCeiling(bounds.x()), viewDistanceCeiling(bounds.z()));

        this.coords = new CoordOps(blockDomain(bounds.x()), blockDomain(bounds.z()));
        this.chunks = new ChunkOps(xChunk, zChunk);
        this.vectors = new VectorOps();
        this.blocks = new BlockOps();
    }

    private static WrapDomain chunkDomain(AxisBounds axis) {
        return switch (axis) {
            case AxisBounds.Looped looped -> new WrapDomain(looped.minChunk(), looped.maxChunk());
            case AxisBounds.Unbounded() -> new WrapDomain.Noop();
        };
    }

    private static WrapDomain blockDomain(AxisBounds axis) {
        return switch (axis) {
            case AxisBounds.Looped looped -> new WrapDomain(
                    looped.minChunk() * CoordinateConstants.CHUNK_WIDTH,
                    looped.maxChunk() * CoordinateConstants.CHUNK_WIDTH);
            case AxisBounds.Unbounded() -> new WrapDomain.Noop();
        };
    }

    private static int viewDistanceCeiling(AxisBounds axis) {
        return switch (axis) {
            case AxisBounds.Looped looped ->
                    Math.max(1, looped.chunkWidth() / 2 - CoordinateConstants.VIEW_DISTANCE_MARGIN);
            case AxisBounds.Unbounded() -> Integer.MAX_VALUE;
        };
    }

    public final class CoordOps {
        public final WrapDomain x;
        public final WrapDomain z;

        private CoordOps(WrapDomain x, WrapDomain z) {
            this.x = x;
            this.z = z;
        }

        public double sqrDistToBounds(double xFrom, double yFrom, double zFrom, double xTo, double yTo, double zTo) {
            double dy = yTo - yFrom;
            return x.sqrDistToBounds(xTo - xFrom) + dy * dy + z.sqrDistToBounds(zTo - zFrom);
        }
    }

    public final class ChunkOps {
        public final WrapDomain x;
        public final WrapDomain z;

        private ChunkOps(WrapDomain x, WrapDomain z) {
            this.x = x;
            this.z = z;
        }

        public ChunkPos wrap(ChunkPos chunkPos) {
            if (!x.isOver(chunkPos.x) && !z.isOver(chunkPos.z)) {
                return chunkPos;
            }

            return new ChunkPos(x.wrap(chunkPos.x), z.wrap(chunkPos.z));
        }

        public SectionPos wrapSection(SectionPos sectionPos) {
            if (!x.isOver(sectionPos.x()) && !z.isOver(sectionPos.z())) {
                return sectionPos;
            }

            return SectionPos.of(x.wrap(sectionPos.x()), sectionPos.y(), z.wrap(sectionPos.z()));
        }

        public long wrapSectionNode(long sectionNode) {
            int sectionX = SectionPos.x(sectionNode);
            int sectionZ = SectionPos.z(sectionNode);
            if (!x.isOver(sectionX) && !z.isOver(sectionZ)) {
                return sectionNode;
            }

            return SectionPos.asLong(x.wrap(sectionX), SectionPos.y(sectionNode), z.wrap(sectionZ));
        }

        public long wrapChunkKey(long chunkKey) {
            int chunkX = ChunkPos.getX(chunkKey);
            int chunkZ = ChunkPos.getZ(chunkKey);
            if (!x.isOver(chunkX) && !z.isOver(chunkZ)) {
                return chunkKey;
            }

            return ChunkPos.asLong(x.wrap(chunkX), z.wrap(chunkZ));
        }

        public ChunkPos unwrap(ChunkPos anchor, ChunkPos wrapped) {
            int unwrappedX = x.unwrap(anchor.x, wrapped.x);
            int unwrappedZ = z.unwrap(anchor.z, wrapped.z);
            if (unwrappedX == wrapped.x && unwrappedZ == wrapped.z) {
                return wrapped;
            }

            return new ChunkPos(unwrappedX, unwrappedZ);
        }

        public boolean isOver(ChunkPos chunkPos) {
            return x.isOver(chunkPos.x) || z.isOver(chunkPos.z);
        }

        public int chessboardDistance(ChunkPos fromChunkPos, ChunkPos toChunkPos) {
            return fromChunkPos.getChessboardDistance(
                    x.unwrap(fromChunkPos.x, toChunkPos.x),
                    z.unwrap(fromChunkPos.z, toChunkPos.z));
        }

        public int overshoot(int chunkX, int chunkZ) {
            return Math.max(x.overshoot(chunkX), z.overshoot(chunkZ));
        }

        public int sqrDistToBounds(int xFrom, int zFrom, int xTo, int zTo) {
            return x.sqrDistToBounds(xTo - xFrom) + z.sqrDistToBounds(zTo - zFrom);
        }

        public int sqrDistToBounds(long from, long to) {
            return sqrDistToBounds(ChunkPos.getX(from), ChunkPos.getZ(from), ChunkPos.getX(to), ChunkPos.getZ(to));
        }

        public int sqrDistToBounds(ChunkPos from, ChunkPos to) {
            return sqrDistToBounds(from.x, from.z, to.x, to.z);
        }
    }

    public final class VectorOps {
        public Vec3 wrap(Vec3 vec) {
            if (!coords.x.isOver(vec.x) && !coords.z.isOver(vec.z)) {
                return vec;
            }

            return new Vec3(coords.x.wrap(vec.x), vec.y, coords.z.wrap(vec.z));
        }

        public Vec3 nearestCopy(Vec3 ref, Vec3 target) {
            double nearestX = coords.x.unwrapAround(ref.x, target.x);
            double nearestZ = coords.z.unwrapAround(ref.z, target.z);
            if (nearestX == target.x && nearestZ == target.z) {
                return target;
            }

            return new Vec3(nearestX, target.y, nearestZ);
        }

        public boolean isOver(Vec3 vec) {
            return coords.x.isOver(vec.x) || coords.z.isOver(vec.z);
        }
    }

    public final class BlockOps {
        public BlockPos wrap(BlockPos blockPos) {
            if (!coords.x.isOver(blockPos.getX()) && !coords.z.isOver(blockPos.getZ())) {
                return blockPos;
            }

            return new BlockPos(coords.x.wrap(blockPos.getX()), blockPos.getY(), coords.z.wrap(blockPos.getZ()));
        }

        public BlockPos unwrap(BlockPos anchor, BlockPos wrapped) {
            int unwrappedX = coords.x.unwrap(anchor.getX(), wrapped.getX());
            int unwrappedZ = coords.z.unwrap(anchor.getZ(), wrapped.getZ());
            if (unwrappedX == wrapped.getX() && unwrappedZ == wrapped.getZ()) {
                return wrapped;
            }

            return new BlockPos(unwrappedX, wrapped.getY(), unwrappedZ);
        }

        public BlockPos nearestCopy(BlockPos ref, BlockPos target) {
            int nearestX = coords.x.unwrapAround(ref.getX(), target.getX());
            int nearestZ = coords.z.unwrapAround(ref.getZ(), target.getZ());
            if (nearestX == target.getX() && nearestZ == target.getZ()) {
                return target;
            }

            return new BlockPos(nearestX, target.getY(), nearestZ);
        }
    }

    public boolean crossesBounds(AABB box) {
        return !coords.x.containsSpan(box.minX, box.maxX) || !coords.z.containsSpan(box.minZ, box.maxZ);
    }

    public List<AABB> splitAcrossBounds(AABB box) {
        if (!crossesBounds(box)) {
            return List.of(box);
        }

        List<double[]> xSpans = coords.x.spans(box.minX, box.maxX);
        List<double[]> zSpans = coords.z.spans(box.minZ, box.maxZ);
        List<AABB> pieces = new ArrayList<>(xSpans.size() * zSpans.size());
        for (double[] xSpan : xSpans) {
            for (double[] zSpan : zSpans) {
                pieces.add(new AABB(xSpan[0], box.minY, zSpan[0], xSpan[1], box.maxY, zSpan[1]));
            }
        }

        return pieces;
    }

    public boolean crossesBounds(BoundingBox region) {
        return coords.x.isOver(region.minX()) || coords.x.isOver(region.maxX())
                || coords.z.isOver(region.minZ()) || coords.z.isOver(region.maxZ());
    }

    public List<BoundingBox> splitAcrossBounds(BoundingBox region) {
        if (!crossesBounds(region)) {
            return List.of(region);
        }

        List<int[]> xSpans = coords.x.cellSpans(region.minX(), region.maxX());
        List<int[]> zSpans = coords.z.cellSpans(region.minZ(), region.maxZ());
        List<BoundingBox> pieces = new ArrayList<>(xSpans.size() * zSpans.size());
        for (int[] xSpan : xSpans) {
            for (int[] zSpan : zSpans) {
                pieces.add(new BoundingBox(xSpan[0], region.minY(), zSpan[0], xSpan[1], region.maxY(), zSpan[1]));
            }
        }

        return pieces;
    }

    public long wrapBlockNode(long blockNode) {
        int x = BlockPos.getX(blockNode);
        int z = BlockPos.getZ(blockNode);
        if (!coords.x.isOver(x) && !coords.z.isOver(z)) {
            return blockNode;
        }

        return BlockPos.asLong(coords.x.wrap(x), BlockPos.getY(blockNode), coords.z.wrap(z));
    }

    public double distanceToSqrWrappedCoord(AABB aabb, Vec3 vec) {
        double xGap = seamGap(coords.x, aabb.minX, aabb.maxX, vec.x);
        double yGap = Math.max(Math.max(aabb.minY - vec.y, vec.y - aabb.maxY), 0.0);
        double zGap = seamGap(coords.z, aabb.minZ, aabb.maxZ, vec.z);
        return xGap * xGap + yGap * yGap + zGap * zGap;
    }

    private static double seamGap(WrapDomain domain, double min, double max, double coord) {
        double center = (min + max) / 2.0;
        double nearestCenter = domain.unwrapAround(coord, center);
        return Math.max(Math.abs(nearestCenter - coord) - (max - min) / 2.0, 0.0);
    }

    public AABB foldBoxToward(Vec3 ref, AABB box) {
        double centerX = (box.minX + box.maxX) / 2.0;
        double centerZ = (box.minZ + box.maxZ) / 2.0;
        double shiftX = coords.x.unwrapAround(ref.x, centerX) - centerX;
        double shiftZ = coords.z.unwrapAround(ref.z, centerZ) - centerZ;
        return shiftX == 0.0 && shiftZ == 0.0 ? box : box.move(shiftX, 0.0, shiftZ);
    }

    public Vec3 mapFrom(WorldLoopTransformer source, Vec3 position, double declaredScale) {
        double mappedX = coords.x.mapFrom(source.coords.x, position.x, declaredScale);
        double mappedZ = coords.z.mapFrom(source.coords.z, position.z, declaredScale);
        return mappedX == position.x && mappedZ == position.z ? position : new Vec3(mappedX, position.y, mappedZ);
    }

    public boolean spansSeam(BoundingBox region) {
        return coords.x.spansSeam(region.minX(), region.maxX()) || coords.z.spansSeam(region.minZ(), region.maxZ());
    }

    public boolean exceedsWorld(BoundingBox region) {
        return coords.x.exceedsWorld(region.minX(), region.maxX())
                || coords.z.exceedsWorld(region.minZ(), region.maxZ());
    }

    public BoundingBox foldAcrossSeam(BoundingBox region) {
        if (!spansSeam(region)) {
            return region;
        }

        return new BoundingBox(
                coords.x.foldSpanStart(region.minX(), region.maxX()),
                region.minY(),
                coords.z.foldSpanStart(region.minZ(), region.maxZ()),
                coords.x.foldSpanEnd(region.minX(), region.maxX()),
                region.maxY(),
                coords.z.foldSpanEnd(region.minZ(), region.maxZ()));
    }

    public boolean regionsOverlap(BoundingBox first, BoundingBox second) {
        return coords.x.overlaps(first.minX(), first.maxX(), second.minX(), second.maxX())
                && coords.z.overlaps(first.minZ(), first.maxZ(), second.minZ(), second.maxZ())
                && first.minY() <= second.maxY() && second.minY() <= first.maxY();
    }

    public int limitViewDistance(int viewDistance) {
        return Math.min(viewDistance, maxViewDistance);
    }

    public int maxViewDistance() {
        return maxViewDistance;
    }

    @Override
    public String toString() {
        return "WorldLoopTransformer[x " + axisString(bounds.x()) + ", z " + axisString(bounds.z()) + "]";
    }

    private static String axisString(AxisBounds axis) {
        return switch (axis) {
            case AxisBounds.Looped looped -> looped.minChunk() + ".." + looped.maxChunk() + " chunks";
            case AxisBounds.Unbounded() -> "unbounded";
        };
    }

    public boolean isWrapped() {
        return this.wrapped;
    }
}
