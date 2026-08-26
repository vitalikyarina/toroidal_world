package com.toroidalworld.core;

import java.util.ArrayList;
import java.util.List;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class DeckGroupFold implements WorldFold {
    private static final int CANDIDATE_REACH = 2;

    private static final int COORD_FOLD_ATTEMPTS = 3;

    private final FlatShape shape;

    private final Lattice blocks;

    private final Lattice chunks;

    private final boolean wrapped;

    public DeckGroupFold(FlatShape shape) {
        this.shape = shape;
        this.wrapped = shape.bounds().x() instanceof AxisBounds.Looped
                || shape.bounds().z() instanceof AxisBounds.Looped;
        this.chunks = new Lattice(shape, 1);
        this.blocks = new Lattice(shape, CoordinateConstants.CHUNK_WIDTH);
    }

    public FlatShape shape() {
        return this.shape;
    }

    @Override
    public WorldLoopBounds bounds() {
        return this.shape.bounds();
    }

    @Override
    public boolean isWrapped() {
        return this.wrapped;
    }

    @Override
    public boolean decomposesPerAxis() {
        return this.shape.decomposesPerAxis();
    }

    @Override
    public boolean preservesLocalIndices() {
        return this.shape.preservesLocalIndices();
    }

    @Override
    public WrapDomain blockDomain(Direction.Axis axis) {
        return perAxisDomain(this.blocks, axis);
    }

    @Override
    public WrapDomain chunkDomain(Direction.Axis axis) {
        return perAxisDomain(this.chunks, axis);
    }

    @Override
    public boolean isOver(Vec3 pos) {
        return this.blocks.x.isOver(pos.x) || this.blocks.z.isOver(pos.z);
    }

    @Override
    public boolean isOver(BlockPos pos) {
        return this.blocks.x.isOver(pos.getX()) || this.blocks.z.isOver(pos.getZ());
    }

    @Override
    public boolean isOver(ChunkPos pos) {
        return this.chunks.x.isOver(pos.x()) || this.chunks.z.isOver(pos.z());
    }

    @Override
    public int chunkOvershoot(ChunkPos pos) {
        return Math.max(this.chunks.x.overshoot(pos.x()), this.chunks.z.overshoot(pos.z()));
    }

    @Override
    public int maxViewDistance() {
        return this.shape.bounds().maxViewDistance();
    }

    private WrapDomain perAxisDomain(Lattice lattice, Direction.Axis axis) {
        if (!decomposesPerAxis()) {
            throw new IllegalStateException(this.shape.identification() + " does not decompose per axis");
        }

        return switch (axis) {
            case X -> lattice.x;
            case Z -> lattice.z;
            case Y -> throw new IllegalArgumentException("The fold contract carries no Y axis");
        };
    }

    @Override
    public Vec3 fold(Vec3 pos) {
        SeamTransform applied = this.blocks.foldCoords(pos.x, pos.z);
        return applied.isIdentity() ? pos : new Vec3(applied.applyX(pos.x), pos.y, applied.applyZ(pos.z));
    }

    @Override
    public BlockPos fold(BlockPos pos) {
        SeamTransform applied = this.blocks.foldCells(pos.getX(), pos.getZ());
        return applied.isIdentity()
                ? pos
                : new BlockPos(applied.applyCellX(pos.getX()), pos.getY(), applied.applyCellZ(pos.getZ()));
    }

    @Override
    public ChunkPos fold(ChunkPos pos) {
        SeamTransform applied = this.chunks.foldCells(pos.x(), pos.z());
        return applied.isIdentity() ? pos : new ChunkPos(applied.applyCellX(pos.x()), applied.applyCellZ(pos.z()));
    }

    @Override
    public SectionPos fold(SectionPos pos) {
        SeamTransform applied = this.chunks.foldCells(pos.x(), pos.z());
        return applied.isIdentity()
                ? pos
                : SectionPos.of(applied.applyCellX(pos.x()), pos.y(), applied.applyCellZ(pos.z()));
    }

    @Override
    public long foldBlockNode(long blockNode) {
        int x = BlockPos.getX(blockNode);
        int z = BlockPos.getZ(blockNode);
        SeamTransform applied = this.blocks.foldCells(x, z);
        return applied.isIdentity()
                ? blockNode
                : BlockPos.asLong(applied.applyCellX(x), BlockPos.getY(blockNode), applied.applyCellZ(z));
    }

    @Override
    public long foldChunkKey(long chunkKey) {
        int x = ChunkPos.getX(chunkKey);
        int z = ChunkPos.getZ(chunkKey);
        SeamTransform applied = this.chunks.foldCells(x, z);
        return applied.isIdentity() ? chunkKey : ChunkPos.pack(applied.applyCellX(x), applied.applyCellZ(z));
    }

    @Override
    public long foldSectionNode(long sectionNode) {
        int x = SectionPos.x(sectionNode);
        int z = SectionPos.z(sectionNode);
        SeamTransform applied = this.chunks.foldCells(x, z);
        return applied.isIdentity()
                ? sectionNode
                : SectionPos.asLong(applied.applyCellX(x), SectionPos.y(sectionNode), applied.applyCellZ(z));
    }

    @Override
    public Folded<Vec3> foldOriented(Vec3 pos) {
        SeamTransform applied = this.blocks.foldCoords(pos.x, pos.z);
        Vec3 value = applied.isIdentity() ? pos : new Vec3(applied.applyX(pos.x), pos.y, applied.applyZ(pos.z));
        return new Folded<>(value, applied.orientation());
    }

    @Override
    public Folded<BlockPos> foldOriented(BlockPos pos) {
        SeamTransform applied = this.blocks.foldCells(pos.getX(), pos.getZ());
        BlockPos value = applied.isIdentity()
                ? pos
                : new BlockPos(applied.applyCellX(pos.getX()), pos.getY(), applied.applyCellZ(pos.getZ()));
        return new Folded<>(value, applied.orientation());
    }

    @Override
    public Folded<ChunkPos> foldOriented(ChunkPos pos) {
        SeamTransform applied = this.chunks.foldCells(pos.x(), pos.z());
        ChunkPos value = applied.isIdentity()
                ? pos
                : new ChunkPos(applied.applyCellX(pos.x()), applied.applyCellZ(pos.z()));
        return new Folded<>(value, applied.orientation());
    }

    @Override
    public Vec3 nearestCopy(Vec3 ref, Vec3 target) {
        SeamTransform move = this.blocks.nearestCoords(ref.x, ref.z, target.x, target.z);
        return move.isIdentity() ? target : new Vec3(move.applyX(target.x), target.y, move.applyZ(target.z));
    }

    @Override
    public BlockPos nearestCopy(BlockPos ref, BlockPos target) {
        SeamTransform move = this.blocks.nearestCells(ref.getX(), ref.getZ(), target.getX(), target.getZ());
        return move.isIdentity()
                ? target
                : new BlockPos(move.applyCellX(target.getX()), target.getY(), move.applyCellZ(target.getZ()));
    }

    @Override
    public ChunkPos nearestCopy(ChunkPos ref, ChunkPos target) {
        SeamTransform move = this.chunks.nearestCells(ref.x(), ref.z(), target.x(), target.z());
        return move.isIdentity() ? target : new ChunkPos(move.applyCellX(target.x()), move.applyCellZ(target.z()));
    }

    @Override
    public Folded<Vec3> nearestCopyOriented(Vec3 ref, Vec3 target) {
        SeamTransform move = this.blocks.nearestCoords(ref.x, ref.z, target.x, target.z);
        Vec3 value = move.isIdentity() ? target : new Vec3(move.applyX(target.x), target.y, move.applyZ(target.z));
        return new Folded<>(value, move.orientation());
    }

    @Override
    public Folded<BlockPos> nearestCopyOriented(BlockPos ref, BlockPos target) {
        SeamTransform move = this.blocks.nearestCells(ref.getX(), ref.getZ(), target.getX(), target.getZ());
        BlockPos value = move.isIdentity()
                ? target
                : new BlockPos(move.applyCellX(target.getX()), target.getY(), move.applyCellZ(target.getZ()));
        return new Folded<>(value, move.orientation());
    }

    @Override
    public DeckTransformation deckTransformation(ChunkPos chunk, ChunkPos copy) {
        if (chunk.equals(copy)) {
            return DeckTransformation.IDENTITY;
        }

        DeckTransformation carried = new DeckTransformation(this.blocks.between(
                chunk.getMinBlockX(), chunk.getMinBlockZ(), copy.getMinBlockX(), copy.getMinBlockZ()));
        if (!carried.apply(chunk).equals(copy)) {
            throw new IllegalArgumentException(copy + " is not a copy of " + chunk + " in " + this.shape);
        }

        return carried;
    }

    @Override
    public BlockPos reseat(BlockPos pos, ChunkPos copy) {
        return deckTransformation(ChunkPos.containing(pos), copy).apply(pos);
    }

    @Override
    public Vec3 foldDelta(Vec3 from, Vec3 to) {
        return nearestCopy(from, to).subtract(from);
    }

    @Override
    public double sqrDistance(Vec3 from, Vec3 to) {
        return sqrDistance(from.x, from.y, from.z, to.x, to.y, to.z);
    }

    @Override
    public double sqrDistance(double xFrom, double yFrom, double zFrom, double xTo, double yTo, double zTo) {
        SeamTransform move = this.blocks.nearestCoords(xFrom, zFrom, xTo, zTo);
        double dx = move.applyX(xTo) - xFrom;
        double dy = yTo - yFrom;
        double dz = move.applyZ(zTo) - zFrom;
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public int sqrChunkDistance(ChunkPos from, ChunkPos to) {
        SeamTransform move = this.chunks.nearestCells(from.x(), from.z(), to.x(), to.z());
        int dx = move.applyCellX(to.x()) - from.x();
        int dz = move.applyCellZ(to.z()) - from.z();
        return dx * dx + dz * dz;
    }

    @Override
    public double sqrDistanceToBox(AABB box, Vec3 point) {
        AABB folded = foldBox(point, box).value();
        double xGap = Math.max(Math.max(folded.minX - point.x, point.x - folded.maxX), 0.0);
        double yGap = Math.max(Math.max(folded.minY - point.y, point.y - folded.maxY), 0.0);
        double zGap = Math.max(Math.max(folded.minZ - point.z, point.z - folded.maxZ), 0.0);
        return xGap * xGap + yGap * yGap + zGap * zGap;
    }

    @Override
    public boolean crossesBounds(AABB box) {
        return !this.blocks.x.containsSpan(box.minX, box.maxX) || !this.blocks.z.containsSpan(box.minZ, box.maxZ);
    }

    @Override
    public boolean crossesBounds(BoundingBox region) {
        return this.blocks.x.isOver(region.minX()) || this.blocks.x.isOver(region.maxX())
                || this.blocks.z.isOver(region.minZ()) || this.blocks.z.isOver(region.maxZ());
    }

    @Override
    public List<Folded<AABB>> split(AABB box) {
        if (!crossesBounds(box)) {
            return List.of(Folded.of(box));
        }

        List<CoordPiece> reduced = this.blocks.splitCoords(box.minX, box.maxX, box.minZ, box.maxZ);
        List<Folded<AABB>> pieces = new ArrayList<>(reduced.size());
        for (CoordPiece piece : reduced) {
            pieces.add(new Folded<>(
                    new AABB(piece.minX(), box.minY, piece.minZ(), piece.maxX(), box.maxY, piece.maxZ()),
                    piece.applied().orientation()));
        }

        return pieces;
    }

    @Override
    public List<Folded<BoundingBox>> split(BoundingBox region) {
        if (!crossesBounds(region)) {
            return List.of(Folded.of(region));
        }

        List<CellPiece> reduced =
                this.blocks.splitCells(region.minX(), region.maxX(), region.minZ(), region.maxZ());
        List<Folded<BoundingBox>> pieces = new ArrayList<>(reduced.size());
        for (CellPiece piece : reduced) {
            pieces.add(new Folded<>(
                    new BoundingBox(piece.minX(), region.minY(), piece.minZ(),
                            piece.maxX(), region.maxY(), piece.maxZ()),
                    piece.applied().orientation()));
        }

        return pieces;
    }

    @Override
    public boolean regionsOverlap(BoundingBox first, BoundingBox second) {
        if (first.minY() > second.maxY() || second.minY() > first.maxY()) {
            return false;
        }

        for (Folded<BoundingBox> firstPiece : split(first)) {
            for (Folded<BoundingBox> secondPiece : split(second)) {
                if (horizontallyIntersects(firstPiece.value(), secondPiece.value())) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public Folded<AABB> foldBox(Vec3 ref, AABB box) {
        double centerX = (box.minX + box.maxX) / 2.0;
        double centerZ = (box.minZ + box.maxZ) / 2.0;
        SeamTransform move = this.blocks.nearestCoords(ref.x, ref.z, centerX, centerZ);
        if (move.isIdentity()) {
            return Folded.of(box);
        }

        double firstX = move.applyX(box.minX);
        double secondX = move.applyX(box.maxX);
        double firstZ = move.applyZ(box.minZ);
        double secondZ = move.applyZ(box.maxZ);
        return new Folded<>(new AABB(
                Math.min(firstX, secondX), box.minY, Math.min(firstZ, secondZ),
                Math.max(firstX, secondX), box.maxY, Math.max(firstZ, secondZ)),
                move.orientation());
    }

    private static boolean horizontallyIntersects(BoundingBox first, BoundingBox second) {
        return first.minX() <= second.maxX() && second.minX() <= first.maxX()
                && first.minZ() <= second.maxZ() && second.minZ() <= first.maxZ();
    }

    private record CoordPiece(double minX, double maxX, double minZ, double maxZ, SeamTransform applied) {}

    private record CellPiece(int minX, int maxX, int minZ, int maxZ, SeamTransform applied) {}

    private record Step(SeamTransform generator, boolean onX) {}

    private static final class Lattice {
        private final WrapDomain x;

        private final WrapDomain z;

        private final Step[] steps;

        private final SeamTransform[] candidates;

        private Lattice(FlatShape shape, int unit) {
            AxisBounds xBounds = shape.bounds().x();
            AxisBounds zBounds = shape.bounds().z();
            this.x = domainOf(xBounds, unit);
            this.z = domainOf(zBounds, unit);

            FlatShape.Mirror mirror = shape.mirror();
            boolean mirrorsZ = mirror != null && mirror.axis() == Direction.Axis.Z;
            boolean mirrorsX = mirror != null && mirror.axis() == Direction.Axis.X;
            int mirrorLine = mirror == null ? 0 : mirror.lineChunk() * unit;
            int skew = shape.skewChunks() * unit;

            Step xStep = xBounds instanceof AxisBounds.Looped
                    ? new Step(mirrorsZ
                            ? SeamTransform.glideX(this.x.domainLength, mirrorShift(mirrorLine, this.z))
                            : SeamTransform.translation(this.x.domainLength, 0), true)
                    : null;
            Step zStep = zBounds instanceof AxisBounds.Looped
                    ? new Step(mirrorsX
                            ? SeamTransform.glideZ(mirrorShift(mirrorLine, this.x), this.z.domainLength)
                            : SeamTransform.translation(skew, this.z.domainLength), false)
                    : null;

            this.steps = orderSteps(xStep, zStep, mirrorsZ);
            this.candidates = buildCandidates(this.steps);
        }

        private static int mirrorShift(int mirrorLine, WrapDomain mirrored) {
            int rawShift = 2 * mirrorLine;
            if (mirrored instanceof WrapDomain.Noop) {
                return rawShift;
            }

            int anchor = mirrored.lowerBound + mirrored.upperBound;
            int offset = Math.floorMod(rawShift - anchor, mirrored.domainLength);
            return anchor + (2 * offset > mirrored.domainLength ? offset - mirrored.domainLength : offset);
        }

        private static WrapDomain domainOf(AxisBounds axis, int unit) {
            return switch (axis) {
                case AxisBounds.Looped looped -> new WrapDomain(looped.minChunk() * unit, looped.maxChunk() * unit);
                case AxisBounds.Unbounded() -> new WrapDomain.Noop();
            };
        }

        private static Step[] orderSteps(Step xStep, Step zStep, boolean mirrorsZ) {
            if (xStep == null && zStep == null) {
                return new Step[0];
            }

            if (xStep == null) {
                return new Step[] {zStep};
            }

            if (zStep == null) {
                return new Step[] {xStep};
            }

            return mirrorsZ ? new Step[] {xStep, zStep} : new Step[] {zStep, xStep};
        }

        private static SeamTransform[] buildCandidates(Step[] steps) {
            if (steps.length == 0) {
                return new SeamTransform[] {SeamTransform.IDENTITY};
            }

            List<SeamTransform> built = new ArrayList<>();
            for (int first = -CANDIDATE_REACH; first <= CANDIDATE_REACH; first++) {
                SeamTransform firstPower = steps[0].generator().power(first);
                if (steps.length == 1) {
                    built.add(firstPower);
                    continue;
                }

                for (int second = -CANDIDATE_REACH; second <= CANDIDATE_REACH; second++) {
                    built.add(firstPower.then(steps[1].generator().power(second)));
                }
            }

            return built.toArray(new SeamTransform[0]);
        }

        private SeamTransform foldCells(int cellX, int cellZ) {
            SeamTransform applied = SeamTransform.IDENTITY;
            int currentX = cellX;
            int currentZ = cellZ;
            for (Step step : this.steps) {
                WrapDomain domain = step.onX() ? this.x : this.z;
                int coord = step.onX() ? currentX : currentZ;
                long laps = Math.floorDiv((long) coord - domain.lowerBound, domain.domainLength);
                if (laps == 0) {
                    continue;
                }

                SeamTransform move = step.generator().power(Math.toIntExact(-laps));
                int movedX = move.applyCellX(currentX);
                int movedZ = move.applyCellZ(currentZ);
                currentX = movedX;
                currentZ = movedZ;
                applied = applied.then(move);
            }

            return applied;
        }

        private SeamTransform foldCoords(double coordX, double coordZ) {
            SeamTransform applied = SeamTransform.IDENTITY;
            double currentX = coordX;
            double currentZ = coordZ;
            for (Step step : this.steps) {
                WrapDomain domain = step.onX() ? this.x : this.z;
                for (int attempt = 0; attempt < COORD_FOLD_ATTEMPTS; attempt++) {
                    double coord = step.onX() ? currentX : currentZ;
                    long laps = (long) Math.floor((coord - domain.lowerBound) / domain.domainLength);
                    if (laps == 0) {
                        break;
                    }

                    SeamTransform move = step.generator().power(Math.toIntExact(-laps));
                    double movedX = move.applyX(currentX);
                    double movedZ = move.applyZ(currentZ);
                    currentX = movedX;
                    currentZ = movedZ;
                    applied = applied.then(move);
                }
            }

            return applied;
        }

        private SeamTransform between(int fromX, int fromZ, int toX, int toZ) {
            return foldCells(fromX, fromZ).then(foldCells(toX, toZ).inverse());
        }

        private SeamTransform nearestCells(int refX, int refZ, int targetX, int targetZ) {
            SeamTransform toRef = foldCells(refX, refZ);
            SeamTransform toTarget = foldCells(targetX, targetZ);
            SeamTransform fromRef = toRef.inverse();
            int foldedTargetX = toTarget.applyCellX(targetX);
            int foldedTargetZ = toTarget.applyCellZ(targetZ);

            SeamTransform best = SeamTransform.IDENTITY;
            long bestDistance = squared((long) targetX - refX) + squared((long) targetZ - refZ);
            long bestShiftX = 0;
            long bestShiftZ = 0;
            for (SeamTransform candidate : this.candidates) {
                int inFrameX = candidate.applyCellX(foldedTargetX);
                int inFrameZ = candidate.applyCellZ(foldedTargetZ);
                long copyX = fromRef.applyCellX(inFrameX);
                long copyZ = fromRef.applyCellZ(inFrameZ);
                long distance = squared(copyX - refX) + squared(copyZ - refZ);
                long shiftX = copyX - targetX;
                long shiftZ = copyZ - targetZ;
                if (distance < bestDistance || (distance == bestDistance
                        && closerToRest(shiftX, shiftZ, bestShiftX, bestShiftZ))) {
                    best = toTarget.then(candidate).then(fromRef);
                    bestDistance = distance;
                    bestShiftX = shiftX;
                    bestShiftZ = shiftZ;
                }
            }

            return best;
        }

        private SeamTransform nearestCoords(double refX, double refZ, double targetX, double targetZ) {
            SeamTransform toRef = foldCoords(refX, refZ);
            SeamTransform toTarget = foldCoords(targetX, targetZ);
            SeamTransform fromRef = toRef.inverse();
            double foldedTargetX = toTarget.applyX(targetX);
            double foldedTargetZ = toTarget.applyZ(targetZ);

            SeamTransform best = SeamTransform.IDENTITY;
            double bestDistance = squared(targetX - refX) + squared(targetZ - refZ);
            double bestShiftX = 0.0;
            double bestShiftZ = 0.0;
            for (SeamTransform candidate : this.candidates) {
                double inFrameX = candidate.applyX(foldedTargetX);
                double inFrameZ = candidate.applyZ(foldedTargetZ);
                double copyX = fromRef.applyX(inFrameX);
                double copyZ = fromRef.applyZ(inFrameZ);
                double distance = squared(copyX - refX) + squared(copyZ - refZ);
                double shiftX = copyX - targetX;
                double shiftZ = copyZ - targetZ;
                if (distance < bestDistance || (distance == bestDistance
                        && closerToRest(shiftX, shiftZ, bestShiftX, bestShiftZ))) {
                    best = toTarget.then(candidate).then(fromRef);
                    bestDistance = distance;
                    bestShiftX = shiftX;
                    bestShiftZ = shiftZ;
                }
            }

            return best;
        }

        private List<CoordPiece> splitCoords(double minX, double maxX, double minZ, double maxZ) {
            List<CoordPiece> pieces = new ArrayList<>();
            pieces.add(new CoordPiece(minX, maxX, minZ, maxZ, SeamTransform.IDENTITY));
            for (Step step : this.steps) {
                WrapDomain domain = step.onX() ? this.x : this.z;
                List<CoordPiece> next = new ArrayList<>();
                for (CoordPiece piece : pieces) {
                    double low = step.onX() ? piece.minX() : piece.minZ();
                    double high = step.onX() ? piece.maxX() : piece.maxZ();
                    for (double[] slice : sliceSpan(domain, low, high)) {
                        long laps = (long) Math.floor((slice[0] - domain.lowerBound) / domain.domainLength);
                        SeamTransform move = step.generator().power(Math.toIntExact(-laps));
                        double lowX = step.onX() ? slice[0] : piece.minX();
                        double highX = step.onX() ? slice[1] : piece.maxX();
                        double lowZ = step.onX() ? piece.minZ() : slice[0];
                        double highZ = step.onX() ? piece.maxZ() : slice[1];
                        double firstX = move.applyX(lowX);
                        double secondX = move.applyX(highX);
                        double firstZ = move.applyZ(lowZ);
                        double secondZ = move.applyZ(highZ);
                        next.add(new CoordPiece(
                                Math.min(firstX, secondX), Math.max(firstX, secondX),
                                Math.min(firstZ, secondZ), Math.max(firstZ, secondZ),
                                piece.applied().then(move)));
                    }
                }

                pieces = next;
            }

            return pieces;
        }

        private List<CellPiece> splitCells(int minX, int maxX, int minZ, int maxZ) {
            List<CellPiece> pieces = new ArrayList<>();
            pieces.add(new CellPiece(minX, maxX, minZ, maxZ, SeamTransform.IDENTITY));
            for (Step step : this.steps) {
                WrapDomain domain = step.onX() ? this.x : this.z;
                List<CellPiece> next = new ArrayList<>();
                for (CellPiece piece : pieces) {
                    int low = step.onX() ? piece.minX() : piece.minZ();
                    int high = step.onX() ? piece.maxX() : piece.maxZ();
                    for (int[] slice : sliceCellSpan(domain, low, high)) {
                        long laps = Math.floorDiv((long) slice[0] - domain.lowerBound, domain.domainLength);
                        SeamTransform move = step.generator().power(Math.toIntExact(-laps));
                        int lowX = step.onX() ? slice[0] : piece.minX();
                        int highX = step.onX() ? slice[1] : piece.maxX();
                        int lowZ = step.onX() ? piece.minZ() : slice[0];
                        int highZ = step.onX() ? piece.maxZ() : slice[1];
                        int firstX = move.applyCellX(lowX);
                        int secondX = move.applyCellX(highX);
                        int firstZ = move.applyCellZ(lowZ);
                        int secondZ = move.applyCellZ(highZ);
                        next.add(new CellPiece(
                                Math.min(firstX, secondX), Math.max(firstX, secondX),
                                Math.min(firstZ, secondZ), Math.max(firstZ, secondZ),
                                piece.applied().then(move)));
                    }
                }

                pieces = next;
            }

            return pieces;
        }

        private static List<double[]> sliceSpan(WrapDomain domain, double min, double max) {
            double length = Math.min(max - min, domain.domainLength);
            double end = min + length;
            List<double[]> slices = new ArrayList<>();
            double cursor = min;
            while (cursor < end) {
                long laps = (long) Math.floor((cursor - domain.lowerBound) / domain.domainLength);
                double lapEnd = domain.lowerBound + (laps + 1) * (double) domain.domainLength;
                double sliceEnd = Math.min(end, lapEnd);
                slices.add(new double[] {cursor, sliceEnd});
                cursor = sliceEnd;
            }

            return slices;
        }

        private static List<int[]> sliceCellSpan(WrapDomain domain, int min, int max) {
            long length = Math.min((long) max - min + 1, domain.domainLength);
            long end = min + length - 1;
            List<int[]> slices = new ArrayList<>();
            long cursor = min;
            while (cursor <= end) {
                long laps = Math.floorDiv(cursor - domain.lowerBound, domain.domainLength);
                long lapEnd = domain.lowerBound + (laps + 1) * (long) domain.domainLength - 1;
                long sliceEnd = Math.min(end, lapEnd);
                slices.add(new int[] {(int) cursor, (int) sliceEnd});
                cursor = sliceEnd + 1;
            }

            return slices;
        }

        private static boolean closerToRest(long firstX, long firstZ, long secondX, long secondZ) {
            long first = squared(firstX) + squared(firstZ);
            long second = squared(secondX) + squared(secondZ);
            if (first != second) {
                return first < second;
            }

            return firstX != secondX ? firstX < secondX : firstZ < secondZ;
        }

        private static boolean closerToRest(double firstX, double firstZ, double secondX, double secondZ) {
            double first = squared(firstX) + squared(firstZ);
            double second = squared(secondX) + squared(secondZ);
            if (first != second) {
                return first < second;
            }

            return firstX != secondX ? firstX < secondX : firstZ < secondZ;
        }

        private static long squared(long value) {
            return value * value;
        }

        private static double squared(double value) {
            return value * value;
        }
    }
}
