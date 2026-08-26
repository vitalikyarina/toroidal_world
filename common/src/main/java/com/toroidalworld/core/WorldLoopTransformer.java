package com.toroidalworld.core;

import java.util.ArrayList;
import java.util.List;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class WorldLoopTransformer implements WorldFold {
    private final CoordOps coords;
    private final ChunkOps chunks;
    private final VectorOps vectors;
    private final BlockOps blocks;

    private final WorldLoopBounds bounds;

    private final boolean wrapped;

    private final int maxViewDistance;

    WorldLoopTransformer(WorldLoopBounds bounds) {
        this.bounds = bounds;
        this.wrapped = bounds.x() instanceof AxisBounds.Looped || bounds.z() instanceof AxisBounds.Looped;
        this.maxViewDistance = bounds.maxViewDistance();

        this.coords = new CoordOps(blockDomainFor(bounds.x()), blockDomainFor(bounds.z()));
        this.chunks = new ChunkOps(chunkDomainFor(bounds.x()), chunkDomainFor(bounds.z()));
        this.vectors = new VectorOps();
        this.blocks = new BlockOps();
    }

    private static WrapDomain chunkDomainFor(AxisBounds axis) {
        return switch (axis) {
            case AxisBounds.Looped looped -> new WrapDomain(looped.minChunk(), looped.maxChunk());
            case AxisBounds.Unbounded() -> new WrapDomain.Noop();
        };
    }

    private static WrapDomain blockDomainFor(AxisBounds axis) {
        return switch (axis) {
            case AxisBounds.Looped looped -> new WrapDomain(looped.minBlock(), looped.maxBlock());
            case AxisBounds.Unbounded() -> new WrapDomain.Noop();
        };
    }

    private static final class CoordOps {
        private final WrapDomain x;
        private final WrapDomain z;

        private CoordOps(WrapDomain x, WrapDomain z) {
            this.x = x;
            this.z = z;
        }
    }

    private static final class ChunkOps {
        private final WrapDomain x;
        private final WrapDomain z;

        private ChunkOps(WrapDomain x, WrapDomain z) {
            this.x = x;
            this.z = z;
        }

        private ChunkPos wrap(ChunkPos chunkPos) {
            if (!x.isOver(chunkPos.x()) && !z.isOver(chunkPos.z())) {
                return chunkPos;
            }

            return new ChunkPos(x.wrap(chunkPos.x()), z.wrap(chunkPos.z()));
        }

        private SectionPos wrapSection(SectionPos sectionPos) {
            if (!x.isOver(sectionPos.x()) && !z.isOver(sectionPos.z())) {
                return sectionPos;
            }

            return SectionPos.of(x.wrap(sectionPos.x()), sectionPos.y(), z.wrap(sectionPos.z()));
        }

        private long wrapSectionNode(long sectionNode) {
            int sectionX = SectionPos.x(sectionNode);
            int sectionZ = SectionPos.z(sectionNode);
            if (!x.isOver(sectionX) && !z.isOver(sectionZ)) {
                return sectionNode;
            }

            return SectionPos.asLong(x.wrap(sectionX), SectionPos.y(sectionNode), z.wrap(sectionZ));
        }

        private long wrapChunkKey(long chunkKey) {
            int chunkX = ChunkPos.getX(chunkKey);
            int chunkZ = ChunkPos.getZ(chunkKey);
            if (!x.isOver(chunkX) && !z.isOver(chunkZ)) {
                return chunkKey;
            }

            return ChunkPos.pack(x.wrap(chunkX), z.wrap(chunkZ));
        }

        private ChunkPos unwrap(ChunkPos anchor, ChunkPos wrapped) {
            int unwrappedX = x.unwrap(anchor.x(), wrapped.x());
            int unwrappedZ = z.unwrap(anchor.z(), wrapped.z());
            if (unwrappedX == wrapped.x() && unwrappedZ == wrapped.z()) {
                return wrapped;
            }

            return new ChunkPos(unwrappedX, unwrappedZ);
        }

        private boolean isOver(ChunkPos chunkPos) {
            return x.isOver(chunkPos.x()) || z.isOver(chunkPos.z());
        }

        private int overshoot(int chunkX, int chunkZ) {
            return Math.max(x.overshoot(chunkX), z.overshoot(chunkZ));
        }
    }

    private final class VectorOps {
        private Vec3 wrap(Vec3 vec) {
            if (!coords.x.isOver(vec.x) && !coords.z.isOver(vec.z)) {
                return vec;
            }

            return new Vec3(coords.x.wrap(vec.x), vec.y, coords.z.wrap(vec.z));
        }

        private Vec3 nearestCopy(Vec3 ref, Vec3 target) {
            double nearestX = coords.x.unwrapAround(ref.x, target.x);
            double nearestZ = coords.z.unwrapAround(ref.z, target.z);
            if (nearestX == target.x && nearestZ == target.z) {
                return target;
            }

            return new Vec3(nearestX, target.y, nearestZ);
        }

        private boolean isOver(Vec3 vec) {
            return coords.x.isOver(vec.x) || coords.z.isOver(vec.z);
        }
    }

    private final class BlockOps {
        private BlockPos wrap(BlockPos blockPos) {
            if (!coords.x.isOver(blockPos.getX()) && !coords.z.isOver(blockPos.getZ())) {
                return blockPos;
            }

            return new BlockPos(coords.x.wrap(blockPos.getX()), blockPos.getY(), coords.z.wrap(blockPos.getZ()));
        }

        private BlockPos nearestCopy(BlockPos ref, BlockPos target) {
            int nearestX = coords.x.unwrapAround(ref.getX(), target.getX());
            int nearestZ = coords.z.unwrapAround(ref.getZ(), target.getZ());
            if (nearestX == target.getX() && nearestZ == target.getZ()) {
                return target;
            }

            return new BlockPos(nearestX, target.getY(), nearestZ);
        }
    }

    @Override
    public boolean crossesBounds(AABB box) {
        return !coords.x.containsSpan(box.minX, box.maxX) || !coords.z.containsSpan(box.minZ, box.maxZ);
    }

    private List<AABB> splitAcrossBounds(AABB box) {
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

    @Override
    public boolean crossesBounds(BoundingBox region) {
        return coords.x.isOver(region.minX()) || coords.x.isOver(region.maxX())
                || coords.z.isOver(region.minZ()) || coords.z.isOver(region.maxZ());
    }

    private List<BoundingBox> splitAcrossBounds(BoundingBox region) {
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

    private static double seamGap(WrapDomain domain, double min, double max, double coord) {
        double center = (min + max) / 2.0;
        double nearestCenter = domain.unwrapAround(coord, center);
        return Math.max(Math.abs(nearestCenter - coord) - (max - min) / 2.0, 0.0);
    }

    @Override
    public boolean regionsOverlap(BoundingBox first, BoundingBox second) {
        return coords.x.overlaps(first.minX(), first.maxX(), second.minX(), second.maxX())
                && coords.z.overlaps(first.minZ(), first.maxZ(), second.minZ(), second.maxZ())
                && first.minY() <= second.maxY() && second.minY() <= first.maxY();
    }

    @Override
    public int maxViewDistance() {
        return maxViewDistance;
    }

    @Override
    public boolean isOver(Vec3 pos) {
        return vectors.isOver(pos);
    }

    @Override
    public boolean isOver(BlockPos pos) {
        return coords.x.isOver(pos.getX()) || coords.z.isOver(pos.getZ());
    }

    @Override
    public boolean isOver(ChunkPos pos) {
        return chunks.isOver(pos);
    }

    @Override
    public int chunkOvershoot(ChunkPos pos) {
        return chunks.overshoot(pos.x(), pos.z());
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

    @Override
    public boolean isWrapped() {
        return this.wrapped;
    }

    @Override
    public WorldLoopBounds bounds() {
        return this.bounds;
    }

    @Override
    public boolean decomposesPerAxis() {
        return true;
    }

    @Override
    public boolean preservesLocalIndices() {
        return true;
    }

    @Override
    public WrapDomain blockDomain(Direction.Axis axis) {
        return switch (axis) {
            case X -> coords.x;
            case Z -> coords.z;
            case Y -> throw new IllegalArgumentException("The fold contract carries no Y axis");
        };
    }

    @Override
    public WrapDomain chunkDomain(Direction.Axis axis) {
        return switch (axis) {
            case X -> chunks.x;
            case Z -> chunks.z;
            case Y -> throw new IllegalArgumentException("The fold contract carries no Y axis");
        };
    }

    @Override
    public Vec3 fold(Vec3 pos) {
        return vectors.wrap(pos);
    }

    @Override
    public BlockPos fold(BlockPos pos) {
        return blocks.wrap(pos);
    }

    @Override
    public ChunkPos fold(ChunkPos pos) {
        return chunks.wrap(pos);
    }

    @Override
    public SectionPos fold(SectionPos pos) {
        return chunks.wrapSection(pos);
    }

    @Override
    public long foldBlockNode(long blockNode) {
        int x = BlockPos.getX(blockNode);
        int z = BlockPos.getZ(blockNode);
        if (!coords.x.isOver(x) && !coords.z.isOver(z)) {
            return blockNode;
        }

        return BlockPos.asLong(coords.x.wrap(x), BlockPos.getY(blockNode), coords.z.wrap(z));
    }

    @Override
    public long foldChunkKey(long chunkKey) {
        return chunks.wrapChunkKey(chunkKey);
    }

    @Override
    public long foldSectionNode(long sectionNode) {
        return chunks.wrapSectionNode(sectionNode);
    }

    @Override
    public Folded<Vec3> foldOriented(Vec3 pos) {
        return Folded.of(vectors.wrap(pos));
    }

    @Override
    public Folded<BlockPos> foldOriented(BlockPos pos) {
        return Folded.of(blocks.wrap(pos));
    }

    @Override
    public Folded<ChunkPos> foldOriented(ChunkPos pos) {
        return Folded.of(chunks.wrap(pos));
    }

    @Override
    public Vec3 nearestCopy(Vec3 ref, Vec3 target) {
        return vectors.nearestCopy(ref, target);
    }

    @Override
    public BlockPos nearestCopy(BlockPos ref, BlockPos target) {
        return blocks.nearestCopy(ref, target);
    }

    @Override
    public ChunkPos nearestCopy(ChunkPos ref, ChunkPos target) {
        return chunks.unwrap(ref, target);
    }

    @Override
    public Folded<Vec3> nearestCopyOriented(Vec3 ref, Vec3 target) {
        return Folded.of(vectors.nearestCopy(ref, target));
    }

    @Override
    public Folded<BlockPos> nearestCopyOriented(BlockPos ref, BlockPos target) {
        return Folded.of(blocks.nearestCopy(ref, target));
    }

    @Override
    public Vec3 foldDelta(Vec3 from, Vec3 to) {
        return vectors.nearestCopy(from, to).subtract(from);
    }

    @Override
    public double sqrDistance(Vec3 from, Vec3 to) {
        return sqrDistance(from.x, from.y, from.z, to.x, to.y, to.z);
    }

    @Override
    public double sqrDistance(double xFrom, double yFrom, double zFrom, double xTo, double yTo, double zTo) {
        double dx = coords.x.unwrapAround(xFrom, xTo) - xFrom;
        double dy = yTo - yFrom;
        double dz = coords.z.unwrapAround(zFrom, zTo) - zFrom;
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public int sqrChunkDistance(ChunkPos from, ChunkPos to) {
        int dx = chunks.x.unwrapAround(from.x(), to.x()) - from.x();
        int dz = chunks.z.unwrapAround(from.z(), to.z()) - from.z();
        return dx * dx + dz * dz;
    }

    @Override
    public double sqrDistanceToBox(AABB box, Vec3 point) {
        double xGap = seamGap(coords.x, box.minX, box.maxX, point.x);
        double yGap = Math.max(Math.max(box.minY - point.y, point.y - box.maxY), 0.0);
        double zGap = seamGap(coords.z, box.minZ, box.maxZ, point.z);
        return xGap * xGap + yGap * yGap + zGap * zGap;
    }

    @Override
    public List<Folded<AABB>> split(AABB box) {
        List<AABB> pieces = splitAcrossBounds(box);
        List<Folded<AABB>> oriented = new ArrayList<>(pieces.size());
        for (AABB piece : pieces) {
            oriented.add(Folded.of(piece));
        }

        return oriented;
    }

    @Override
    public List<Folded<BoundingBox>> split(BoundingBox region) {
        List<BoundingBox> pieces = splitAcrossBounds(region);
        List<Folded<BoundingBox>> oriented = new ArrayList<>(pieces.size());
        for (BoundingBox piece : pieces) {
            oriented.add(Folded.of(piece));
        }

        return oriented;
    }

    @Override
    public Folded<AABB> foldBox(Vec3 ref, AABB box) {
        double centerX = (box.minX + box.maxX) / 2.0;
        double centerZ = (box.minZ + box.maxZ) / 2.0;
        double shiftX = coords.x.unwrapAround(ref.x, centerX) - centerX;
        double shiftZ = coords.z.unwrapAround(ref.z, centerZ) - centerZ;
        return Folded.of(shiftX == 0.0 && shiftZ == 0.0 ? box : box.move(shiftX, 0.0, shiftZ));
    }
}
