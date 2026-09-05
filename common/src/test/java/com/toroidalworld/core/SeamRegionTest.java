package com.toroidalworld.core;

import static com.toroidalworld.core.WorldFoldFixture.EVEN;
import static com.toroidalworld.core.WorldFoldFixture.PER_AXIS;
import static com.toroidalworld.core.WorldFoldFixture.X_ONLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

class SeamRegionTest {
    private static final long SEED = 0xB0C5L;
    private static final int SAMPLES = 800;
    private static final int LAPS = 16;

    private static final List<WorldFold> TRANSFORMERS = PER_AXIS;

    private static int reachCap(WrapDomain domain, int cap) {
        return domain instanceof WrapDomain.Noop ? cap : Math.min(domain.domainLength, cap);
    }

    private static int sampleBlockInt(Random random, WrapDomain domain) {
        int reach = 3 * reachCap(domain, 16_000);
        return random.nextInt(2 * reach + 1) - reach;
    }

    private static double sampleBlock(Random random, WrapDomain domain) {
        return sampleBlockInt(random, domain) + random.nextDouble();
    }

    private static int sampleChunk(Random random, WrapDomain domain) {
        int reach = 3 * reachCap(domain, 1_000);
        return random.nextInt(2 * reach + 1) - reach;
    }

    private static double sampleY(Random random) {
        return random.nextInt(384) - 64 + random.nextDouble();
    }

    private static boolean containsOnLattice(WrapDomain domain, double min, double max, double coord) {
        for (int laps = -LAPS; laps <= LAPS; laps++) {
            double shifted = coord + laps * (double) domain.domainLength;
            if (shifted >= min - 1e-9 && shifted <= max + 1e-9) return true;
        }
        return false;
    }

    private static boolean containsOnLattice(WrapDomain domain, long min, long max, long coord) {
        for (int laps = -LAPS; laps <= LAPS; laps++) {
            long shifted = coord + (long) laps * domain.domainLength;
            if (shifted >= min && shifted <= max) return true;
        }
        return false;
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

    private static List<AABB> splitAcrossBounds(WorldFold transformer, AABB box) {
        return transformer.split(box).stream().map(WorldFold.Folded::value).toList();
    }

    private static List<BoundingBox> splitAcrossBounds(WorldFold transformer, BoundingBox region) {
        return transformer.split(region).stream().map(WorldFold.Folded::value).toList();
    }

    @Nested
    class SplitAcrossBounds {
        private AABB sampleBox(Random random, WorldFold transformer) {
            double minX = sampleBlock(random, blockX(transformer));
            double minY = sampleY(random);
            double minZ = sampleBlock(random, blockZ(transformer));
            double sizeX = random.nextDouble() * 2.2 * reachCap(blockX(transformer), 16_000);
            double sizeZ = random.nextDouble() * 2.2 * reachCap(blockZ(transformer), 16_000);
            return new AABB(minX, minY, minZ, minX + sizeX, minY + random.nextDouble() * 16, minZ + sizeZ);
        }

        private double coveredSize(WrapDomain domain, double size) {
            return domain instanceof WrapDomain.Noop ? size : Math.min(size, domain.domainLength);
        }

        @Test
        void partsCoverTheBoxVolumeCappedAtOneWorldPerAxis() {
            Random random = new Random(SEED);
            for (WorldFold transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    AABB box = sampleBox(random, transformer);
                    List<AABB> parts = splitAcrossBounds(transformer, box);

                    double expected = coveredSize(blockX(transformer), box.maxX - box.minX)
                            * (box.maxY - box.minY)
                            * coveredSize(blockZ(transformer), box.maxZ - box.minZ);
                    double total = 0;
                    for (AABB part : parts) {
                        total += (part.maxX - part.minX) * (part.maxY - part.minY) * (part.maxZ - part.minZ);
                    }
                    assertEquals(expected, total, 0.5,
                            () -> "splitAcrossBounds(" + box + ") volumes " + in(transformer));
                }
            }
        }

        @Test
        void everyPartLiesInsideTheBoundsAndKeepsTheBoxHeight() {
            Random random = new Random(SEED);
            for (WorldFold transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    AABB box = sampleBox(random, transformer);
                    for (AABB part : splitAcrossBounds(transformer, box)) {
                        if (!(blockX(transformer) instanceof WrapDomain.Noop)) {
                            assertTrue(part.minX >= blockX(transformer).lowerBound - 1e-9
                                            && part.maxX <= blockX(transformer).upperBound + 1e-9,
                                    () -> "part " + part + " of " + box + " leaves the X bounds " + in(transformer));
                        }
                        if (!(blockZ(transformer) instanceof WrapDomain.Noop)) {
                            assertTrue(part.minZ >= blockZ(transformer).lowerBound - 1e-9
                                            && part.maxZ <= blockZ(transformer).upperBound + 1e-9,
                                    () -> "part " + part + " of " + box + " leaves the Z bounds " + in(transformer));
                        }
                        assertEquals(box.minY, part.minY, 0.0, () -> in(transformer));
                        assertEquals(box.maxY, part.maxY, 0.0, () -> in(transformer));
                    }
                }
            }
        }

        @Test
        void containmentAgreesWithTheLatticeOfBoxCopies() {
            Random random = new Random(SEED);
            for (WorldFold transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    AABB box = sampleBox(random, transformer);
                    List<AABB> parts = splitAcrossBounds(transformer, box);
                    for (int p = 0; p < 4; p++) {
                        double x = sampleBlock(random, blockX(transformer));
                        double z = sampleBlock(random, blockZ(transformer));
                        boolean expected = containsOnLattice(blockX(transformer), box.minX, box.maxX, x)
                                && containsOnLattice(blockZ(transformer), box.minZ, box.maxZ, z);

                        double wrappedX = blockX(transformer).wrap(x);
                        double wrappedZ = blockZ(transformer).wrap(z);
                        boolean actual = parts.stream().anyMatch(part -> wrappedX >= part.minX - 1e-9
                                && wrappedX <= part.maxX + 1e-9
                                && wrappedZ >= part.minZ - 1e-9
                                && wrappedZ <= part.maxZ + 1e-9);

                        boolean finalExpected = expected;
                        assertEquals(expected, actual,
                                () -> "(" + x + ", " + z + ") in splitAcrossBounds(" + box + ") should be "
                                        + finalExpected + " " + in(transformer));
                    }
                }
            }
        }

        @Test
        void aBoxInsideTheWorldComesBackAsTheSameInstance() {
            AABB box = new AABB(1, 0, 1, 5, 10, 5);
            for (WorldFold transformer : TRANSFORMERS) {
                List<AABB> parts = splitAcrossBounds(transformer, box);
                assertEquals(1, parts.size(), () -> in(transformer));
                assertSame(box, parts.getFirst(), () -> in(transformer));
            }
        }

        @Test
        void aBoxStartingOnTheLowerBoundAndWiderThanTheWorldIsStillCutInsideTheBounds() {
            WrapDomain domain = blockX(EVEN);
            AABB box = new AABB(domain.lowerBound, 0, 0, domain.lowerBound + domain.domainLength + 10, 16, 8);
            for (AABB part : splitAcrossBounds(EVEN, box)) {
                assertTrue(part.maxX <= domain.upperBound + 1e-9,
                        () -> "part " + part + " of " + box + " leaves the X bounds " + in(EVEN));
            }
        }
    }

    @Nested
    class SplitRegionAcrossBounds {
        private BoundingBox sampleRegion(Random random, WorldFold transformer) {
            int minX = sampleBlockInt(random, blockX(transformer));
            int minY = random.nextInt(320) - 64;
            int minZ = sampleBlockInt(random, blockZ(transformer));
            return new BoundingBox(
                    minX, minY, minZ,
                    minX + random.nextInt(2 * reachCap(blockX(transformer), 16_000) + 1),
                    minY + random.nextInt(32),
                    minZ + random.nextInt(2 * reachCap(blockZ(transformer), 16_000) + 1));
        }

        private long coveredCells(WrapDomain domain, int min, int max) {
            long cells = (long) max - min + 1;
            return domain instanceof WrapDomain.Noop ? cells : Math.min(cells, domain.domainLength);
        }

        private long cellCount(BoundingBox region) {
            return ((long) region.maxX() - region.minX() + 1)
                    * (region.maxY() - region.minY() + 1)
                    * (region.maxZ() - region.minZ() + 1);
        }

        private void assertLiesOnACopyInsideTheWorld(WrapDomain domain, int coord, int copy,
                WorldFold transformer) {
            if (domain instanceof WrapDomain.Noop) {
                assertEquals(coord, copy, () -> in(transformer));
                return;
            }

            assertEquals(0, ((long) copy - coord) % domain.domainLength,
                    () -> copy + " is not a copy of " + coord + " " + in(transformer));
            assertFalse(domain.isOver(copy), () -> copy + " lies outside the world " + in(transformer));
        }

        @Test
        void partsCoverTheRegionCellsCappedAtOneWorldPerAxis() {
            Random random = new Random(SEED);
            for (WorldFold transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    BoundingBox region = sampleRegion(random, transformer);
                    long expected = coveredCells(blockX(transformer), region.minX(), region.maxX())
                            * (region.maxY() - region.minY() + 1)
                            * coveredCells(blockZ(transformer), region.minZ(), region.maxZ());

                    long total = 0;
                    for (BoundingBox part : splitAcrossBounds(transformer, region)) {
                        total += cellCount(part);
                    }

                    long covered = total;
                    assertEquals(expected, total,
                            () -> "splitAcrossBounds(" + region + ") covers " + covered + " cells instead of "
                                    + expected + " " + in(transformer));
                }
            }
        }

        @Test
        void everyPartLiesInsideTheBoundsAndKeepsTheRegionHeight() {
            Random random = new Random(SEED);
            for (WorldFold transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    BoundingBox region = sampleRegion(random, transformer);
                    for (BoundingBox part : splitAcrossBounds(transformer, region)) {
                        assertFalse(blockX(transformer).isOver(part.minX()) || blockX(transformer).isOver(part.maxX()),
                                () -> "part " + part + " of " + region + " leaves the X bounds " + in(transformer));
                        assertFalse(blockZ(transformer).isOver(part.minZ()) || blockZ(transformer).isOver(part.maxZ()),
                                () -> "part " + part + " of " + region + " leaves the Z bounds " + in(transformer));
                        assertEquals(region.minY(), part.minY(), () -> in(transformer));
                        assertEquals(region.maxY(), part.maxY(), () -> in(transformer));
                    }
                }
            }
        }

        @Test
        void containmentAgreesWithTheLatticeOfRegionCopies() {
            Random random = new Random(SEED);
            for (WorldFold transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    BoundingBox region = sampleRegion(random, transformer);
                    List<BoundingBox> parts = splitAcrossBounds(transformer, region);
                    for (int p = 0; p < 4; p++) {
                        int x = sampleBlockInt(random, blockX(transformer));
                        int z = sampleBlockInt(random, blockZ(transformer));
                        boolean expected = containsOnLattice(blockX(transformer), region.minX(), region.maxX(), x)
                                && containsOnLattice(blockZ(transformer), region.minZ(), region.maxZ(), z);

                        int wrappedX = blockX(transformer).wrap(x);
                        int wrappedZ = blockZ(transformer).wrap(z);
                        boolean actual = parts.stream()
                                .anyMatch(part -> part.isInside(wrappedX, region.minY(), wrappedZ));

                        assertEquals(expected, actual,
                                () -> "(" + x + ", " + z + ") in splitAcrossBounds(" + region + ") should be "
                                        + expected + " " + in(transformer));
                    }
                }
            }
        }

        @Test
        void aSingleBlockNamesTheSamePhysicalBlockFromEveryCopy() {
            Random random = new Random(SEED);
            for (WorldFold transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    int x = sampleBlockInt(random, blockX(transformer));
                    int y = random.nextInt(320) - 64;
                    int z = sampleBlockInt(random, blockZ(transformer));
                    BoundingBox region = new BoundingBox(x, y, z, x, y, z);

                    List<BoundingBox> parts = splitAcrossBounds(transformer, region);
                    assertEquals(1, parts.size(), () -> "single block " + region + " " + in(transformer));

                    BoundingBox part = parts.getFirst();
                    assertEquals(1, cellCount(part), () -> "single block " + region + " " + in(transformer));
                    assertEquals(y, part.minY(), () -> in(transformer));
                    assertLiesOnACopyInsideTheWorld(blockX(transformer), x, part.minX(), transformer);
                    assertLiesOnACopyInsideTheWorld(blockZ(transformer), z, part.minZ(), transformer);
                }
            }
        }

        @Test
        void aRegionInsideTheWorldComesBackAsTheSameInstance() {
            BoundingBox region = new BoundingBox(1, 0, 1, 5, 10, 5);
            for (WorldFold transformer : TRANSFORMERS) {
                List<BoundingBox> parts = splitAcrossBounds(transformer, region);
                assertEquals(1, parts.size(), () -> in(transformer));
                assertSame(region, parts.getFirst(), () -> in(transformer));
            }
        }

        @Test
        void aRegionCrossingTheSeamIsCutInTwoAtTheBounds() {
            WrapDomain domain = blockX(EVEN);
            BoundingBox region = new BoundingBox(domain.upperBound - 4, 0, 0, domain.upperBound + 3, 8, 4);

            List<BoundingBox> parts = splitAcrossBounds(EVEN, region);
            assertEquals(2, parts.size());
            assertEquals(cellCount(region), parts.stream().mapToLong(this::cellCount).sum());
            for (BoundingBox part : parts) {
                assertFalse(domain.isOver(part.minX()) || domain.isOver(part.maxX()),
                        () -> "part " + part + " leaves the X bounds " + in(EVEN));
            }

            assertSame(region, splitAcrossBounds(WorldFolds.NOOP, region).getFirst());
        }
    }

    @Nested
    class RegionFolding {
        private BoundingBox sampleRegion(Random random, WorldFold transformer) {
            int minX = sampleBlockInt(random, blockX(transformer));
            int minZ = sampleBlockInt(random, blockZ(transformer));
            int minY = random.nextInt(320) - 64;
            return new BoundingBox(
                    minX, minY, minZ,
                    minX + random.nextInt(2 * reachCap(blockX(transformer), 16_000) + 1),
                    minY + random.nextInt(32),
                    minZ + random.nextInt(2 * reachCap(blockZ(transformer), 16_000) + 1));
        }

        private boolean overlapNaive(WorldFold transformer, BoundingBox first, BoundingBox second) {
            if (first.minY() > second.maxY() || second.minY() > first.maxY()) return false;
            return axisOverlapOnLattice(blockX(transformer), first.minX(), first.maxX(), second.minX(), second.maxX())
                    && axisOverlapOnLattice(blockZ(transformer), first.minZ(), first.maxZ(), second.minZ(), second.maxZ());
        }

        private boolean axisOverlapOnLattice(WrapDomain domain, int aMin, int aMax, int bMin, int bMax) {
            for (int laps = -LAPS; laps <= LAPS; laps++) {
                long shift = (long) laps * domain.domainLength;
                if (aMin + shift <= bMax && bMin <= aMax + shift) return true;
            }
            return false;
        }

        @Test
        void regionsOverlapAgreesWithTheLatticeOfWorldCopies() {
            Random random = new Random(SEED);
            for (WorldFold transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    BoundingBox first = sampleRegion(random, transformer);
                    BoundingBox second = sampleRegion(random, transformer);
                    boolean expected = overlapNaive(transformer, first, second);
                    assertEquals(expected, transformer.regionsOverlap(first, second),
                            () -> "regionsOverlap(" + first + ", " + second + ") should be " + expected + " "
                                    + in(transformer));
                }
            }
        }

        @Test
        void regionsTouchingOnlyThroughTheSeamOverlap() {
            BoundingBox pastTheTop = new BoundingBox(500, 0, 0, 515, 5, 5);
            BoundingBox atTheBottom = new BoundingBox(-512, 0, 0, -505, 5, 5);
            assertTrue(EVEN.regionsOverlap(pastTheTop, atTheBottom));
            assertFalse(WorldFolds.NOOP.regionsOverlap(pastTheTop, atTheBottom));
        }
    }

    @Nested
    class UnwrapPairing {
        private void checkUnwrappedAxis(WrapDomain domain, int ref, int wrapped, int unwrapped,
                String axis, WorldFold transformer) {
            assertEquals(wrapped, domain.wrap(unwrapped),
                    () -> axis + ": wrap(unwrap(" + ref + ", " + wrapped + ")) changed the position " + in(transformer));

            long best = Long.MAX_VALUE;
            for (int laps = -LAPS; laps <= LAPS; laps++) {
                best = Math.min(best, Math.abs(wrapped + (long) laps * domain.domainLength - ref));
            }
            assertEquals(best, Math.abs((long) unwrapped - ref),
                    () -> axis + ": unwrap(" + ref + ", " + wrapped + ") is not the nearest copy " + in(transformer));
            if (!(domain instanceof WrapDomain.Noop)) {
                assertTrue(2L * Math.abs((long) unwrapped - ref) <= domain.domainLength,
                        () -> axis + ": unwrap(" + ref + ", " + wrapped + ") is over half a world away " + in(transformer));
            }
        }

        @Test
        void chunkUnwrapPairsWithWrapAndLandsOnTheNearestCopy() {
            Random random = new Random(SEED);
            for (WorldFold transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    ChunkPos ref = new ChunkPos(
                            sampleChunk(random, chunkX(transformer)),
                            sampleChunk(random, chunkZ(transformer)));
                    ChunkPos wrapped = transformer.fold(new ChunkPos(
                            sampleChunk(random, chunkX(transformer)),
                            sampleChunk(random, chunkZ(transformer))));

                    ChunkPos unwrapped = transformer.nearestCopy(ref, wrapped);

                    assertEquals(wrapped, transformer.fold(unwrapped),
                            () -> "Chunk.wrap(unwrap(" + ref + ", " + wrapped + ")) " + in(transformer));
                    checkUnwrappedAxis(chunkX(transformer), ref.x, wrapped.x, unwrapped.x, "chunk X", transformer);
                    checkUnwrappedAxis(chunkZ(transformer), ref.z, wrapped.z, unwrapped.z, "chunk Z", transformer);
                }
            }
        }

        @Test
        void blockUnwrapPairsWithWrapKeepsYAndLandsOnTheNearestCopy() {
            Random random = new Random(SEED);
            for (WorldFold transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    BlockPos ref = new BlockPos(
                            sampleBlockInt(random, blockX(transformer)),
                            random.nextInt(384) - 64,
                            sampleBlockInt(random, blockZ(transformer)));
                    BlockPos wrapped = transformer.fold(new BlockPos(
                            sampleBlockInt(random, blockX(transformer)),
                            random.nextInt(384) - 64,
                            sampleBlockInt(random, blockZ(transformer))));

                    BlockPos unwrapped = transformer.nearestCopy(ref, wrapped);

                    assertEquals(wrapped.getY(), unwrapped.getY(),
                            () -> "Block.unwrap moved Y " + in(transformer));
                    assertEquals(wrapped, transformer.fold(unwrapped),
                            () -> "Block.wrap(unwrap(" + ref + ", " + wrapped + ")) " + in(transformer));
                    checkUnwrappedAxis(blockX(transformer), ref.getX(), wrapped.getX(), unwrapped.getX(),
                            "block X", transformer);
                    checkUnwrappedAxis(blockZ(transformer), ref.getZ(), wrapped.getZ(), unwrapped.getZ(),
                            "block Z", transformer);
                }
            }
        }
    }

    @Nested
    class NodesAndViewDistance {
        @Test
        void wrapBlockNodeWrapsTheHorizontalsAndKeepsY() {
            Random random = new Random(SEED);
            for (WorldFold transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    int x = sampleBlockInt(random, blockX(transformer));
                    int y = random.nextInt(384) - 64;
                    int z = sampleBlockInt(random, blockZ(transformer));

                    long wrapped = transformer.foldBlockNode(BlockPos.asLong(x, y, z));

                    assertEquals(blockX(transformer).wrap(x), BlockPos.getX(wrapped),
                            () -> "wrapBlockNode X of (" + x + ", " + y + ", " + z + ") " + in(transformer));
                    assertEquals(y, BlockPos.getY(wrapped),
                            () -> "wrapBlockNode moved Y of (" + x + ", " + y + ", " + z + ") " + in(transformer));
                    assertEquals(blockZ(transformer).wrap(z), BlockPos.getZ(wrapped),
                            () -> "wrapBlockNode Z of (" + x + ", " + y + ", " + z + ") " + in(transformer));
                }
            }
        }

        @Test
        void anInBoundsNodeComesBackUnchanged() {
            long node = BlockPos.asLong(5, 70, 7);
            for (WorldFold transformer : TRANSFORMERS) {
                assertEquals(node, transformer.foldBlockNode(node), () -> in(transformer));
            }
        }

        @Test
        void viewDistanceIsClampedOnlyWhenItExceedsTheCap() {
            assertEquals(29, EVEN.limitViewDistance(32));
            assertEquals(8, EVEN.limitViewDistance(8));
            assertEquals(29, X_ONLY.limitViewDistance(64));
            assertEquals(32, WorldFolds.NOOP.limitViewDistance(32));
        }
    }
}
