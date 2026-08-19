package com.toroidalworld.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

class SeamDistanceTest {
    private static final long SEED = 0xD157L;
    private static final int SAMPLES = 1500;
    private static final int LAPS = 5;

    private static final WorldLoopTransformer EVEN = transformer(-32, 32, -32, 32);
    private static final WorldLoopTransformer ODD = transformer(-2, 3, -2, 3);
    private static final WorldLoopTransformer UNEVEN = transformer(-48, 16, 0, 16);
    private static final WorldLoopTransformer X_ONLY = new WorldLoopTransformer(
            new WorldLoopBounds(new AxisBounds.Looped(-32, 32), AxisBounds.Unbounded.INSTANCE));

    private static final List<WorldLoopTransformer> TRANSFORMERS =
            List.of(EVEN, ODD, UNEVEN, X_ONLY, WorldLoopTransformer.NOOP);

    private static WorldLoopTransformer transformer(int xChunkMin, int xChunkMax, int zChunkMin, int zChunkMax) {
        return new WorldLoopTransformer(new WorldLoopBounds(xChunkMin, xChunkMax, zChunkMin, zChunkMax));
    }

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

    private static double coordSqrRef(WorldLoopTransformer transformer, double dx, double dy, double dz) {
        double best = Double.MAX_VALUE;
        for (int xLaps = -LAPS; xLaps <= LAPS; xLaps++) {
            for (int zLaps = -LAPS; zLaps <= LAPS; zLaps++) {
                double shiftedX = dx + xLaps * (double) transformer.coords.x.domainLength;
                double shiftedZ = dz + zLaps * (double) transformer.coords.z.domainLength;
                best = Math.min(best, shiftedX * shiftedX + dy * dy + shiftedZ * shiftedZ);
            }
        }
        return best;
    }

    private static long chunkSqrRef(WorldLoopTransformer transformer, int dx, int dz) {
        long best = Long.MAX_VALUE;
        for (int xLaps = -LAPS; xLaps <= LAPS; xLaps++) {
            for (int zLaps = -LAPS; zLaps <= LAPS; zLaps++) {
                long shiftedX = dx + (long) xLaps * transformer.chunks.x.domainLength;
                long shiftedZ = dz + (long) zLaps * transformer.chunks.z.domainLength;
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

    private static double boxSqrRef(WorldLoopTransformer transformer, AABB box, Vec3 point) {
        double best = Double.MAX_VALUE;
        for (int xLaps = -LAPS; xLaps <= LAPS; xLaps++) {
            for (int zLaps = -LAPS; zLaps <= LAPS; zLaps++) {
                AABB copy = box.move(
                        xLaps * (double) transformer.coords.x.domainLength,
                        0.0,
                        zLaps * (double) transformer.coords.z.domainLength);
                best = Math.min(best, flatBoxSqr(copy, point));
            }
        }
        return best;
    }

    private static String in(WorldLoopTransformer transformer) {
        return "in " + transformer;
    }

    @Nested
    class PointDistance {
        @Test
        void coordSqrDistIsTheMinimumOverWorldCopies() {
            Random random = new Random(SEED);
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    double xFrom = transformer.coords.x.wrap(sampleBlock(random, transformer.coords.x));
                    double yFrom = sampleY(random);
                    double zFrom = transformer.coords.z.wrap(sampleBlock(random, transformer.coords.z));
                    double xTo = transformer.coords.x.wrap(sampleBlock(random, transformer.coords.x));
                    double yTo = sampleY(random);
                    double zTo = transformer.coords.z.wrap(sampleBlock(random, transformer.coords.z));

                    double expected = coordSqrRef(transformer, xTo - xFrom, yTo - yFrom, zTo - zFrom);
                    double actual = transformer.coords.sqrDistToBounds(xFrom, yFrom, zFrom, xTo, yTo, zTo);
                    assertEquals(expected, actual, 1e-3, () -> "Coord.sqrDistToBounds " + in(transformer));
                }
            }
        }

        @Test
        void chunkSqrDistMatchesTheLatticeAndEveryOverloadAgrees() {
            Random random = new Random(SEED);
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    int xFrom = transformer.chunks.x.wrap(sampleChunk(random, transformer.chunks.x));
                    int zFrom = transformer.chunks.z.wrap(sampleChunk(random, transformer.chunks.z));
                    int xTo = transformer.chunks.x.wrap(sampleChunk(random, transformer.chunks.x));
                    int zTo = transformer.chunks.z.wrap(sampleChunk(random, transformer.chunks.z));

                    long expected = chunkSqrRef(transformer, xTo - xFrom, zTo - zFrom);
                    int actual = transformer.chunks.sqrDistToBounds(xFrom, zFrom, xTo, zTo);
                    assertEquals(expected, actual, () -> "Chunk.sqrDistToBounds(ints) " + in(transformer));

                    ChunkPos from = new ChunkPos(xFrom, zFrom);
                    ChunkPos to = new ChunkPos(xTo, zTo);
                    assertEquals(actual, transformer.chunks.sqrDistToBounds(from, to),
                            () -> "Chunk.sqrDistToBounds(ChunkPos) " + in(transformer));
                    assertEquals(actual, transformer.chunks.sqrDistToBounds(ChunkPos.pack(xFrom, zFrom), ChunkPos.pack(xTo, zTo)),
                            () -> "Chunk.sqrDistToBounds(packed) " + in(transformer));
                }
            }
        }

        @Test
        void chessboardDistanceIsTheMinimumOverWorldCopiesHoweverFarOutTheTarget() {
            Random random = new Random(SEED);
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    ChunkPos from = new ChunkPos(
                            transformer.chunks.x.wrap(sampleChunk(random, transformer.chunks.x)),
                            transformer.chunks.z.wrap(sampleChunk(random, transformer.chunks.z)));
                    ChunkPos to = new ChunkPos(
                            sampleChunk(random, transformer.chunks.x),
                            sampleChunk(random, transformer.chunks.z));

                    int wrappedX = transformer.chunks.x.wrap(to.x());
                    int wrappedZ = transformer.chunks.z.wrap(to.z());
                    int expected = Integer.MAX_VALUE;
                    for (int xLaps = -1; xLaps <= 1; xLaps++) {
                        for (int zLaps = -1; zLaps <= 1; zLaps++) {
                            int dx = Math.abs(wrappedX + xLaps * transformer.chunks.x.domainLength - from.x());
                            int dz = Math.abs(wrappedZ + zLaps * transformer.chunks.z.domainLength - from.z());
                            expected = Math.min(expected, Math.max(dx, dz));
                        }
                    }

                    int finalExpected = expected;
                    assertEquals(expected, transformer.chunks.chessboardDistance(from, to),
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
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    int x = sampleChunk(random, transformer.chunks.x);
                    int z = sampleChunk(random, transformer.chunks.z);
                    int expected = Math.max(
                            Math.abs(x - refClamp(transformer.chunks.x, x)),
                            Math.abs(z - refClamp(transformer.chunks.z, z)));

                    assertEquals(expected, transformer.chunks.overshoot(x, z),
                            () -> "overshoot(" + x + ", " + z + ") " + in(transformer));
                }
            }
        }

        @Test
        void insideTheWorldOvershootIsZero() {
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
                assertEquals(0, transformer.chunks.overshoot(0, 0), () -> in(transformer));
                assertEquals(0, transformer.chunks.overshoot(
                        transformer.chunks.x.lowerBound, transformer.chunks.z.upperBound - 1), () -> in(transformer));
            }
        }
    }

    @Nested
    class NearestCopy {
        @Test
        void landsOnTheSamePositionNoFurtherThanAnyOtherCopyAndWithinHalfAWorld() {
            Random random = new Random(SEED);
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    Vec3 ref = new Vec3(
                            sampleBlock(random, transformer.coords.x),
                            sampleY(random),
                            sampleBlock(random, transformer.coords.z));
                    Vec3 target = new Vec3(
                            sampleBlock(random, transformer.coords.x),
                            sampleY(random),
                            sampleBlock(random, transformer.coords.z));

                    Vec3 nearest = transformer.vectors.nearestCopy(ref, target);

                    assertEquals(target.y, nearest.y, 0.0, () -> "Y is seamless " + in(transformer));
                    assertEquals(transformer.coords.x.wrap(target.x), transformer.coords.x.wrap(nearest.x), 1e-6,
                            () -> "nearestCopy moved the X position " + in(transformer));
                    assertEquals(transformer.coords.z.wrap(target.z), transformer.coords.z.wrap(nearest.z), 1e-6,
                            () -> "nearestCopy moved the Z position " + in(transformer));

                    double expected = coordSqrRef(transformer,
                            transformer.coords.x.wrap(target.x) - ref.x, 0.0,
                            transformer.coords.z.wrap(target.z) - ref.z);
                    double actual = (nearest.x - ref.x) * (nearest.x - ref.x)
                            + (nearest.z - ref.z) * (nearest.z - ref.z);
                    assertEquals(expected, actual, 1e-3,
                            () -> "nearestCopy(" + ref + ", " + target + ") is not the nearest copy " + in(transformer));

                    if (!(transformer.coords.x instanceof WrapDomain.Noop)) {
                        assertTrue(Math.abs(nearest.x - ref.x) <= transformer.coords.x.domainLength / 2.0 + 1e-9,
                                () -> "nearestCopy is over half a world away on X " + in(transformer));
                    }
                    if (!(transformer.coords.z instanceof WrapDomain.Noop)) {
                        assertTrue(Math.abs(nearest.z - ref.z) <= transformer.coords.z.domainLength / 2.0 + 1e-9,
                                () -> "nearestCopy is over half a world away on Z " + in(transformer));
                    }
                }
            }
        }

        @Test
        void blocksLandOnTheSameBlockNoFurtherThanAnyOtherCopyAndWithinHalfAWorld() {
            Random random = new Random(SEED);
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    BlockPos ref = new BlockPos(
                            sampleBlockCoord(random, transformer.coords.x),
                            sampleYBlock(random),
                            sampleBlockCoord(random, transformer.coords.z));
                    BlockPos target = new BlockPos(
                            sampleBlockCoord(random, transformer.coords.x),
                            sampleYBlock(random),
                            sampleBlockCoord(random, transformer.coords.z));

                    BlockPos nearest = transformer.blocks.nearestCopy(ref, target);

                    assertEquals(target.getY(), nearest.getY(), () -> "Y is seamless " + in(transformer));
                    assertEquals(transformer.coords.x.wrap(target.getX()), transformer.coords.x.wrap(nearest.getX()),
                            () -> "nearestCopy moved the X block " + in(transformer));
                    assertEquals(transformer.coords.z.wrap(target.getZ()), transformer.coords.z.wrap(nearest.getZ()),
                            () -> "nearestCopy moved the Z block " + in(transformer));

                    double expected = coordSqrRef(transformer,
                            transformer.coords.x.wrap(target.getX()) - (double) ref.getX(), 0.0,
                            transformer.coords.z.wrap(target.getZ()) - (double) ref.getZ());
                    double dx = nearest.getX() - (double) ref.getX();
                    double dz = nearest.getZ() - (double) ref.getZ();
                    assertEquals(expected, dx * dx + dz * dz, 1e-3,
                            () -> "nearestCopy(" + ref + ", " + target + ") is not the nearest copy " + in(transformer));

                    if (!(transformer.coords.x instanceof WrapDomain.Noop)) {
                        assertTrue(Math.abs(dx) <= transformer.coords.x.domainLength / 2.0,
                                () -> "nearestCopy is over half a world away on X " + in(transformer));
                    }
                    if (!(transformer.coords.z instanceof WrapDomain.Noop)) {
                        assertTrue(Math.abs(dz) <= transformer.coords.z.domainLength / 2.0,
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
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    double minX = transformer.coords.x.wrap(sampleBlock(random, transformer.coords.x));
                    double minY = sampleY(random);
                    double minZ = transformer.coords.z.wrap(sampleBlock(random, transformer.coords.z));
                    AABB box = new AABB(minX, minY, minZ,
                            minX + random.nextDouble() * 8, minY + random.nextDouble() * 8, minZ + random.nextDouble() * 8);
                    Vec3 point = new Vec3(
                            transformer.coords.x.wrap(sampleBlock(random, transformer.coords.x)),
                            sampleY(random),
                            transformer.coords.z.wrap(sampleBlock(random, transformer.coords.z)));

                    assertEquals(boxSqrRef(transformer, box, point),
                            transformer.distanceToSqrWrappedCoord(box, point), 1e-3,
                            () -> "distanceToSqrWrappedCoord(" + box + ", " + point + ") " + in(transformer));
                }
            }
        }

        @Test
        void aBlockJustAcrossTheSeamIsAStepAwayNotAWorldAway() {
            AABB box = new AABB(-512, 0, 0, -502, 10, 10);
            Vec3 point = new Vec3(500, 5, 5);
            assertEquals(144, EVEN.distanceToSqrWrappedCoord(box, point), 1e-9);
        }

        @Test
        void aPointInsideTheBoxIsAtDistanceZero() {
            AABB box = new AABB(-5, 0, -5, 5, 10, 5);
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
                assertEquals(0.0, transformer.distanceToSqrWrappedCoord(box, new Vec3(0, 5, 0)), 0.0,
                        () -> in(transformer));
            }
        }
    }

    @Nested
    class FoldBox {
        @Test
        void movesByWholeWorldWidthsToTheCopyNearestTheReference() {
            Random random = new Random(SEED);
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    double minX = sampleBlock(random, transformer.coords.x);
                    double minY = sampleY(random);
                    double minZ = sampleBlock(random, transformer.coords.z);
                    AABB box = new AABB(minX, minY, minZ,
                            minX + random.nextDouble() * 8, minY + 1, minZ + random.nextDouble() * 8);
                    Vec3 ref = new Vec3(
                            transformer.coords.x.wrap(sampleBlock(random, transformer.coords.x)),
                            sampleY(random),
                            transformer.coords.z.wrap(sampleBlock(random, transformer.coords.z)));

                    AABB folded = transformer.foldBoxToward(ref, box);

                    assertEquals(box.maxX - box.minX, folded.maxX - folded.minX, 1e-9, () -> "X size " + in(transformer));
                    assertEquals(box.maxZ - box.minZ, folded.maxZ - folded.minZ, 1e-9, () -> "Z size " + in(transformer));
                    assertEquals(box.minY, folded.minY, 0.0, () -> "Y floor " + in(transformer));
                    assertEquals(box.maxY, folded.maxY, 0.0, () -> "Y ceiling " + in(transformer));

                    checkShiftOnLattice(transformer.coords.x, box.minX, folded.minX, "X", transformer);
                    checkShiftOnLattice(transformer.coords.z, box.minZ, folded.minZ, "Z", transformer);

                    double centerX = (box.minX + box.maxX) / 2.0;
                    double centerZ = (box.minZ + box.maxZ) / 2.0;
                    double foldedCenterX = (folded.minX + folded.maxX) / 2.0;
                    double foldedCenterZ = (folded.minZ + folded.maxZ) / 2.0;
                    double expected = coordSqrRef(transformer,
                            transformer.coords.x.wrap(centerX) - ref.x, 0.0,
                            transformer.coords.z.wrap(centerZ) - ref.z);
                    double actual = (foldedCenterX - ref.x) * (foldedCenterX - ref.x)
                            + (foldedCenterZ - ref.z) * (foldedCenterZ - ref.z);
                    assertEquals(expected, actual, 1e-3,
                            () -> "foldBoxToward did not pick the nearest copy " + in(transformer));
                }
            }
        }

        private void checkShiftOnLattice(WrapDomain domain, double boxMin, double foldedMin,
                String axis, WorldLoopTransformer transformer) {
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
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
                assertSame(box, transformer.foldBoxToward(ref, box), () -> in(transformer));
            }
        }
    }

    @Nested
    class DisabledEverywhere {
        private final WorldLoopTransformer disabled = WorldLoopTransformer.NOOP;

        @Test
        void everyDistanceIsThePlainOne() {
            Random random = new Random(SEED);
            for (int i = 0; i < SAMPLES; i++) {
                double dx = random.nextInt(20_001) - 10_000 + random.nextDouble();
                double dy = sampleY(random);
                double dz = random.nextInt(20_001) - 10_000 + random.nextDouble();
                assertEquals(dx * dx + dy * dy + dz * dz,
                        disabled.coords.sqrDistToBounds(0, 0, 0, dx, dy, dz), 1e-3);

                ChunkPos from = new ChunkPos(random.nextInt(2_001) - 1_000, random.nextInt(2_001) - 1_000);
                ChunkPos to = new ChunkPos(random.nextInt(2_001) - 1_000, random.nextInt(2_001) - 1_000);
                long flatDx = to.x() - from.x();
                long flatDz = to.z() - from.z();
                assertEquals(flatDx * flatDx + flatDz * flatDz, disabled.chunks.sqrDistToBounds(from, to));
                assertEquals(from.getChessboardDistance(to), disabled.chunks.chessboardDistance(from, to));

                Vec3 ref = new Vec3(dx, dy, dz);
                Vec3 target = new Vec3(dz, dy, dx);
                assertEquals(target, disabled.vectors.nearestCopy(ref, target));

                AABB box = new AABB(dx, dy, dz, dx + 3, dy + 3, dz + 3);
                assertEquals(flatBoxSqr(box, ref), disabled.distanceToSqrWrappedCoord(box, ref), 1e-9);
                assertSame(box, disabled.foldBoxToward(ref, box));
            }
        }
    }
}
