package com.toroidalworld.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold.Folded;
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

class DeckGroupFoldMatrixTest {
    private static final int UNIT = 16;

    private static final int MIN_CHUNK = -8;
    private static final int MAX_CHUNK = 8;
    private static final int WIDTH = (MAX_CHUNK - MIN_CHUNK) * UNIT;
    private static final int LOWER = MIN_CHUNK * UNIT;
    private static final int UPPER = MAX_CHUNK * UNIT;

    private static final int SKEW_CHUNKS = 3;
    private static final int SKEW = SKEW_CHUNKS * UNIT;

    private static final int OFFSET_MIRROR_CHUNK = 3;
    private static final int OFFSET_MIRROR_LINE = OFFSET_MIRROR_CHUNK * UNIT;

    private static final int ORBIT_REACH = 8;
    private static final int SAMPLES = 300;
    private static final long SEED = 0x70201DL;

    private static final AxisBounds.Looped LOOPED = new AxisBounds.Looped(MIN_CHUNK, MAX_CHUNK);
    private static final AxisBounds UNBOUNDED = AxisBounds.Unbounded.INSTANCE;
    private static final WorldLoopBounds BOTH = new WorldLoopBounds(LOOPED, LOOPED);
    private static final WorldLoopBounds X_ONLY = new WorldLoopBounds(LOOPED, UNBOUNDED);

    private record Group(boolean xLoops, boolean zLoops, int skew, boolean mirrorsZ, int mirrorLine) {
        int[] cell(int x, int z, int first, int second) {
            int outX = x;
            int outZ = z;
            if (this.xLoops) {
                outX += first * WIDTH;
                if (this.mirrorsZ && (first & 1) != 0) {
                    outZ = 2 * this.mirrorLine - 1 - outZ;
                }
            }

            if (this.zLoops) {
                outX += second * this.skew;
                outZ += second * WIDTH;
            }

            return new int[] {outX, outZ};
        }

        double[] coord(double x, double z, int first, int second) {
            double outX = x;
            double outZ = z;
            if (this.xLoops) {
                outX += first * WIDTH;
                if (this.mirrorsZ && (first & 1) != 0) {
                    outZ = 2 * this.mirrorLine - outZ;
                }
            }

            if (this.zLoops) {
                outX += second * this.skew;
                outZ += second * WIDTH;
            }

            return new double[] {outX, outZ};
        }
    }

    private record Case(String name, FlatShape shape, Group group) {
        DeckGroupFold fold() {
            return new DeckGroupFold(this.shape);
        }
    }

    private static final Case RECTANGLE = new Case("rectangle",
            FlatShape.rectangle(), new Group(false, false, 0, false, 0));

    private static final Case CYLINDER = new Case("cylinder",
            FlatShape.cylinder(X_ONLY), new Group(true, false, 0, false, 0));

    private static final Case TORUS = new Case("torus",
            FlatShape.latticeTorus(BOTH, 0), new Group(true, true, 0, false, 0));

    private static final Case LATTICE_TORUS = new Case("lattice torus",
            FlatShape.latticeTorus(BOTH, SKEW_CHUNKS), new Group(true, true, SKEW, false, 0));

    private static final Case MOBIUS = new Case("mobius",
            FlatShape.mirrored(X_ONLY, Direction.Axis.Z, 0), new Group(true, false, 0, true, 0));

    private static final Case KLEIN_CENTRED = new Case("klein (mirror on the world centre)",
            FlatShape.mirrored(BOTH, Direction.Axis.Z, 0), new Group(true, true, 0, true, 0));

    private static final Case KLEIN_OFFSET = new Case("klein (mirror off centre)",
            FlatShape.mirrored(BOTH, Direction.Axis.Z, OFFSET_MIRROR_CHUNK),
            new Group(true, true, 0, true, OFFSET_MIRROR_LINE));

    private static List<Case> cases() {
        return List.of(RECTANGLE, CYLINDER, TORUS, LATTICE_TORUS, MOBIUS, KLEIN_CENTRED, KLEIN_OFFSET);
    }

    private static List<Case> mirroredCases() {
        return List.of(MOBIUS, KLEIN_CENTRED, KLEIN_OFFSET);
    }

    private static List<Case> decomposableCases() {
        return List.of(RECTANGLE, CYLINDER, TORUS);
    }

    private static List<int[]> cellOrbit(Group group, int x, int z) {
        List<int[]> points = new ArrayList<>();
        for (int first = -ORBIT_REACH; first <= ORBIT_REACH; first++) {
            for (int second = -ORBIT_REACH; second <= ORBIT_REACH; second++) {
                points.add(group.cell(x, z, first, second));
            }
        }

        return points;
    }

    private static List<double[]> coordOrbit(Group group, double x, double z) {
        List<double[]> points = new ArrayList<>();
        for (int first = -ORBIT_REACH; first <= ORBIT_REACH; first++) {
            for (int second = -ORBIT_REACH; second <= ORBIT_REACH; second++) {
                points.add(group.coord(x, z, first, second));
            }
        }

        return points;
    }

    private static boolean inCellOrbit(Group group, int sourceX, int sourceZ, int x, int z) {
        for (int[] point : cellOrbit(group, sourceX, sourceZ)) {
            if (point[0] == x && point[1] == z) {
                return true;
            }
        }

        return false;
    }

    private static int sample(Random random) {
        return LOWER - 3 * WIDTH + random.nextInt(6 * WIDTH);
    }

    private static long squared(long value) {
        return value * value;
    }

    private static double squared(double value) {
        return value * value;
    }

    private static void assertInsideWorld(Case testCase, int x, int z, String what) {
        if (testCase.group().xLoops()) {
            assertTrue(x >= LOWER && x < UPPER,
                    testCase.name() + ": " + what + " x " + x + " outside [" + LOWER + ", " + UPPER + ")");
        }

        if (testCase.group().zLoops()) {
            assertTrue(z >= LOWER && z < UPPER,
                    testCase.name() + ": " + what + " z " + z + " outside [" + LOWER + ", " + UPPER + ")");
        }
    }

    @Nested
    class Fold {
        @Test
        void landsInTheFundamentalDomainAndStaysInTheOrbit() {
            for (Case testCase : cases()) {
                DeckGroupFold fold = testCase.fold();
                Random random = new Random(SEED);
                for (int sample = 0; sample < SAMPLES; sample++) {
                    int x = sample(random);
                    int z = sample(random);
                    BlockPos folded = fold.fold(new BlockPos(x, 64, z));
                    assertInsideWorld(testCase, folded.getX(), folded.getZ(), "fold");
                    assertTrue(inCellOrbit(testCase.group(), x, z, folded.getX(), folded.getZ()),
                            testCase.name() + ": the fold of (" + x + ", " + z + ") left the orbit");
                    assertEquals(folded, fold.fold(folded), testCase.name() + ": fold is not idempotent");
                }
            }
        }

        @Test
        void aPositionInsideTheWorldComesBackAsTheSameObject() {
            for (Case testCase : cases()) {
                DeckGroupFold fold = testCase.fold();
                BlockPos block = new BlockPos(LOWER + 1, 64, LOWER + 1);
                Vec3 vector = new Vec3(LOWER + 1.5, 64.0, LOWER + 1.5);
                ChunkPos chunk = new ChunkPos(MIN_CHUNK, MIN_CHUNK);
                assertSame(block, fold.fold(block), testCase.name() + ": an in-bounds block fold rebuilt it");
                assertSame(vector, fold.fold(vector), testCase.name() + ": an in-bounds vector fold rebuilt it");
                assertSame(chunk, fold.fold(chunk), testCase.name() + ": an in-bounds chunk fold rebuilt it");
            }
        }

        @Test
        void theCellFoldAgreesWithTheCoordinateFoldAtTheCellCentre() {
            for (Case testCase : cases()) {
                DeckGroupFold fold = testCase.fold();
                Random random = new Random(SEED + 3);
                for (int sample = 0; sample < SAMPLES; sample++) {
                    int x = sample(random);
                    int z = sample(random);
                    BlockPos cell = fold.fold(new BlockPos(x, 64, z));
                    Vec3 centre = fold.fold(new Vec3(x + 0.5, 64.0, z + 0.5));
                    assertEquals(cell.getX() + 0.5, centre.x, testCase.name() + ": cell and coordinate x disagree");
                    assertEquals(cell.getZ() + 0.5, centre.z, testCase.name() + ": cell and coordinate z disagree");
                }
            }
        }

        @Test
        void theChunkFoldAgreesWithTheBlockFold() {
            for (Case testCase : cases()) {
                DeckGroupFold fold = testCase.fold();
                Random random = new Random(SEED + 4);
                for (int sample = 0; sample < SAMPLES; sample++) {
                    int x = sample(random);
                    int z = sample(random);
                    BlockPos block = fold.fold(new BlockPos(x, 64, z));
                    ChunkPos chunk = fold.fold(new ChunkPos(Math.floorDiv(x, UNIT), Math.floorDiv(z, UNIT)));
                    assertEquals(chunk.x, Math.floorDiv(block.getX(), UNIT),
                            testCase.name() + ": the chunk fold and the block fold disagree on x");
                    assertEquals(chunk.z, Math.floorDiv(block.getZ(), UNIT),
                            testCase.name() + ": the chunk fold and the block fold disagree on z");
                }
            }
        }

        @Test
        void theKeyFoldsAgreeWithThePositionFolds() {
            for (Case testCase : cases()) {
                DeckGroupFold fold = testCase.fold();
                Random random = new Random(SEED + 5);
                for (int sample = 0; sample < SAMPLES; sample++) {
                    int x = sample(random);
                    int z = sample(random);
                    BlockPos block = new BlockPos(x, 64, z);
                    assertEquals(fold.fold(block).asLong(), fold.foldBlockNode(block.asLong()),
                            testCase.name() + ": the block node fold disagrees");

                    ChunkPos chunk = new ChunkPos(Math.floorDiv(x, UNIT), Math.floorDiv(z, UNIT));
                    assertEquals(ChunkPos.asLong(fold.fold(chunk).x, fold.fold(chunk).z),
                            fold.foldChunkKey(ChunkPos.asLong(chunk.x, chunk.z)),
                            testCase.name() + ": the chunk key fold disagrees");

                    SectionPos section = SectionPos.of(chunk.x, 4, chunk.z);
                    assertEquals(fold.fold(section).asLong(), fold.foldSectionNode(section.asLong()),
                            testCase.name() + ": the section node fold disagrees");
                }
            }
        }
    }

    @Nested
    class NearestCopy {
        @Test
        void minimisesOverTheWholeOrbitOnTheBlockGrid() {
            for (Case testCase : cases()) {
                DeckGroupFold fold = testCase.fold();
                Random random = new Random(SEED + 1);
                for (int sample = 0; sample < SAMPLES; sample++) {
                    int refX = sample(random);
                    int refZ = sample(random);
                    int targetX = sample(random);
                    int targetZ = sample(random);

                    BlockPos nearest = fold.nearestCopy(
                            new BlockPos(refX, 64, refZ), new BlockPos(targetX, 64, targetZ));
                    assertTrue(inCellOrbit(testCase.group(), targetX, targetZ, nearest.getX(), nearest.getZ()),
                            testCase.name() + ": nearestCopy left the orbit");

                    long best = Long.MAX_VALUE;
                    for (int[] point : cellOrbit(testCase.group(), targetX, targetZ)) {
                        best = Math.min(best, squared(point[0] - refX) + squared(point[1] - refZ));
                    }

                    assertEquals(best, squared(nearest.getX() - refX) + squared(nearest.getZ() - refZ),
                            testCase.name() + ": not the nearest copy of (" + targetX + ", " + targetZ
                                    + ") around (" + refX + ", " + refZ + ")");
                }
            }
        }

        @Test
        void minimisesOverTheWholeOrbitOnTheCoordinateGrid() {
            for (Case testCase : cases()) {
                DeckGroupFold fold = testCase.fold();
                Random random = new Random(SEED + 2);
                for (int sample = 0; sample < SAMPLES; sample++) {
                    double refX = sample(random) + 0.25;
                    double refZ = sample(random) + 0.75;
                    double targetX = sample(random) + 0.5;
                    double targetZ = sample(random) + 0.5;

                    Vec3 nearest = fold.nearestCopy(new Vec3(refX, 64.0, refZ), new Vec3(targetX, 64.0, targetZ));
                    double best = Double.MAX_VALUE;
                    for (double[] point : coordOrbit(testCase.group(), targetX, targetZ)) {
                        best = Math.min(best, squared(point[0] - refX) + squared(point[1] - refZ));
                    }

                    assertEquals(best, squared(nearest.x - refX) + squared(nearest.z - refZ),
                            testCase.name() + ": not the nearest coordinate copy");
                }
            }
        }

        @Test
        void theShortestDeltaIsTheNearestCopyMinusTheReference() {
            for (Case testCase : cases()) {
                DeckGroupFold fold = testCase.fold();
                Random random = new Random(SEED + 6);
                for (int sample = 0; sample < SAMPLES; sample++) {
                    Vec3 from = new Vec3(sample(random) + 0.5, 64.0, sample(random) + 0.5);
                    Vec3 to = new Vec3(sample(random) + 0.5, 70.0, sample(random) + 0.5);
                    Vec3 delta = fold.foldDelta(from, to);
                    assertEquals(fold.nearestCopy(from, to).subtract(from), delta,
                            testCase.name() + ": foldDelta is not nearestCopy minus the reference");
                    assertEquals(fold.sqrDistance(from, to),
                            delta.x * delta.x + delta.y * delta.y + delta.z * delta.z,
                            testCase.name() + ": sqrDistance disagrees with foldDelta");
                }
            }
        }

        @Test
        void aTieAtHalfAWorldTakesNoLaps() {
            DeckGroupFold fold = TORUS.fold();
            BlockPos ref = new BlockPos(0, 64, 0);
            for (int half : new int[] {WIDTH / 2, -WIDTH / 2}) {
                BlockPos target = new BlockPos(half, 64, 0);
                assertSame(target, fold.nearestCopy(ref, target),
                        "a tie at exactly half a world moved the argument");
                BlockPos across = new BlockPos(0, 64, half);
                assertSame(across, fold.nearestCopy(ref, across),
                        "a tie at exactly half a world moved the argument on z");
            }
        }

        @Test
        void theTieDoesNotDependOnWhereTheBoundsAreDrawn() {
            DeckGroupFold centred = new DeckGroupFold(FlatShape.latticeTorus(BOTH, 0));
            DeckGroupFold shifted = new DeckGroupFold(FlatShape.latticeTorus(
                    new WorldLoopBounds(new AxisBounds.Looped(-5, 11), new AxisBounds.Looped(-5, 11)), 0));
            BlockPos ref = new BlockPos(0, 64, 0);
            BlockPos target = new BlockPos(WIDTH / 2, 64, WIDTH / 2);
            BlockPos fromCentred = centred.nearestCopy(ref, target);
            BlockPos fromShifted = shifted.nearestCopy(ref, target);
            assertEquals(fromCentred.getX() - ref.getX(), fromShifted.getX() - ref.getX(),
                    "the tie was decided from the bounds, not from the arguments");
            assertEquals(fromCentred.getZ() - ref.getZ(), fromShifted.getZ() - ref.getZ(),
                    "the tie was decided from the bounds, not from the arguments");
        }

        @Test
        void aMirroredSeamOffersTheFlippedCopy() {
            DeckGroupFold fold = MOBIUS.fold();
            BlockPos ref = new BlockPos(UPPER - 1, 64, 100);
            BlockPos target = new BlockPos(LOWER + 1, 64, -100);
            Folded<BlockPos> nearest = fold.nearestCopyOriented(ref, target);

            assertEquals(new BlockPos(UPPER + 1, 64, 99), nearest.value(),
                    "the copy across a mirrored seam is not the flipped one");
            assertEquals(FoldOrientation.MIRROR_Z, nearest.orientation(),
                    "crossing a mirrored seam did not report the flip");
            assertTrue(squared(nearest.value().getX() - ref.getX()) + squared(nearest.value().getZ() - ref.getZ())
                            < squared(target.getX() - ref.getX()) + squared(target.getZ() - ref.getZ()),
                    "the flipped copy is not nearer than the unflipped one");
        }
    }

    @Nested
    class Orientation {
        @Test
        void aMirroredShapeReallyProducesAMirroredFold() {
            for (Case testCase : mirroredCases()) {
                DeckGroupFold fold = testCase.fold();
                boolean sawMirror = false;
                Random random = new Random(SEED + 7);
                for (int sample = 0; sample < SAMPLES && !sawMirror; sample++) {
                    Folded<BlockPos> folded = fold.foldOriented(new BlockPos(sample(random), 64, sample(random)));
                    sawMirror = folded.orientation() == FoldOrientation.MIRROR_Z;
                }

                assertTrue(sawMirror, testCase.name() + ": no sample ever folded through the mirror");
            }
        }

        @Test
        void anUnmirroredShapeNeverProducesOne() {
            for (Case testCase : List.of(RECTANGLE, CYLINDER, TORUS, LATTICE_TORUS)) {
                DeckGroupFold fold = testCase.fold();
                Random random = new Random(SEED + 7);
                for (int sample = 0; sample < SAMPLES; sample++) {
                    Folded<BlockPos> folded = fold.foldOriented(new BlockPos(sample(random), 64, sample(random)));
                    assertTrue(folded.isIdentity(), testCase.name() + ": an unmirrored fold reported a flip");
                }
            }
        }

        @Test
        void twoLapsAcrossAMirroredSeamReturnTheIdentity() {
            for (Case testCase : mirroredCases()) {
                DeckGroupFold fold = testCase.fold();
                int inside = LOWER + WIDTH / 2;
                BlockPos oneLap = new BlockPos(inside + WIDTH, 64, inside);
                BlockPos twoLaps = new BlockPos(inside + 2 * WIDTH, 64, inside);
                assertEquals(FoldOrientation.MIRROR_Z, fold.foldOriented(oneLap).orientation(),
                        testCase.name() + ": one lap across the mirrored seam did not flip");
                assertEquals(FoldOrientation.IDENTITY, fold.foldOriented(twoLaps).orientation(),
                        testCase.name() + ": two laps across the mirrored seam did not come back upright");
            }
        }

        @Test
        void theReportedOrientationExplainsTheFold() {
            for (Case testCase : cases()) {
                DeckGroupFold fold = testCase.fold();
                Random random = new Random(SEED + 8);
                int checked = 0;
                for (int sample = 0; sample < SAMPLES; sample++) {
                    Vec3 base = new Vec3(sample(random) + 0.5, 64.0, sample(random) + 0.5);
                    Vec3 offset = new Vec3(random.nextInt(WIDTH) - WIDTH / 2.0, 0.0,
                            random.nextInt(WIDTH) - WIDTH / 2.0);

                    Folded<Vec3> folded = fold.foldOriented(base);
                    Vec3 expected = folded.value().add(folded.orientation().applyToDelta(offset));
                    if (!insideWorld(testCase, expected)) {
                        continue;
                    }

                    checked++;
                    assertEquals(expected, fold.fold(base.add(offset)),
                            testCase.name() + ": the reported orientation does not explain the fold");
                }

                assertTrue(checked > SAMPLES / 10,
                        testCase.name() + ": only " + checked + " samples stayed inside the world — the "
                                + "orientation is barely being exercised");
            }
        }

        private static boolean insideWorld(Case testCase, Vec3 position) {
            if (testCase.group().xLoops() && (position.x < LOWER || position.x >= UPPER)) {
                return false;
            }

            return !testCase.group().zLoops() || (position.z >= LOWER && position.z < UPPER);
        }

        @Test
        void aDoubleMirrorKeepsHandednessAndComposesAwayToTheIdentity() {
            assertTrue(FoldOrientation.HALF_TURN.preservesHandedness(), "a half turn reverses handedness");
            assertFalse(FoldOrientation.MIRROR_X.preservesHandedness(), "a single mirror keeps handedness");
            assertEquals(FoldOrientation.HALF_TURN,
                    FoldOrientation.MIRROR_X.compose(FoldOrientation.MIRROR_Z), "mirrors do not compose");
            for (FoldOrientation orientation : FoldOrientation.values()) {
                assertEquals(FoldOrientation.IDENTITY, orientation.compose(orientation),
                        orientation + " is not its own inverse");
            }
        }
    }

    @Nested
    class ReductionOrder {
        @Test
        void reducingZBeforeXLeavesTheMirroredAxisOutsideTheWorld() {
            DeckGroupFold fold = KLEIN_OFFSET.fold();
            Random random = new Random(SEED + 9);
            boolean sawTheDifference = false;
            for (int sample = 0; sample < SAMPLES; sample++) {
                int x = sample(random);
                int z = sample(random);
                BlockPos folded = fold.fold(new BlockPos(x, 64, z));
                assertInsideWorld(KLEIN_OFFSET, folded.getX(), folded.getZ(), "fold");

                int[] reversed = reduceZBeforeX(x, z);
                if (reversed[1] < LOWER || reversed[1] >= UPPER) {
                    sawTheDifference = true;
                }
            }

            assertTrue(sawTheDifference,
                    "reducing Z before X never left the world — the order under test is not being exercised");
        }

        private static int[] reduceZBeforeX(int x, int z) {
            int zLaps = Math.floorDiv(z - LOWER, WIDTH);
            int reducedZ = z - zLaps * WIDTH;
            int xLaps = Math.floorDiv(x - LOWER, WIDTH);
            int reducedX = x - xLaps * WIDTH;
            if ((xLaps & 1) != 0) {
                reducedZ = 2 * OFFSET_MIRROR_LINE - 1 - reducedZ;
            }

            return new int[] {reducedX, reducedZ};
        }
    }

    @Nested
    class RegionOps {
        @Test
        void everySplitPieceLiesInsideTheWorld() {
            for (Case testCase : cases()) {
                DeckGroupFold fold = testCase.fold();
                Random random = new Random(SEED + 10);
                for (int sample = 0; sample < SAMPLES; sample++) {
                    BoundingBox region = randomRegion(random);
                    for (Folded<BoundingBox> piece : fold.split(region)) {
                        assertInsideWorld(testCase, piece.value().minX(), piece.value().minZ(), "piece min");
                        assertInsideWorld(testCase, piece.value().maxX(), piece.value().maxZ(), "piece max");
                    }
                }
            }
        }

        @Test
        void everyBlockOfTheRegionLandsInSomePiece() {
            for (Case testCase : cases()) {
                DeckGroupFold fold = testCase.fold();
                Random random = new Random(SEED + 11);
                for (int sample = 0; sample < 60; sample++) {
                    BoundingBox region = randomRegion(random);
                    List<Folded<BoundingBox>> pieces = fold.split(region);
                    for (int probe = 0; probe < 12; probe++) {
                        int x = region.minX() + random.nextInt(region.maxX() - region.minX() + 1);
                        int z = region.minZ() + random.nextInt(region.maxZ() - region.minZ() + 1);
                        BlockPos folded = fold.fold(new BlockPos(x, 64, z));
                        boolean covered = false;
                        for (Folded<BoundingBox> piece : pieces) {
                            covered |= folded.getX() >= piece.value().minX() && folded.getX() <= piece.value().maxX()
                                    && folded.getZ() >= piece.value().minZ()
                                    && folded.getZ() <= piece.value().maxZ();
                        }

                        assertTrue(covered, testCase.name() + ": the fold of (" + x + ", " + z
                                + ") is in no piece of the split region");
                    }
                }
            }
        }

        @Test
        void regionsOverlapAgreesWithTheOrbit() {
            for (Case testCase : cases()) {
                DeckGroupFold fold = testCase.fold();
                Random random = new Random(SEED + 12);
                for (int sample = 0; sample < SAMPLES; sample++) {
                    BoundingBox first = randomRegion(random);
                    BoundingBox second = randomRegion(random);
                    assertEquals(anyCopyIntersects(testCase.group(), first, second),
                            fold.regionsOverlap(first, second),
                            testCase.name() + ": regionsOverlap disagrees with the orbit");
                }
            }
        }

        @Test
        void theDistanceToABoxIsTheNearestCopyOfThatBox() {
            for (Case testCase : cases()) {
                DeckGroupFold fold = testCase.fold();
                Random random = new Random(SEED + 13);
                for (int sample = 0; sample < SAMPLES; sample++) {
                    Vec3 point = new Vec3(sample(random) + 0.5, 64.0, sample(random) + 0.5);
                    double minX = sample(random);
                    double minZ = sample(random);
                    AABB box = new AABB(minX, 60.0, minZ, minX + 4.0, 68.0, minZ + 6.0);

                    double best = Double.MAX_VALUE;
                    for (int first = -ORBIT_REACH; first <= ORBIT_REACH; first++) {
                        for (int second = -ORBIT_REACH; second <= ORBIT_REACH; second++) {
                            double[] low = testCase.group().coord(box.minX, box.minZ, first, second);
                            double[] high = testCase.group().coord(box.maxX, box.maxZ, first, second);
                            best = Math.min(best, gapSquared(low, high, point));
                        }
                    }

                    assertEquals(best, fold.sqrDistanceToBox(box, point),
                            testCase.name() + ": the distance to the box is not the nearest copy's");
                }
            }
        }

        private static double gapSquared(double[] low, double[] high, Vec3 point) {
            double minX = Math.min(low[0], high[0]);
            double maxX = Math.max(low[0], high[0]);
            double minZ = Math.min(low[1], high[1]);
            double maxZ = Math.max(low[1], high[1]);
            double xGap = Math.max(Math.max(minX - point.x, point.x - maxX), 0.0);
            double yGap = Math.max(Math.max(60.0 - point.y, point.y - 68.0), 0.0);
            double zGap = Math.max(Math.max(minZ - point.z, point.z - maxZ), 0.0);
            return xGap * xGap + yGap * yGap + zGap * zGap;
        }

        private static boolean anyCopyIntersects(Group group, BoundingBox first, BoundingBox second) {
            if (first.minY() > second.maxY() || second.minY() > first.maxY()) {
                return false;
            }

            for (int i = -ORBIT_REACH; i <= ORBIT_REACH; i++) {
                for (int j = -ORBIT_REACH; j <= ORBIT_REACH; j++) {
                    int[] low = group.cell(second.minX(), second.minZ(), i, j);
                    int[] high = group.cell(second.maxX(), second.maxZ(), i, j);
                    int minX = Math.min(low[0], high[0]);
                    int maxX = Math.max(low[0], high[0]);
                    int minZ = Math.min(low[1], high[1]);
                    int maxZ = Math.max(low[1], high[1]);
                    if (first.minX() <= maxX && minX <= first.maxX()
                            && first.minZ() <= maxZ && minZ <= first.maxZ()) {
                        return true;
                    }
                }
            }

            return false;
        }

        private static BoundingBox randomRegion(Random random) {
            int minX = sample(random);
            int minZ = sample(random);
            return new BoundingBox(minX, 60, minZ, minX + random.nextInt(40), 68, minZ + random.nextInt(40));
        }
    }

    @Nested
    class Normalisation {
        @Test
        void theSkewIsFoldedIntoHalfTheWorld() {
            int halfWorldChunks = (MAX_CHUNK - MIN_CHUNK) / 2;
            for (int skew : new int[] {0, 3, 8, 9, 17, 1000, -1000, -9}) {
                int normalised = FlatShape.latticeTorus(BOTH, skew).skewChunks();
                assertTrue(Math.abs(normalised) <= halfWorldChunks,
                        "a skew of " + skew + " normalised to " + normalised + ", past half the world");
            }
        }

        @Test
        void aSkewOneWorldWiderIsTheSameLattice() {
            DeckGroupFold plain = new DeckGroupFold(FlatShape.latticeTorus(BOTH, SKEW_CHUNKS));
            DeckGroupFold wider = new DeckGroupFold(
                    FlatShape.latticeTorus(BOTH, SKEW_CHUNKS + (MAX_CHUNK - MIN_CHUNK)));
            assertSameFold(plain, wider, "a skew one world wider is a different lattice");
        }

        @Test
        void aMirrorLineHalfAWorldFurtherIsTheSameBottle() {
            DeckGroupFold plain = new DeckGroupFold(
                    FlatShape.mirrored(BOTH, Direction.Axis.Z, OFFSET_MIRROR_CHUNK));
            DeckGroupFold further = new DeckGroupFold(FlatShape.mirrored(
                    BOTH, Direction.Axis.Z, OFFSET_MIRROR_CHUNK + (MAX_CHUNK - MIN_CHUNK) / 2));
            assertSameFold(plain, further, "a mirror line half a world further is a different bottle");
        }

        private static void assertSameFold(DeckGroupFold first, DeckGroupFold second, String message) {
            Random random = new Random(SEED + 15);
            for (int sample = 0; sample < SAMPLES; sample++) {
                BlockPos ref = new BlockPos(sample(random), 64, sample(random));
                BlockPos target = new BlockPos(sample(random), 64, sample(random));
                assertEquals(first.fold(target), second.fold(target), message);
                assertEquals(first.nearestCopy(ref, target), second.nearestCopy(ref, target), message);
            }
        }
    }

    @Nested
    class PerAxisAgreement {
        @Test
        void theDecomposableShapesAgreeWithThePerAxisTransformer() {
            for (Case testCase : decomposableCases()) {
                DeckGroupFold generic = testCase.fold();
                WorldLoopTransformer perAxis = new WorldLoopTransformer(testCase.shape().bounds());
                Random random = new Random(SEED + 14);
                for (int sample = 0; sample < SAMPLES; sample++) {
                    BlockPos ref = new BlockPos(sample(random), 64, sample(random));
                    BlockPos target = new BlockPos(sample(random), 70, sample(random));
                    ChunkPos refChunk = new ChunkPos(Math.floorDiv(ref.getX(), UNIT), Math.floorDiv(ref.getZ(), UNIT));
                    ChunkPos targetChunk =
                            new ChunkPos(Math.floorDiv(target.getX(), UNIT), Math.floorDiv(target.getZ(), UNIT));
                    Vec3 refVector = new Vec3(ref.getX() + 0.5, 64.0, ref.getZ() + 0.5);
                    Vec3 targetVector = new Vec3(target.getX() + 0.5, 70.0, target.getZ() + 0.5);

                    assertEquals(perAxis.fold(target), generic.fold(target), testCase.name() + ": block fold");
                    assertEquals(perAxis.fold(targetChunk), generic.fold(targetChunk),
                            testCase.name() + ": chunk fold");
                    assertEquals(perAxis.fold(targetVector), generic.fold(targetVector),
                            testCase.name() + ": vector fold");
                    long targetKey = ChunkPos.asLong(targetChunk.x, targetChunk.z);
                    assertEquals(perAxis.foldChunkKey(targetKey), generic.foldChunkKey(targetKey),
                            testCase.name() + ": chunk key fold");
                    assertEquals(perAxis.nearestCopy(ref, target), generic.nearestCopy(ref, target),
                            testCase.name() + ": block nearestCopy");
                    assertEquals(perAxis.nearestCopy(refChunk, targetChunk),
                            generic.nearestCopy(refChunk, targetChunk), testCase.name() + ": chunk nearestCopy");
                    assertEquals(perAxis.nearestCopy(refVector, targetVector),
                            generic.nearestCopy(refVector, targetVector), testCase.name() + ": vector nearestCopy");
                    assertEquals(perAxis.sqrDistance(refVector, targetVector),
                            generic.sqrDistance(refVector, targetVector), testCase.name() + ": sqrDistance");
                    assertEquals(perAxis.sqrChunkDistance(refChunk, targetChunk),
                            generic.sqrChunkDistance(refChunk, targetChunk), testCase.name() + ": chunk distance");
                }
            }
        }

        @Test
        void thePerAxisViewIsOpenOnlyWhereTheShapeDecomposes() {
            for (Case testCase : decomposableCases()) {
                DeckGroupFold fold = testCase.fold();
                assertTrue(fold.decomposesPerAxis(), testCase.name() + ": should decompose per axis");
                assertSame(fold.blockDomain(Direction.Axis.X), fold.blockDomain(Direction.Axis.X),
                        testCase.name() + ": the per-axis view is not stable");
                assertThrows(IllegalArgumentException.class, () -> fold.blockDomain(Direction.Axis.Y),
                        testCase.name() + ": the contract answered for Y");
            }

            for (Case testCase : List.of(LATTICE_TORUS, MOBIUS, KLEIN_CENTRED, KLEIN_OFFSET)) {
                DeckGroupFold fold = testCase.fold();
                assertFalse(fold.decomposesPerAxis(), testCase.name() + ": should not decompose per axis");
                assertThrows(IllegalStateException.class, () -> fold.blockDomain(Direction.Axis.X),
                        testCase.name() + ": the per-axis view was open on a shape that does not decompose");
                assertThrows(IllegalStateException.class, () -> fold.chunkDomain(Direction.Axis.Z),
                        testCase.name() + ": the per-axis chunk view was open");
            }
        }

        @Test
        void localIndicesSurviveExactlyWhereThereIsNoMirror() {
            for (Case testCase : cases()) {
                assertEquals(!testCase.shape().isMirrored(), testCase.fold().preservesLocalIndices(),
                        testCase.name() + ": preservesLocalIndices disagrees with the mirror");
            }
        }
    }
}
