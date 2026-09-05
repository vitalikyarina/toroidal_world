package com.toroidalworld.core;

import static com.toroidalworld.core.WorldFoldFixture.EVEN;
import static com.toroidalworld.core.WorldFoldFixture.PER_AXIS;
import static com.toroidalworld.core.WorldFoldFixture.SQUARE;
import static com.toroidalworld.core.WorldFoldFixture.X_ONLY_BOUNDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

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

class IdentityFastPathTest {
    private static final long SEED = 0x1DEA5L;
    private static final int SAMPLES = 600;
    private static final int CHUNK_BLOCKS = 16;
    private static final int BLOCK_REACH_CAP = 16_000;
    private static final int CHUNK_REACH_CAP = 1_000;
    private static final int UNBOUNDED_REGION_REACH = 64;
    private static final int WORLD_FLOOR = -64;
    private static final int WORLD_HEIGHT = 384;
    private static final int SECTION_FLOOR = -4;
    private static final int SECTION_COUNT = 24;
    private static final int SKEW_CHUNKS = 5;
    private static final int MIRROR_LINE_CHUNK = 5;
    private static final double NETHER_SCALE = 0.125;
    private static final double SHIFT_TOLERANCE = 1.0e-9;
    private static final int COPY_REACH = 3;
    private static final int WIDE_REGION_LAPS = 3;

    private static final List<WorldFold> DECK_GROUP = List.of(
            new DeckGroupFold(FlatShape.torus(SQUARE)),
            new DeckGroupFold(FlatShape.latticeTorus(SQUARE, SKEW_CHUNKS)),
            new DeckGroupFold(FlatShape.mirrored(X_ONLY_BOUNDS, Direction.Axis.Z, 0)),
            new DeckGroupFold(FlatShape.mirrored(SQUARE, Direction.Axis.Z, MIRROR_LINE_CHUNK)));

    private static final List<WorldFold> FOLDS = Stream.concat(PER_AXIS.stream(), DECK_GROUP.stream()).toList();

    private static final List<WorldFold> DECOMPOSABLE = FOLDS.stream().filter(WorldFold::decomposesPerAxis).toList();

    @Test
    void blockFoldKeepsItsValueAndReturnsAnInBoundsArgumentUntouched() {
        forEach(FOLDS, (fold, random) -> {
            BlockPos pos = sampleBlockPos(random, fold);
            BlockPos folded = fold.fold(pos);

            if (fold.decomposesPerAxis()) {
                assertEquals(new BlockPos(blockX(fold).wrap(pos.getX()), pos.getY(), blockZ(fold).wrap(pos.getZ())),
                        folded, () -> "fold(" + pos + ") " + in(fold));
            }

            assertUntouched(pos, folded, !fold.isOver(pos), fold);
        });
    }

    @Test
    void blockNearestCopyOfAnInsidePositionKeepsItsValueAndReturnsAnUnshiftedArgumentUntouched() {
        forEach(FOLDS, (fold, random) -> {
            BlockPos anchor = sampleBlockPos(random, fold);
            BlockPos inside = insideBlockPos(random, fold);
            BlockPos nearest = fold.nearestCopy(anchor, inside);

            if (fold.decomposesPerAxis()) {
                assertEquals(new BlockPos(
                        blockX(fold).unwrap(anchor.getX(), inside.getX()), inside.getY(),
                        blockZ(fold).unwrap(anchor.getZ(), inside.getZ())),
                        nearest, () -> "nearestCopy(" + anchor + ", " + inside + ") " + in(fold));
            }

            assertUntouched(inside, nearest, fold);
        });
    }

    @Test
    void chunkFoldKeepsItsValueAndReturnsAnInBoundsArgumentUntouched() {
        forEach(FOLDS, (fold, random) -> {
            ChunkPos pos = sampleChunkPos(random, fold);
            ChunkPos folded = fold.fold(pos);

            if (fold.decomposesPerAxis()) {
                assertEquals(new ChunkPos(chunkX(fold).wrap(pos.x()), chunkZ(fold).wrap(pos.z())), folded,
                        () -> "fold(" + pos + ") " + in(fold));
            }

            assertUntouched(pos, folded, !fold.isOver(pos), fold);
        });
    }

    @Test
    void chunkNearestCopyOfAnInsideChunkKeepsItsValueAndReturnsAnUnshiftedArgumentUntouched() {
        forEach(FOLDS, (fold, random) -> {
            ChunkPos anchor = sampleChunkPos(random, fold);
            ChunkPos inside = insideChunkPos(random, fold);
            ChunkPos nearest = fold.nearestCopy(anchor, inside);

            if (fold.decomposesPerAxis()) {
                assertEquals(new ChunkPos(
                        chunkX(fold).unwrap(anchor.x(), inside.x()), chunkZ(fold).unwrap(anchor.z(), inside.z())),
                        nearest, () -> "nearestCopy(" + anchor + ", " + inside + ") " + in(fold));
            }

            assertUntouched(inside, nearest, fold);
        });
    }

    @Test
    void sectionFoldKeepsItsValueAndReturnsAnInBoundsArgumentUntouched() {
        forEach(FOLDS, (fold, random) -> {
            SectionPos pos = sampleSectionPos(random, fold);
            SectionPos folded = fold.fold(pos);

            if (fold.decomposesPerAxis()) {
                assertEquals(SectionPos.of(chunkX(fold).wrap(pos.x()), pos.y(), chunkZ(fold).wrap(pos.z())), folded,
                        () -> "fold(" + pos + ") " + in(fold));
            }

            assertUntouched(pos, folded, !fold.isOver(pos.chunk()), fold);
        });
    }

    @Test
    void theFoldTransformationSeatsThePositionAndStandsDownInsideTheBounds() {
        forEach(FOLDS, (fold, random) -> {
            Vec3 vec = sampleVec(random, fold);
            DeckTransformation lap = fold.foldTransformation(vec);

            assertEquals(fold.fold(vec), lap.apply(vec),
                    () -> "foldTransformation(" + vec + ").apply is not fold(" + vec + ") " + in(fold));

            if (!fold.isOver(vec)) {
                assertSame(DeckTransformation.IDENTITY, lap,
                        () -> "foldTransformation(" + vec + ") allocated an identity " + in(fold));
                assertSame(vec, lap.apply(vec),
                        () -> "foldTransformation(" + vec + ").apply rebuilt its argument " + in(fold));
            }
        });
    }

    @Test
    void vectorFoldKeepsItsValueAndReturnsAnInBoundsArgumentUntouched() {
        forEach(FOLDS, (fold, random) -> {
            Vec3 vec = sampleVec(random, fold);
            Vec3 folded = fold.fold(vec);

            if (fold.decomposesPerAxis()) {
                assertEquals(new Vec3(blockX(fold).wrap(vec.x), vec.y, blockZ(fold).wrap(vec.z)), folded,
                        () -> "fold(" + vec + ") " + in(fold));
            }

            assertUntouched(vec, folded, !fold.isOver(vec), fold);
        });
    }

    @Test
    void vectorNearestCopyKeepsItsValueAndReturnsAnUnshiftedTargetUntouched() {
        forEach(FOLDS, (fold, random) -> {
            Vec3 ref = sampleVec(random, fold);
            Vec3 target = sampleVec(random, fold);
            Vec3 nearest = fold.nearestCopy(ref, target);

            if (fold.decomposesPerAxis()) {
                assertEquals(new Vec3(
                        nearestCopy(blockX(fold), ref.x, target.x), target.y, nearestCopy(blockZ(fold), ref.z, target.z)),
                        nearest, () -> "nearestCopy(" + ref + ", " + target + ") " + in(fold));
            }

            assertUntouched(target, nearest, fold);
        });
    }

    @Test
    void blockNearestCopyKeepsItsValueAndReturnsAnUnshiftedTargetUntouched() {
        forEach(FOLDS, (fold, random) -> {
            BlockPos ref = sampleBlockPos(random, fold);
            BlockPos target = sampleBlockPos(random, fold);
            BlockPos nearest = fold.nearestCopy(ref, target);

            if (fold.decomposesPerAxis()) {
                assertEquals(new BlockPos(
                        nearestCopy(blockX(fold), ref.getX(), target.getX()), target.getY(),
                        nearestCopy(blockZ(fold), ref.getZ(), target.getZ())),
                        nearest, () -> "nearestCopy(" + ref + ", " + target + ") " + in(fold));
            }

            assertUntouched(target, nearest, fold);
        });
    }

    @Test
    void theOrientedFoldsCarryTheSameValueAndTheSameInstanceAsThePlainOnes() {
        forEach(FOLDS, (fold, random) -> {
            BlockPos block = sampleBlockPos(random, fold);
            Vec3 vec = sampleVec(random, fold);
            ChunkPos chunk = sampleChunkPos(random, fold);
            BlockPos orientedBlock = fold.foldOriented(block).value();
            Vec3 orientedVec = fold.foldOriented(vec).value();
            ChunkPos orientedChunk = fold.foldOriented(chunk).value();

            assertEquals(fold.fold(block), orientedBlock, () -> "foldOriented(" + block + ") " + in(fold));
            assertEquals(fold.fold(vec), orientedVec, () -> "foldOriented(" + vec + ") " + in(fold));
            assertEquals(fold.fold(chunk), orientedChunk, () -> "foldOriented(" + chunk + ") " + in(fold));
            assertUntouched(block, orientedBlock, !fold.isOver(block), fold);
            assertUntouched(vec, orientedVec, !fold.isOver(vec), fold);
            assertUntouched(chunk, orientedChunk, !fold.isOver(chunk), fold);
        });
    }

    @Test
    void theOrientedNearestCopiesCarryTheSameValueAndTheSameInstanceAsThePlainOnes() {
        forEach(FOLDS, (fold, random) -> {
            BlockPos blockRef = sampleBlockPos(random, fold);
            BlockPos block = sampleBlockPos(random, fold);
            Vec3 vecRef = sampleVec(random, fold);
            Vec3 vec = sampleVec(random, fold);
            BlockPos orientedBlock = fold.nearestCopyOriented(blockRef, block).value();
            Vec3 orientedVec = fold.nearestCopyOriented(vecRef, vec).value();

            assertEquals(fold.nearestCopy(blockRef, block), orientedBlock,
                    () -> "nearestCopyOriented(" + blockRef + ", " + block + ") " + in(fold));
            assertEquals(fold.nearestCopy(vecRef, vec), orientedVec,
                    () -> "nearestCopyOriented(" + vecRef + ", " + vec + ") " + in(fold));
            assertUntouched(block, orientedBlock, fold);
            assertUntouched(vec, orientedVec, fold);
        });
    }

    @Test
    void reseatKeepsTheLowBitsAndReturnsTheArgumentUntouchedInItsOwnChunk() {
        forEach(FOLDS, (fold, random) -> {
            BlockPos pos = sampleBlockPos(random, fold);
            ChunkPos chunk = ChunkPos.containing(pos);
            ChunkPos copy = fold.decomposesPerAxis()
                    ? new ChunkPos(chunk.x() + lapsOf(random, fold.bounds().x()), chunk.z() + lapsOf(random, fold.bounds().z()))
                    : chunk;
            BlockPos reseated = fold.reseat(pos, copy);

            assertEquals(new BlockPos(
                    copy.getMinBlockX() + Math.floorMod(pos.getX(), CHUNK_BLOCKS), pos.getY(),
                    copy.getMinBlockZ() + Math.floorMod(pos.getZ(), CHUNK_BLOCKS)),
                    reseated, () -> "reseat(" + pos + ", " + copy + ") " + in(fold));
            assertUntouched(pos, reseated, copy.equals(chunk), fold);
        });
    }

    @Test
    void theDeckTransformationOfAChunkOntoItselfIsTheIdentityInstance() {
        forEach(FOLDS, (fold, random) -> {
            ChunkPos chunk = sampleChunkPos(random, fold);
            BoundingBox box = new BoundingBox(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(),
                    chunk.getMaxBlockX(), 10, chunk.getMaxBlockZ());

            assertSame(DeckTransformation.IDENTITY, fold.deckTransformation(chunk, chunk),
                    () -> "a chunk carried onto itself " + in(fold));
            assertSame(box, DeckTransformation.IDENTITY.apply(box), "the identity rebuilt a box");
            assertSame(chunk, DeckTransformation.IDENTITY.apply(chunk), "the identity rebuilt a chunk");
        });
    }

    @Test
    void crossesBoundsAgreesWithWhetherTheSplitChangesAnything() {
        forEach(FOLDS, (fold, random) -> {
            AABB box = sampleBox(random, fold);
            List<WorldFold.Folded<AABB>> pieces = fold.split(box);
            boolean untouched = pieces.size() == 1 && pieces.getFirst().value() == box;

            assertEquals(untouched, !fold.crossesBounds(box), () -> "crossesBounds(" + box + ") " + in(fold));
        });
    }

    @Test
    void aRegionInsideTheBoundsSplitsIntoItself() {
        forEach(FOLDS, (fold, random) -> {
            BoundingBox region = sampleRegion(random, fold);
            List<WorldFold.Folded<BoundingBox>> pieces = fold.split(region);
            boolean untouched = pieces.size() == 1 && pieces.getFirst().value() == region;

            assertEquals(untouched, !fold.crossesBounds(region), () -> "crossesBounds(" + region + ") " + in(fold));
        });
    }

    @Test
    void foldBoxKeepsItsValueAndReturnsABoxWhoseCentreNeedsNoShiftUntouched() {
        forEach(FOLDS, (fold, random) -> {
            AABB box = sampleBox(random, fold);
            boolean anchoredAtTheCentre = random.nextBoolean();
            Vec3 ref = anchoredAtTheCentre ? box.getCenter() : sampleVec(random, fold);
            AABB folded = fold.foldBox(ref, box).value();

            if (fold.decomposesPerAxis()) {
                double centerX = (box.minX + box.maxX) / 2.0;
                double centerZ = (box.minZ + box.maxZ) / 2.0;
                double shiftX = nearestCopy(blockX(fold), ref.x, centerX) - centerX;
                double shiftZ = nearestCopy(blockZ(fold), ref.z, centerZ) - centerZ;
                assertBoxEquals(box.move(shiftX, 0.0, shiftZ), folded,
                        () -> "foldBox(" + ref + ", " + box + ") " + in(fold));
            }

            assertUntouched(box, folded, anchoredAtTheCentre, fold);
        });
    }

    @Test
    void foldAcrossSeamReturnsARegionSpanningNoSeamUntouched() {
        forEach(PER_AXIS, (fold, random) -> {
            BoundingBox region = sampleRegion(random, fold);
            BoundingBox folded = SeamSpans.foldAcrossSeam(fold, region);

            assertUntouched(region, folded, !SeamSpans.crossesSeam(fold, region), fold);
        });
    }

    @Test
    void dimensionMappingReturnsAPositionThatMapsOntoItselfUntouched() {
        forEach(PER_AXIS, (fold, random) -> {
            Vec3 inside = insideVec(random, fold);
            WorldFold destination = PER_AXIS.get(random.nextInt(PER_AXIS.size()));
            Vec3 position = sampleVec(random, fold);
            double declaredScale = random.nextBoolean() ? 1.0 : NETHER_SCALE;

            assertSame(inside, DimensionMapping.map(fold, fold, inside, 1.0),
                    () -> "a position mapped onto its own world " + in(fold));
            assertUntouched(position, DimensionMapping.map(fold, destination, position, declaredScale), fold);
        });
    }

    @Test
    void copiesTouchingIsTheLapProductWhereTheShapeDecomposes() {
        forEach(DECOMPOSABLE, (fold, random) -> {
            BoundingBox region = wideRegion(random, fold);
            int reach = random.nextInt(COPY_REACH + 1);
            List<DeckTransformation> copies = fold.copiesTouching(region, reach);

            Set<DeckTransformation> expected = new HashSet<>();
            for (int lapX : laps(blockX(fold), region.minX(), region.maxX(), reach)) {
                for (int lapZ : laps(blockZ(fold), region.minZ(), region.maxZ(), reach)) {
                    expected.add(new DeckTransformation(SeamTransform.translation(
                            lapX * blockX(fold).domainLength, lapZ * blockZ(fold).domainLength)));
                }
            }

            assertEquals(expected, new HashSet<>(copies),
                    () -> "copiesTouching(" + region + ", " + reach + ") " + in(fold));
            assertEquals(expected.size(), copies.size(), () -> "a copy listed twice " + in(fold));
            for (int index = 0; index < copies.size(); index++) {
                if (copies.get(index).isIdentity()) {
                    assertEquals(0, index, () -> "the identity is not first in " + copies + " " + in(fold));
                    assertSame(DeckTransformation.IDENTITY, copies.get(index), () -> in(fold));
                }
            }
        });
    }

    @Test
    void foldsOntoItselfIsWiderThanTheWorldOnEitherAxisWhereTheShapeDecomposes() {
        forEach(DECOMPOSABLE, (fold, random) -> {
            BoundingBox region = wideRegion(random, fold);
            boolean expected = widerThanTheWorld(blockX(fold), region.getXSpan())
                    || widerThanTheWorld(blockZ(fold), region.getZSpan());

            assertEquals(expected, fold.foldsOntoItself(region), () -> "foldsOntoItself(" + region + ") " + in(fold));
        });
    }

    @Test
    void aRegionInsideTheBoundsListsTheIdentityInstanceAloneAndNeverFoldsOntoItself() {
        forEach(FOLDS, (fold, random) -> {
            BoundingBox region = insideRegion(random, fold);
            List<DeckTransformation> copies = fold.copiesTouching(region, random.nextInt(COPY_REACH + 1));

            assertEquals(1, copies.size(), () -> "copiesTouching(" + region + ") " + in(fold));
            assertSame(DeckTransformation.IDENTITY, copies.getFirst(), () -> in(fold));
            assertFalse(fold.foldsOntoItself(region), () -> "foldsOntoItself(" + region + ") " + in(fold));
        });
    }

    @Test
    void aNegativeReachIsRefused() {
        forEach(FOLDS, (fold, random) -> {
            BoundingBox region = sampleRegion(random, fold);
            assertThrows(IllegalArgumentException.class, () -> fold.copiesTouching(region, -1), () -> in(fold));
        });
    }

    @Test
    void aBoxWhoseFarEdgeRestsOnTheUpperBoundCrossesNothing() {
        AABB box = new AABB(500.0, 60.0, 500.0, 512.0, 62.0, 512.0);

        assertSame(box, EVEN.split(box).getFirst().value());
    }

    @Test
    void aBoxOfNoWidthOnTheUpperBoundStillFolds() {
        AABB box = new AABB(512.0, 60.0, 512.0, 512.0, 60.0, 512.0);
        AABB folded = EVEN.split(box).getFirst().value();

        assertNotSame(box, folded);
        assertEquals(-512.0, folded.minX);
        assertEquals(-512.0, folded.minZ);
    }

    @Test
    void aBoxRunningPastTheBoundsIsStillCutApart() {
        AABB box = new AABB(500.0, 60.0, -100.0, 520.0, 62.0, -90.0);

        assertEquals(2, EVEN.split(box).size());
    }

    private static void assertBoxEquals(AABB expected, AABB actual, Supplier<String> message) {
        assertEquals(expected.minX, actual.minX, SHIFT_TOLERANCE, message);
        assertEquals(expected.minY, actual.minY, SHIFT_TOLERANCE, message);
        assertEquals(expected.minZ, actual.minZ, SHIFT_TOLERANCE, message);
        assertEquals(expected.maxX, actual.maxX, SHIFT_TOLERANCE, message);
        assertEquals(expected.maxY, actual.maxY, SHIFT_TOLERANCE, message);
        assertEquals(expected.maxZ, actual.maxZ, SHIFT_TOLERANCE, message);
    }

    private static <T> void assertUntouched(T argument, T result, WorldFold fold) {
        assertUntouched(argument, result, false, fold);
    }

    private static <T> void assertUntouched(T argument, T result, boolean mustBeUnmoved, WorldFold fold) {
        if (mustBeUnmoved || result.equals(argument)) {
            assertSame(argument, result, () -> "an unmoved " + argument + " must come back as itself " + in(fold));
        }
    }

    private static int lapsOf(Random random, AxisBounds axis) {
        return axis instanceof AxisBounds.Looped looped ? (random.nextInt(5) - 2) * looped.chunkWidth() : 0;
    }

    private static double nearestCopy(WrapDomain domain, double ref, double coord) {
        if (domain instanceof WrapDomain.Noop) {
            return coord;
        }

        double nearest = coord;
        for (int laps = -8; laps <= 8; laps++) {
            double candidate = coord - laps * (double) domain.domainLength;
            double candidateGap = Math.abs(candidate - ref);
            double nearestGap = Math.abs(nearest - ref);
            if (candidateGap < nearestGap
                    || (candidateGap == nearestGap && Math.abs(candidate - coord) < Math.abs(nearest - coord))) {
                nearest = candidate;
            }
        }

        return nearest;
    }

    private static int nearestCopy(WrapDomain domain, int ref, int coord) {
        return (int) nearestCopy(domain, (double) ref, (double) coord);
    }

    private interface Case {
        void check(WorldFold fold, Random random);
    }

    private static void forEach(List<WorldFold> folds, Case body) {
        for (WorldFold fold : folds) {
            Random random = new Random(SEED);
            for (int sample = 0; sample < SAMPLES; sample++) {
                body.check(fold, random);
            }
        }
    }

    private static int sampleCoord(Random random, int width, int cap) {
        int span = width == 0 ? cap : Math.min(3 * width, cap);
        return random.nextInt(2 * span + 1) - span;
    }

    private static int sampleBlockCoord(Random random, AxisBounds axis) {
        return sampleCoord(random, axis instanceof AxisBounds.Looped looped ? looped.blockWidth() : 0, BLOCK_REACH_CAP);
    }

    private static int sampleChunkCoord(Random random, AxisBounds axis) {
        return sampleCoord(random, axis instanceof AxisBounds.Looped looped ? looped.chunkWidth() : 0, CHUNK_REACH_CAP);
    }

    private static int insideBlockCoord(Random random, AxisBounds axis) {
        return axis instanceof AxisBounds.Looped looped
                ? looped.minBlock() + random.nextInt(looped.blockWidth())
                : sampleBlockCoord(random, axis);
    }

    private static int insideChunkCoord(Random random, AxisBounds axis) {
        return axis instanceof AxisBounds.Looped looped
                ? looped.minChunk() + random.nextInt(looped.chunkWidth())
                : sampleChunkCoord(random, axis);
    }

    private static int regionSpan(Random random, AxisBounds axis) {
        return random.nextInt(
                (axis instanceof AxisBounds.Looped looped ? looped.blockWidth() : UNBOUNDED_REGION_REACH) + 1);
    }

    private static int sampleY(Random random) {
        return WORLD_FLOOR + random.nextInt(WORLD_HEIGHT);
    }

    private static BlockPos sampleBlockPos(Random random, WorldFold fold) {
        return new BlockPos(sampleBlockCoord(random, fold.bounds().x()), sampleY(random),
                sampleBlockCoord(random, fold.bounds().z()));
    }

    private static BlockPos insideBlockPos(Random random, WorldFold fold) {
        return new BlockPos(insideBlockCoord(random, fold.bounds().x()), sampleY(random),
                insideBlockCoord(random, fold.bounds().z()));
    }

    private static ChunkPos sampleChunkPos(Random random, WorldFold fold) {
        return new ChunkPos(sampleChunkCoord(random, fold.bounds().x()), sampleChunkCoord(random, fold.bounds().z()));
    }

    private static ChunkPos insideChunkPos(Random random, WorldFold fold) {
        return new ChunkPos(insideChunkCoord(random, fold.bounds().x()), insideChunkCoord(random, fold.bounds().z()));
    }

    private static SectionPos sampleSectionPos(Random random, WorldFold fold) {
        return SectionPos.of(sampleChunkCoord(random, fold.bounds().x()),
                SECTION_FLOOR + random.nextInt(SECTION_COUNT), sampleChunkCoord(random, fold.bounds().z()));
    }

    private static Vec3 sampleVec(Random random, WorldFold fold) {
        return new Vec3(sampleBlockCoord(random, fold.bounds().x()) + random.nextDouble(),
                sampleY(random) + random.nextDouble(),
                sampleBlockCoord(random, fold.bounds().z()) + random.nextDouble());
    }

    private static Vec3 insideVec(Random random, WorldFold fold) {
        return new Vec3(insideBlockCoord(random, fold.bounds().x()) + random.nextDouble(),
                sampleY(random) + random.nextDouble(),
                insideBlockCoord(random, fold.bounds().z()) + random.nextDouble());
    }

    private static AABB sampleBox(Random random, WorldFold fold) {
        double minX = sampleBlockCoord(random, fold.bounds().x()) + random.nextDouble();
        double minZ = sampleBlockCoord(random, fold.bounds().z()) + random.nextDouble();
        double minY = sampleY(random);
        return new AABB(minX, minY, minZ,
                minX + random.nextDouble() * 24.0, minY + random.nextDouble() * 4.0,
                minZ + random.nextDouble() * 24.0);
    }

    private static BoundingBox sampleRegion(Random random, WorldFold fold) {
        int minX = sampleBlockCoord(random, fold.bounds().x());
        int minZ = sampleBlockCoord(random, fold.bounds().z());
        int minY = sampleY(random);
        return new BoundingBox(minX, minY, minZ,
                minX + regionSpan(random, fold.bounds().x()), minY + random.nextInt(CHUNK_BLOCKS),
                minZ + regionSpan(random, fold.bounds().z()));
    }

    private static List<Integer> laps(WrapDomain domain, int min, int max, int reach) {
        if (domain instanceof WrapDomain.Noop) {
            return List.of(0);
        }

        int first = Math.max(Math.floorDiv(min - domain.lowerBound, domain.domainLength), -reach);
        int last = Math.min(Math.floorDiv(max - domain.lowerBound, domain.domainLength), reach);
        List<Integer> laps = new ArrayList<>();
        for (int lap = first; lap <= last; lap++) {
            laps.add(lap);
        }

        return laps;
    }

    private static boolean widerThanTheWorld(WrapDomain domain, int span) {
        return !(domain instanceof WrapDomain.Noop) && span > domain.domainLength;
    }

    private static int wideSpan(Random random, AxisBounds axis) {
        return random.nextInt((axis instanceof AxisBounds.Looped looped
                ? WIDE_REGION_LAPS * looped.blockWidth()
                : UNBOUNDED_REGION_REACH) + 1);
    }

    private static BoundingBox wideRegion(Random random, WorldFold fold) {
        int minX = sampleBlockCoord(random, fold.bounds().x());
        int minZ = sampleBlockCoord(random, fold.bounds().z());
        int minY = sampleY(random);
        return new BoundingBox(minX, minY, minZ,
                minX + wideSpan(random, fold.bounds().x()), minY + random.nextInt(CHUNK_BLOCKS),
                minZ + wideSpan(random, fold.bounds().z()));
    }

    private static int insideSpan(Random random, AxisBounds axis, int min) {
        return axis instanceof AxisBounds.Looped looped
                ? random.nextInt(looped.maxBlock() - min)
                : regionSpan(random, axis);
    }

    private static BoundingBox insideRegion(Random random, WorldFold fold) {
        int minX = insideBlockCoord(random, fold.bounds().x());
        int minZ = insideBlockCoord(random, fold.bounds().z());
        int minY = sampleY(random);
        return new BoundingBox(minX, minY, minZ,
                minX + insideSpan(random, fold.bounds().x(), minX), minY + random.nextInt(CHUNK_BLOCKS),
                minZ + insideSpan(random, fold.bounds().z(), minZ));
    }

    private static WrapDomain blockX(WorldFold fold) {
        return fold.blockDomain(Direction.Axis.X);
    }

    private static WrapDomain blockZ(WorldFold fold) {
        return fold.blockDomain(Direction.Axis.Z);
    }

    private static WrapDomain chunkX(WorldFold fold) {
        return fold.chunkDomain(Direction.Axis.X);
    }

    private static WrapDomain chunkZ(WorldFold fold) {
        return fold.chunkDomain(Direction.Axis.Z);
    }

    private static String in(WorldFold fold) {
        return "in " + fold;
    }
}
