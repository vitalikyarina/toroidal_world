package com.toroidalworld.core;

import static com.toroidalworld.core.WorldFoldFixture.EVEN;
import static com.toroidalworld.core.WorldFoldFixture.PER_AXIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

class SeamDistanceTest {
    private static final long SEED = 0xD157L;
    private static final int SAMPLES = 1500;
    private static final int LAPS = 5;

    private static final List<WorldFold> TRANSFORMERS = PER_AXIS;

    private static int sampleChunk(Random random, WrapDomain domain) {
        int reach = 3 * (domain instanceof WrapDomain.Noop ? 1_000 : Math.min(domain.domainLength, 1_000));
        return random.nextInt(2 * reach + 1) - reach;
    }

    private static double sampleBlock(Random random, WrapDomain domain) {
        int reach = 3 * (domain instanceof WrapDomain.Noop ? 16_000 : Math.min(domain.domainLength, 16_000));
        return random.nextInt(2 * reach + 1) - reach + random.nextDouble();
    }

    private static int sampleBlockCoord(Random random, WrapDomain domain) {
        return (int) Math.floor(sampleBlock(random, domain));
    }

    private static double sampleY(Random random) {
        return random.nextInt(384) - 64 + random.nextDouble();
    }

    private static int sampleYBlock(Random random) {
        return random.nextInt(384) - 64;
    }

    private static double coordSqrRef(WorldFold transformer, double dx, double dy, double dz) {
        double best = Double.MAX_VALUE;
        for (int xLaps = -LAPS; xLaps <= LAPS; xLaps++) {
            for (int zLaps = -LAPS; zLaps <= LAPS; zLaps++) {
                double shiftedX = dx + xLaps * (double) blockX(transformer).domainLength;
                double shiftedZ = dz + zLaps * (double) blockZ(transformer).domainLength;
                best = Math.min(best, shiftedX * shiftedX + dy * dy + shiftedZ * shiftedZ);
            }
        }
        return best;
    }

    private static long chunkSqrRef(WorldFold transformer, int dx, int dz) {
        long best = Long.MAX_VALUE;
        for (int xLaps = -LAPS; xLaps <= LAPS; xLaps++) {
            for (int zLaps = -LAPS; zLaps <= LAPS; zLaps++) {
                long shiftedX = dx + (long) xLaps * chunkX(transformer).domainLength;
                long shiftedZ = dz + (long) zLaps * chunkZ(transformer).domainLength;
                best = Math.min(best, shiftedX * shiftedX + shiftedZ * shiftedZ);
            }
        }
        return best;
    }

    private static double flatBoxSqr(AABB box, Vec3 point) {
        double xGap = Math.max(Math.max(box.minX - point.x, point.x - box.maxX), 0.0);
        double yGap = Math.max(Math.max(box.minY - point.y, point.y - box.maxY), 0.0);
        double zGap = Math.max(Math.max(box.minZ - point.z, point.z - box.maxZ), 0.0);
        return xGap * xGap + yGap * yGap + zGap * zGap;
    }

    private static double boxSqrRef(WorldFold transformer, AABB box, Vec3 point) {
        double best = Double.MAX_VALUE;
        for (int xLaps = -LAPS; xLaps <= LAPS; xLaps++) {
            for (int zLaps = -LAPS; zLaps <= LAPS; zLaps++) {
                AABB copy = box.move(
                        xLaps * (double) blockX(transformer).domainLength,
                        0.0,
                        zLaps * (double) blockZ(transformer).domainLength);
                best = Math.min(best, flatBoxSqr(copy, point));
            }
        }
        return best;
    }

    private static String in(WorldFold transformer) {
        return "in " + transformer;
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

    @Nested
    class PointDistance {
        @Test
        void coordSqrDistIsTheMinimumOverWorldCopies() {
            Random random = new Random(SEED);
            for (WorldFold transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    double xFrom = blockX(transformer).wrap(sampleBlock(random, blockX(transformer)));
                    double yFrom = sampleY(random);
                    double zFrom = blockZ(transformer).wrap(sampleBlock(random, blockZ(transformer)));
                    double xTo = blockX(transformer).wrap(sampleBlock(random, blockX(transformer)));
                    double yTo = sampleY(random);
                    double zTo = blockZ(transformer).wrap(sampleBlock(random, blockZ(transformer)));

                    double expected = coordSqrRef(transformer, xTo - xFrom, yTo - yFrom, zTo - zFrom);
                    double actual = transformer.sqrDistance(xFrom, yFrom, zFrom, xTo, yTo, zTo);
                    assertEquals(expected, actual, 1e-3, () -> "Coord.sqrDistToBounds " + in(transformer));
                }
            }
        }

        @Test
        void chunkSqrDistMatchesTheLattice() {
            Random random = new Random(SEED);
            for (WorldFold transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    int xFrom = chunkX(transformer).wrap(sampleChunk(random, chunkX(transformer)));
                    int zFrom = chunkZ(transformer).wrap(sampleChunk(random, chunkZ(transformer)));
                    int xTo = chunkX(transformer).wrap(sampleChunk(random, chunkX(transformer)));
                    int zTo = chunkZ(transformer).wrap(sampleChunk(random, chunkZ(transformer)));

                    long expected = chunkSqrRef(transformer, xTo - xFrom, zTo - zFrom);
                    int actual = transformer.sqrChunkDistance(new ChunkPos(xFrom, zFrom), new ChunkPos(xTo, zTo));
                    assertEquals(expected, actual, () -> "sqrChunkDistance " + in(transformer));
                }
            }
        }

        @Test
        void chessboardDistanceIsTheMinimumOverWorldCopiesHoweverFarOutTheTarget() {
            Random random = new Random(SEED);
            for (WorldFold transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    ChunkPos from = new ChunkPos(
                            chunkX(transformer).wrap(sampleChunk(random, chunkX(transformer))),
                            chunkZ(transformer).wrap(sampleChunk(random, chunkZ(transformer))));
                    ChunkPos to = new ChunkPos(
                            sampleChunk(random, chunkX(transformer)),
                            sampleChunk(random, chunkZ(transformer)));

                    int wrappedX = chunkX(transformer).wrap(to.x);
                    int wrappedZ = chunkZ(transformer).wrap(to.z);
                    int expected = Integer.MAX_VALUE;
                    for (int xLaps = -1; xLaps <= 1; xLaps++) {
                        for (int zLaps = -1; zLaps <= 1; zLaps++) {
                            int dx = Math.abs(wrappedX + xLaps * chunkX(transformer).domainLength - from.x);
                            int dz = Math.abs(wrappedZ + zLaps * chunkZ(transformer).domainLength - from.z);
                            expected = Math.min(expected, Math.max(dx, dz));
                        }
                    }

                    int finalExpected = expected;
                    assertEquals(expected, from.getChessboardDistance(transformer.nearestCopy(from, to)),
                            () -> "chessboardDistance(" + from + ", " + to + ") should be " + finalExpected + " "
                                    + in(transformer));
                }
            }
        }
    }

    private static int refClamp(WrapDomain domain, int coord) {
        return domain instanceof WrapDomain.Noop ? coord
                : Math.min(Math.max(coord, domain.lowerBound), domain.upperBound - 1);
    }

    @Nested
    class Overshoot {
        @Test
        void overshootIsTheChessboardDistanceToTheNearestChunkStillInside() {
            Random random = new Random(SEED);
            for (WorldFold transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    int x = sampleChunk(random, chunkX(transformer));
                    int z = sampleChunk(random, chunkZ(transformer));
                    int expected = Math.max(
                            Math.abs(x - refClamp(chunkX(transformer), x)),
                            Math.abs(z - refClamp(chunkZ(transformer), z)));

                    assertEquals(expected, transformer.chunkOvershoot(new ChunkPos(x, z)),
                            () -> "overshoot(" + x + ", " + z + ") " + in(transformer));
                }
            }
        }

        @Test
        void insideTheWorldOvershootIsZero() {
            for (WorldFold transformer : TRANSFORMERS) {
                assertEquals(0, transformer.chunkOvershoot(new ChunkPos(0, 0)), () -> in(transformer));
                assertEquals(0, transformer.chunkOvershoot(new ChunkPos(
                        chunkX(transformer).lowerBound,
                        chunkZ(transformer).upperBound - 1)), () -> in(transformer));
            }
        }
    }

    @Nested
    class NearestCopy {
        @Test
        void landsOnTheSamePositionNoFurtherThanAnyOtherCopyAndWithinHalfAWorld() {
            Random random = new Random(SEED);
            for (WorldFold transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    Vec3 ref = new Vec3(
                            sampleBlock(random, blockX(transformer)),
                            sampleY(random),
                            sampleBlock(random, blockZ(transformer)));
                    Vec3 target = new Vec3(
                            sampleBlock(random, blockX(transformer)),
                            sampleY(random),
                            sampleBlock(random, blockZ(transformer)));

                    Vec3 nearest = transformer.nearestCopy(ref, target);

                    assertEquals(target.y, nearest.y, 0.0, () -> "Y is seamless " + in(transformer));
                    assertEquals(blockX(transformer).wrap(target.x), blockX(transformer).wrap(nearest.x), 1e-6,
                            () -> "nearestCopy moved the X position " + in(transformer));
                    assertEquals(blockZ(transformer).wrap(target.z), blockZ(transformer).wrap(nearest.z), 1e-6,
                            () -> "nearestCopy moved the Z position " + in(transformer));

                    double expected = coordSqrRef(transformer,
                            blockX(transformer).wrap(target.x) - ref.x, 0.0,
                            blockZ(transformer).wrap(target.z) - ref.z);
                    double actual = (nearest.x - ref.x) * (nearest.x - ref.x)
                            + (nearest.z - ref.z) * (nearest.z - ref.z);
                    assertEquals(expected, actual, 1e-3,
                            () -> "nearestCopy(" + ref + ", " + target + ") is not the nearest copy " + in(transformer));

                    if (!(blockX(transformer) instanceof WrapDomain.Noop)) {
                        assertTrue(Math.abs(nearest.x - ref.x) <= blockX(transformer).domainLength / 2.0 + 1e-9,
                                () -> "nearestCopy is over half a world away on X " + in(transformer));
                    }
                    if (!(blockZ(transformer) instanceof WrapDomain.Noop)) {
                        assertTrue(Math.abs(nearest.z - ref.z) <= blockZ(transformer).domainLength / 2.0 + 1e-9,
                                () -> "nearestCopy is over half a world away on Z " + in(transformer));
                    }
                }
            }
        }

        @Test
        void blocksLandOnTheSameBlockNoFurtherThanAnyOtherCopyAndWithinHalfAWorld() {
            Random random = new Random(SEED);
            for (WorldFold transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    BlockPos ref = new BlockPos(
                            sampleBlockCoord(random, blockX(transformer)),
                            sampleYBlock(random),
                            sampleBlockCoord(random, blockZ(transformer)));
                    BlockPos target = new BlockPos(
                            sampleBlockCoord(random, blockX(transformer)),
                            sampleYBlock(random),
                            sampleBlockCoord(random, blockZ(transformer)));

                    BlockPos nearest = transformer.nearestCopy(ref, target);

                    assertEquals(target.getY(), nearest.getY(), () -> "Y is seamless " + in(transformer));
                    assertEquals(blockX(transformer).wrap(target.getX()), blockX(transformer).wrap(nearest.getX()),
                            () -> "nearestCopy moved the X block " + in(transformer));
                    assertEquals(blockZ(transformer).wrap(target.getZ()), blockZ(transformer).wrap(nearest.getZ()),
                            () -> "nearestCopy moved the Z block " + in(transformer));

                    double expected = coordSqrRef(transformer,
                            blockX(transformer).wrap(target.getX()) - (double) ref.getX(), 0.0,
                            blockZ(transformer).wrap(target.getZ()) - (double) ref.getZ());
                    double dx = nearest.getX() - (double) ref.getX();
                    double dz = nearest.getZ() - (double) ref.getZ();
                    assertEquals(expected, dx * dx + dz * dz, 1e-3,
                            () -> "nearestCopy(" + ref + ", " + target + ") is not the nearest copy " + in(transformer));

                    if (!(blockX(transformer) instanceof WrapDomain.Noop)) {
                        assertTrue(Math.abs(dx) <= blockX(transformer).domainLength / 2.0,
                                () -> "nearestCopy is over half a world away on X " + in(transformer));
                    }
                    if (!(blockZ(transformer) instanceof WrapDomain.Noop)) {
                        assertTrue(Math.abs(dz) <= blockZ(transformer).domainLength / 2.0,
                                () -> "nearestCopy is over half a world away on Z " + in(transformer));
                    }
                }
            }
        }
    }

    @Nested
    class BoxDistance {
        @Test
        void distanceToSqrWrappedCoordMatchesTheNearestBoxCopy() {
            Random random = new Random(SEED);
            for (WorldFold transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    double minX = blockX(transformer).wrap(sampleBlock(random, blockX(transformer)));
                    double minY = sampleY(random);
                    double minZ = blockZ(transformer).wrap(sampleBlock(random, blockZ(transformer)));
                    AABB box = new AABB(minX, minY, minZ,
                            minX + random.nextDouble() * 8, minY + random.nextDouble() * 8, minZ + random.nextDouble() * 8);
                    Vec3 point = new Vec3(
                            blockX(transformer).wrap(sampleBlock(random, blockX(transformer))),
                            sampleY(random),
                            blockZ(transformer).wrap(sampleBlock(random, blockZ(transformer))));

                    assertEquals(boxSqrRef(transformer, box, point),
                            transformer.sqrDistanceToBox(box, point), 1e-3,
                            () -> "distanceToSqrWrappedCoord(" + box + ", " + point + ") " + in(transformer));
                }
            }
        }

        @Test
        void aBlockJustAcrossTheSeamIsAStepAwayNotAWorldAway() {
            AABB box = new AABB(-512, 0, 0, -502, 10, 10);
            Vec3 point = new Vec3(500, 5, 5);
            assertEquals(144, EVEN.sqrDistanceToBox(box, point), 1e-9);
        }

        @Test
        void aPointInsideTheBoxIsAtDistanceZero() {
            AABB box = new AABB(-5, 0, -5, 5, 10, 5);
            for (WorldFold transformer : TRANSFORMERS) {
                assertEquals(0.0, transformer.sqrDistanceToBox(box, new Vec3(0, 5, 0)), 0.0,
                        () -> in(transformer));
            }
        }
    }

    @Nested
    class FoldBox {
        @Test
        void movesByWholeWorldWidthsToTheCopyNearestTheReference() {
            Random random = new Random(SEED);
            for (WorldFold transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    double minX = sampleBlock(random, blockX(transformer));
                    double minY = sampleY(random);
                    double minZ = sampleBlock(random, blockZ(transformer));
                    AABB box = new AABB(minX, minY, minZ,
                            minX + random.nextDouble() * 8, minY + 1, minZ + random.nextDouble() * 8);
                    Vec3 ref = new Vec3(
                            blockX(transformer).wrap(sampleBlock(random, blockX(transformer))),
                            sampleY(random),
                            blockZ(transformer).wrap(sampleBlock(random, blockZ(transformer))));

                    AABB folded = transformer.foldBox(ref, box).value();

                    assertEquals(box.maxX - box.minX, folded.maxX - folded.minX, 1e-9, () -> "X size " + in(transformer));
                    assertEquals(box.maxZ - box.minZ, folded.maxZ - folded.minZ, 1e-9, () -> "Z size " + in(transformer));
                    assertEquals(box.minY, folded.minY, 0.0, () -> "Y floor " + in(transformer));
                    assertEquals(box.maxY, folded.maxY, 0.0, () -> "Y ceiling " + in(transformer));

                    checkShiftOnLattice(blockX(transformer), box.minX, folded.minX, "X", transformer);
                    checkShiftOnLattice(blockZ(transformer), box.minZ, folded.minZ, "Z", transformer);

                    double centerX = (box.minX + box.maxX) / 2.0;
                    double centerZ = (box.minZ + box.maxZ) / 2.0;
                    double foldedCenterX = (folded.minX + folded.maxX) / 2.0;
                    double foldedCenterZ = (folded.minZ + folded.maxZ) / 2.0;
                    double expected = coordSqrRef(transformer,
                            blockX(transformer).wrap(centerX) - ref.x, 0.0,
                            blockZ(transformer).wrap(centerZ) - ref.z);
                    double actual = (foldedCenterX - ref.x) * (foldedCenterX - ref.x)
                            + (foldedCenterZ - ref.z) * (foldedCenterZ - ref.z);
                    assertEquals(expected, actual, 1e-3,
                            () -> "foldBoxToward did not pick the nearest copy " + in(transformer));
                }
            }
        }

        private void checkShiftOnLattice(WrapDomain domain, double boxMin, double foldedMin,
                String axis, WorldFold transformer) {
            if (domain instanceof WrapDomain.Noop) {
                assertEquals(boxMin, foldedMin, 0.0,
                        () -> axis + " shift moved an unbounded axis " + in(transformer));
                return;
            }

            double shiftLaps = (foldedMin - boxMin) / domain.domainLength;
            assertEquals(Math.round(shiftLaps), shiftLaps, 1e-9,
                    () -> axis + " shift left its lattice " + in(transformer));
        }

        @Test
        void aBoxAlreadyNearestComesBackAsTheSameInstance() {
            AABB box = new AABB(3, 0, 3, 6, 2, 6);
            Vec3 ref = new Vec3(0, 1, 0);
            for (WorldFold transformer : TRANSFORMERS) {
                assertSame(box, transformer.foldBox(ref, box).value(), () -> in(transformer));
            }
        }
    }

    @Nested
    class DisabledEverywhere {
        private final WorldFold disabled = WorldFolds.NOOP;

        @Test
        void everyDistanceIsThePlainOne() {
            Random random = new Random(SEED);
            for (int i = 0; i < SAMPLES; i++) {
                double dx = random.nextInt(20_001) - 10_000 + random.nextDouble();
                double dy = sampleY(random);
                double dz = random.nextInt(20_001) - 10_000 + random.nextDouble();
                assertEquals(dx * dx + dy * dy + dz * dz,
                        disabled.sqrDistance(0, 0, 0, dx, dy, dz), 1e-3);

                ChunkPos from = new ChunkPos(random.nextInt(2_001) - 1_000, random.nextInt(2_001) - 1_000);
                ChunkPos to = new ChunkPos(random.nextInt(2_001) - 1_000, random.nextInt(2_001) - 1_000);
                long flatDx = to.x - from.x;
                long flatDz = to.z - from.z;
                assertEquals(flatDx * flatDx + flatDz * flatDz, disabled.sqrChunkDistance(from, to));
                assertEquals(from.getChessboardDistance(to), from.getChessboardDistance(disabled.nearestCopy(from, to)));

                Vec3 ref = new Vec3(dx, dy, dz);
                Vec3 target = new Vec3(dz, dy, dx);
                assertEquals(target, disabled.nearestCopy(ref, target));

                AABB box = new AABB(dx, dy, dz, dx + 3, dy + 3, dz + 3);
                assertEquals(flatBoxSqr(box, ref), disabled.sqrDistanceToBox(box, ref), 1e-9);
                assertSame(box, disabled.foldBox(ref, box).value());
            }
        }
    }
}
