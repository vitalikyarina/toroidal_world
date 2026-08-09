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

// The looped shape of one dimension: four wrap domains — block and chunk units per horizontal axis — behind the typed
// operations the rest of the mod asks of them, grouped by the kind of value they act on.
//
// Every operation below that builds a position returns the one it was handed whenever that one already names the ground
// it means, the way wrapBlockNode does. Nearly every call is such a call — a world's coordinates are overwhelmingly
// inside it — and what those calls used to build was a fresh object carrying the very same numbers.
//
// It follows that a result kept beyond the call is the caller's own object: a mutable position handed in comes back
// mutable. That is exactly what vanilla would have returned to the same caller, and it is what every call site in the
// mod feeds these — a position read off a record, a command argument, or a variable vanilla itself owns.
public class WorldLoopTransformer {
    public static final WorldLoopTransformer NOOP = new WorldLoopTransformer(WorldLoopBounds.UNBOUNDED);

    public final CoordOps coords;
    public final ChunkOps chunks;
    public final VectorOps vectors;
    public final BlockOps blocks;

    // Chunk widths of the world, 0 on an unbounded axis — a world that does not close has no width there. Every
    // consumer of these stands on a wrapped path whose axes really loop; a question that must hold on any axis goes
    // through the domain's semantic methods instead.
    public final int xWidth;
    public final int zWidth;

    public final WorldLoopBounds bounds;

    // Read on every wrapped-path gate in the mod, so it is a plain precomputed flag rather than a settings comparison
    // per call. True when at least one axis really wraps — a loop whose every operation is the identity says no.
    private final boolean wrapped;

    // The loaded square must never reach its own far side across the seam, so a looped axis caps the view distance at
    // half its width minus a margin; an unbounded axis has no far side and imposes no ceiling of its own.
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

    // Floored at 1 chunk: on a looped axis under 8 chunks (128 blocks) the subtraction goes to zero or below, and
    // limitViewDistance would clamp every view distance non-positive — a world that renders nothing. Only hand-edited
    // save data gets that narrow (the settings screen's floor is 16 chunks); such a world shows its far side, but it
    // shows something.
    private static int viewDistanceCeiling(AxisBounds axis) {
        return switch (axis) {
            case AxisBounds.Looped looped ->
                    Math.max(1, looped.chunkWidth() / 2 - CoordinateConstants.VIEW_DISTANCE_MARGIN);
            case AxisBounds.Unbounded() -> Integer.MAX_VALUE;
        };
    }

    // Block-unit domains for loose double coordinates that are not yet a Vec3 or a BlockPos.
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

        // Sections stand on the chunk grid horizontally, so the chunk domains answer for them; Y stacks vertically,
        // has no seam, and passes through untouched.
        public SectionPos wrapSection(SectionPos sectionPos) {
            if (!x.isOver(sectionPos.x()) && !z.isOver(sectionPos.z())) {
                return sectionPos;
            }

            return SectionPos.of(x.wrap(sectionPos.x()), sectionPos.y(), z.wrap(sectionPos.z()));
        }

        // wrapSection asked of a packed section long, allocating nothing — the section trackers and the entity filing
        // callback walk their keys as bare longs.
        public long wrapSectionNode(long sectionNode) {
            int sectionX = SectionPos.x(sectionNode);
            int sectionZ = SectionPos.z(sectionNode);
            if (!x.isOver(sectionX) && !z.isOver(sectionZ)) {
                return sectionNode;
            }

            return SectionPos.asLong(x.wrap(sectionX), SectionPos.y(sectionNode), z.wrap(sectionZ));
        }

        // wrap asked of a packed ChunkPos long, allocating nothing — the ticket and entity managers key their maps by
        // bare chunk longs.
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

        // How far apart two chunks really are, measured through the seam. Vanilla asks this wherever it decides whether
        // one chunk is near enough to another to be meaningful, and it asks it of the canonical coordinates — which for
        // a pair straddling the bounds are a whole world apart, so a perfectly good neighbour reads as nonsense.
        //
        // Wrapped before unwrapping because unwrapping shifts by at most one world width, and a coordinate arriving
        // from elsewhere may be several laps out.
        public int chessboardDistance(ChunkPos fromChunkPos, ChunkPos toChunkPos) {
            return fromChunkPos.getChessboardDistance(
                    x.unwrap(fromChunkPos.x, x.wrap(toChunkPos.x)),
                    z.unwrap(fromChunkPos.z, z.wrap(toChunkPos.z)));
        }

        // How far past the world a chunk lies, chessboard-wise — the same metric the generation pyramid measures its
        // radii in, so a chunk this far out is directly comparable to a chunk that far from a full one.
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

        // The representation of a point nearest a reference, each horizontal axis folded on its own; Y has no seam and
        // comes through untouched. Unlike blocks.unwrap and chunks.unwrap, which take a coordinate already inside the
        // world, the target here may lie any number of laps out — it is wrapped before it is unwrapped.
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

        // vectors.nearestCopy asked of a block, and held to the same two promises: the value it names, and the argument
        // itself back when that value is where the argument already was. Composing wrap with unwrap keeps only the
        // first — a target out of bounds whose nearest copy is itself would come back as a fresh position — so the
        // fold is taken per axis, as the vector twin takes it.
        public BlockPos nearestCopy(BlockPos ref, BlockPos target) {
            int nearestX = coords.x.unwrapAround(ref.getX(), target.getX());
            int nearestZ = coords.z.unwrapAround(ref.getZ(), target.getZ());
            if (nearestX == target.getX() && nearestZ == target.getZ()) {
                return target;
            }

            return new BlockPos(nearestX, target.getY(), nearestZ);
        }
    }

    // A box reaching past the bounds covers ground on the other side of the world, but no vanilla query knows that: it
    // would search empty space and find nothing. The box is therefore cut into the pieces it actually covers — one per
    // axis it crosses, so one, two or four — each of them inside the world.
    //
    // Each axis is handled by wrapping the low edge into the world and then walking the box's own length from there: if
    // it runs off the top, what is left continues from the bottom. A box wider than the world simply covers all of it.
    // Whether a box reaches past the world at all — what splitAcrossBounds has to know before it can answer, asked on
    // its own so a caller can learn it without a list being built to carry the answer. Nearly every box put to an entity
    // query crosses nothing, and that case now costs two comparisons per axis and no allocation at all.
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

    // Wrap the horizontal component of a packed BlockPos long back into the world, allocating no BlockPos — the light
    // engine walks its graph in packed node longs on its own thread, where a fresh object per neighbour would be costly.
    public long wrapBlockNode(long blockNode) {
        int x = BlockPos.getX(blockNode);
        int z = BlockPos.getZ(blockNode);
        if (!coords.x.isOver(x) && !coords.z.isOver(z)) {
            return blockNode;
        }

        return BlockPos.asLong(coords.x.wrap(x), BlockPos.getY(blockNode), coords.z.wrap(z));
    }

    // Distance² from a point to a box, the seam counted in: each horizontal gap is measured to the box copy nearest the
    // point. Folding the flat gap instead would read through the seam toward the box's far edge and overstate the
    // distance by the box's own extent. Y has no seam and keeps the plain clamp.
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

    // Move a box to the copy of itself nearest a reference point, each horizontal axis folded independently. A target a
    // whole world away across the seam is laid back down beside the reference, so a plain range check on it measures the
    // short distance; a box already on this side is returned unchanged, keeping ordinary reach byte-for-byte.
    public AABB foldBoxToward(Vec3 ref, AABB box) {
        double centerX = (box.minX + box.maxX) / 2.0;
        double centerZ = (box.minZ + box.maxZ) / 2.0;
        double shiftX = coords.x.unwrapAround(ref.x, centerX) - centerX;
        double shiftZ = coords.z.unwrapAround(ref.z, centerZ) - centerZ;
        return shiftX == 0.0 && shiftZ == 0.0 ? box : box.move(shiftX, 0.0, shiftZ);
    }

    // A position carried in from another world: each horizontal axis stretched by the ratio the two worlds' widths set
    // on it, then folded into these bounds. Height crosses as it is — no dimension holds a different amount of it than
    // its neighbour.
    //
    // Where an axis closes in both worlds their widths are the mapping, whatever coordinate scale the dimensions
    // themselves declare; where either does not close there is no width to read a ratio from, and what the dimensions
    // declare is the only mapping there is — which is the one vanilla would have applied to the whole position.
    public Vec3 mapFrom(WorldLoopTransformer source, Vec3 position, double declaredScale) {
        double mappedX = coords.x.mapFrom(source.coords.x, position.x, declaredScale);
        double mappedZ = coords.z.mapFrom(source.coords.z, position.z, declaredScale);
        return mappedX == position.x && mappedZ == position.z ? position : new Vec3(mappedX, position.y, mappedZ);
    }

    // A region named by two corners is ambiguous the moment either horizontal axis reads shorter through the seam: the
    // same pair then bounds two different regions, and the corners carry nothing that says which one was meant. An axis
    // that does not wrap answers no outright — it has no seam for a region to span.
    public boolean spansSeam(BoundingBox region) {
        return coords.x.spansSeam(region.minX(), region.maxX()) || coords.z.spansSeam(region.minZ(), region.maxZ());
    }

    // The region a pair of corners was more likely to mean: the short way round the seam. For the readers that must
    // answer rather than refuse — a datapack condition cannot be told "ask again", a chunk reservation harms nobody —
    // this picks the shorter of the two readings and leaves an unambiguous one untouched.
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

    // Whether two regions cover a block in common, the seam counted in. Comparing the raw corners is the whole truth on
    // a flat world and blind on a torus: a region that runs past the bounds lies physically against the far edge of the
    // world, beside anything sitting there, while the two sets of numbers read a world apart. Nothing that overlapped
    // before stops overlapping now — a block shared in raw coordinates is still one block after both are folded — so
    // this only finds the overlaps the raw comparison could not see. Y is compared as it comes: it has no seam.
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
