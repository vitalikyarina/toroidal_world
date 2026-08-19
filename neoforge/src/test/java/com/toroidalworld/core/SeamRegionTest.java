package com.toroidalworld.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

class SeamRegionTest {
    private static final long SEED = 0xB0C5L;
    private static final int SAMPLES = 800;
    private static final int LAPS = 16;

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

    private static boolean spansNaive(WrapDomain domain, int min, int max) {
        return !(domain instanceof WrapDomain.Noop) && 2 * Math.abs((long) max - min) > domain.domainLength;
    }

    private static String in(WorldLoopTransformer transformer) {
        return "in " + transformer;
    }

    @Nested
    class SplitAcrossBounds {
        private AABB sampleBox(Random random, WorldLoopTransformer transformer) {
            double minX = sampleBlock(random, transformer.coords.x);
            double minY = sampleY(random);
            double minZ = sampleBlock(random, transformer.coords.z);
            double sizeX = random.nextDouble() * 2.2 * reachCap(transformer.coords.x, 16_000);
            double sizeZ = random.nextDouble() * 2.2 * reachCap(transformer.coords.z, 16_000);
            return new AABB(minX, minY, minZ, minX + sizeX, minY + random.nextDouble() * 16, minZ + sizeZ);
        }

        private double coveredSize(WrapDomain domain, double size) {
            return domain instanceof WrapDomain.Noop ? size : Math.min(size, domain.domainLength);
        }

        @Test
        void partsCoverTheBoxVolumeCappedAtOneWorldPerAxis() {
            Random random = new Random(SEED);
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    AABB box = sampleBox(random, transformer);
                    List<AABB> parts = transformer.splitAcrossBounds(box);

                    double expected = coveredSize(transformer.coords.x, box.maxX - box.minX)
                            * (box.maxY - box.minY)
                            * coveredSize(transformer.coords.z, box.maxZ - box.minZ);
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
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    AABB box = sampleBox(random, transformer);
                    for (AABB part : transformer.splitAcrossBounds(box)) {
                        if (!(transformer.coords.x instanceof WrapDomain.Noop)) {
                            assertTrue(part.minX >= transformer.coords.x.lowerBound - 1e-9
                                            && part.maxX <= transformer.coords.x.upperBound + 1e-9,
                                    () -> "part " + part + " of " + box + " leaves the X bounds " + in(transformer));
                        }
                        if (!(transformer.coords.z instanceof WrapDomain.Noop)) {
                            assertTrue(part.minZ >= transformer.coords.z.lowerBound - 1e-9
                                            && part.maxZ <= transformer.coords.z.upperBound + 1e-9,
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
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    AABB box = sampleBox(random, transformer);
                    List<AABB> parts = transformer.splitAcrossBounds(box);
                    for (int p = 0; p < 4; p++) {
                        double x = sampleBlock(random, transformer.coords.x);
                        double z = sampleBlock(random, transformer.coords.z);
                        boolean expected = containsOnLattice(transformer.coords.x, box.minX, box.maxX, x)
                                && containsOnLattice(transformer.coords.z, box.minZ, box.maxZ, z);

                        double wrappedX = transformer.coords.x.wrap(x);
                        double wrappedZ = transformer.coords.z.wrap(z);
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
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
                List<AABB> parts = transformer.splitAcrossBounds(box);
                assertEquals(1, parts.size(), () -> in(transformer));
                assertSame(box, parts.getFirst(), () -> in(transformer));
            }
        }

        @Test
        void aBoxStartingOnTheLowerBoundAndWiderThanTheWorldIsStillCutInsideTheBounds() {
            WrapDomain domain = EVEN.coords.x;
            AABB box = new AABB(domain.lowerBound, 0, 0, domain.lowerBound + domain.domainLength + 10, 16, 8);
            for (AABB part : EVEN.splitAcrossBounds(box)) {
                assertTrue(part.maxX <= domain.upperBound + 1e-9,
                        () -> "part " + part + " of " + box + " leaves the X bounds " + in(EVEN));
            }
        }
    }

    @Nested
    class SplitRegionAcrossBounds {
        private BoundingBox sampleRegion(Random random, WorldLoopTransformer transformer) {
            int minX = sampleBlockInt(random, transformer.coords.x);
            int minY = random.nextInt(320) - 64;
            int minZ = sampleBlockInt(random, transformer.coords.z);
            return new BoundingBox(
                    minX, minY, minZ,
                    minX + random.nextInt(2 * reachCap(transformer.coords.x, 16_000) + 1),
                    minY + random.nextInt(32),
                    minZ + random.nextInt(2 * reachCap(transformer.coords.z, 16_000) + 1));
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
                WorldLoopTransformer transformer) {
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
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    BoundingBox region = sampleRegion(random, transformer);
                    long expected = coveredCells(transformer.coords.x, region.minX(), region.maxX())
                            * (region.maxY() - region.minY() + 1)
                            * coveredCells(transformer.coords.z, region.minZ(), region.maxZ());

                    long total = 0;
                    for (BoundingBox part : transformer.splitAcrossBounds(region)) {
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
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    BoundingBox region = sampleRegion(random, transformer);
                    for (BoundingBox part : transformer.splitAcrossBounds(region)) {
                        assertFalse(transformer.coords.x.isOver(part.minX()) || transformer.coords.x.isOver(part.maxX()),
                                () -> "part " + part + " of " + region + " leaves the X bounds " + in(transformer));
                        assertFalse(transformer.coords.z.isOver(part.minZ()) || transformer.coords.z.isOver(part.maxZ()),
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
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    BoundingBox region = sampleRegion(random, transformer);
                    List<BoundingBox> parts = transformer.splitAcrossBounds(region);
                    for (int p = 0; p < 4; p++) {
                        int x = sampleBlockInt(random, transformer.coords.x);
                        int z = sampleBlockInt(random, transformer.coords.z);
                        boolean expected = containsOnLattice(transformer.coords.x, region.minX(), region.maxX(), x)
                                && containsOnLattice(transformer.coords.z, region.minZ(), region.maxZ(), z);

                        int wrappedX = transformer.coords.x.wrap(x);
                        int wrappedZ = transformer.coords.z.wrap(z);
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
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    int x = sampleBlockInt(random, transformer.coords.x);
                    int y = random.nextInt(320) - 64;
                    int z = sampleBlockInt(random, transformer.coords.z);
                    BoundingBox region = new BoundingBox(x, y, z, x, y, z);

                    List<BoundingBox> parts = transformer.splitAcrossBounds(region);
                    assertEquals(1, parts.size(), () -> "single block " + region + " " + in(transformer));

                    BoundingBox part = parts.getFirst();
                    assertEquals(1, cellCount(part), () -> "single block " + region + " " + in(transformer));
                    assertEquals(y, part.minY(), () -> in(transformer));
                    assertLiesOnACopyInsideTheWorld(transformer.coords.x, x, part.minX(), transformer);
                    assertLiesOnACopyInsideTheWorld(transformer.coords.z, z, part.minZ(), transformer);
                }
            }
        }

        @Test
        void aRegionInsideTheWorldComesBackAsTheSameInstance() {
            BoundingBox region = new BoundingBox(1, 0, 1, 5, 10, 5);
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
                List<BoundingBox> parts = transformer.splitAcrossBounds(region);
                assertEquals(1, parts.size(), () -> in(transformer));
                assertSame(region, parts.getFirst(), () -> in(transformer));
            }
        }

        @Test
        void aRegionCrossingTheSeamIsCutInTwoAtTheBounds() {
            WrapDomain domain = EVEN.coords.x;
            BoundingBox region = new BoundingBox(domain.upperBound - 4, 0, 0, domain.upperBound + 3, 8, 4);

            List<BoundingBox> parts = EVEN.splitAcrossBounds(region);
            assertEquals(2, parts.size());
            assertEquals(cellCount(region), parts.stream().mapToLong(this::cellCount).sum());
            for (BoundingBox part : parts) {
                assertFalse(domain.isOver(part.minX()) || domain.isOver(part.maxX()),
                        () -> "part " + part + " leaves the X bounds " + in(EVEN));
            }

            assertSame(region, WorldLoopTransformer.NOOP.splitAcrossBounds(region).getFirst());
        }
    }

    @Nested
    class RegionFolding {
        private BoundingBox sampleRegion(Random random, WorldLoopTransformer transformer) {
            int minX = sampleBlockInt(random, transformer.coords.x);
            int minZ = sampleBlockInt(random, transformer.coords.z);
            int minY = random.nextInt(320) - 64;
            return new BoundingBox(
                    minX, minY, minZ,
                    minX + random.nextInt(2 * reachCap(transformer.coords.x, 16_000) + 1),
                    minY + random.nextInt(32),
                    minZ + random.nextInt(2 * reachCap(transformer.coords.z, 16_000) + 1));
        }

        private BoundingBox sampleWrappedCornerRegion(Random random, WorldLoopTransformer transformer) {
            int x1 = transformer.coords.x.wrap(sampleBlockInt(random, transformer.coords.x));
            int x2 = transformer.coords.x.wrap(sampleBlockInt(random, transformer.coords.x));
            int z1 = transformer.coords.z.wrap(sampleBlockInt(random, transformer.coords.z));
            int z2 = transformer.coords.z.wrap(sampleBlockInt(random, transformer.coords.z));
            int minY = random.nextInt(320) - 64;
            return new BoundingBox(
                    Math.min(x1, x2), minY, Math.min(z1, z2),
                    Math.max(x1, x2), minY + random.nextInt(32), Math.max(z1, z2));
        }

        private boolean overlapNaive(WorldLoopTransformer transformer, BoundingBox first, BoundingBox second) {
            if (first.minY() > second.maxY() || second.minY() > first.maxY()) return false;
            return axisOverlapOnLattice(transformer.coords.x, first.minX(), first.maxX(), second.minX(), second.maxX())
                    && axisOverlapOnLattice(transformer.coords.z, first.minZ(), first.maxZ(), second.minZ(), second.maxZ());
        }

        private boolean axisOverlapOnLattice(WrapDomain domain, int aMin, int aMax, int bMin, int bMax) {
            for (int laps = -LAPS; laps <= LAPS; laps++) {
                long shift = (long) laps * domain.domainLength;
                if (aMin + shift <= bMax && bMin <= aMax + shift) return true;
            }
            return false;
        }

        @Test
        void spansSeamIsTheDoubledWidthTestOnEitherHorizontalAxis() {
            Random random = new Random(SEED);
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    BoundingBox region = sampleRegion(random, transformer);
                    boolean expected = spansNaive(transformer.coords.x, region.minX(), region.maxX())
                            || spansNaive(transformer.coords.z, region.minZ(), region.maxZ());
                    assertEquals(expected, transformer.spansSeam(region),
                            () -> "spansSeam(" + region + ") " + in(transformer));
                }
            }
        }

        @Test
        void aRegionOnNeitherSeamComesBackAsTheSameInstance() {
            BoundingBox region = new BoundingBox(0, 0, 0, 4, 4, 4);
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
                assertSame(region, transformer.foldAcrossSeam(region), () -> in(transformer));
            }
        }

        @Test
        void foldingKeepsYFoldsOnlySpanningAxesAndCoversTheComplement() {
            Random random = new Random(SEED);
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    BoundingBox region = sampleWrappedCornerRegion(random, transformer);
                    BoundingBox folded = transformer.foldAcrossSeam(region);

                    assertEquals(region.minY(), folded.minY(), () -> in(transformer));
                    assertEquals(region.maxY(), folded.maxY(), () -> in(transformer));
                    checkFoldedAxis(random, transformer.coords.x, region.minX(), region.maxX(),
                            folded.minX(), folded.maxX(), transformer);
                    checkFoldedAxis(random, transformer.coords.z, region.minZ(), region.maxZ(),
                            folded.minZ(), folded.maxZ(), transformer);
                }
            }
        }

        private void checkFoldedAxis(Random random, WrapDomain domain, int min, int max,
                int foldedMin, int foldedMax, WorldLoopTransformer transformer) {
            if (!spansNaive(domain, min, max)) {
                assertEquals(min, foldedMin, () -> "non-spanning axis moved its start " + in(transformer));
                assertEquals(max, foldedMax, () -> "non-spanning axis moved its end " + in(transformer));
                return;
            }

            assertEquals(domain.domainLength, (foldedMax - foldedMin) + (max - min),
                    () -> "folded [" + min + ", " + max + "] " + in(transformer));
            assertFalse(domain.isOver(foldedMin),
                    () -> "folded [" + min + ", " + max + "] starts outside the world " + in(transformer));
            assertTrue(foldedMax >= foldedMin,
                    () -> "folded [" + min + ", " + max + "] runs backwards " + in(transformer));
            for (int p = 0; p < 8; p++) {
                int coord = domain.wrap(sampleBlockInt(random, domain));
                assertTrue(containsOnLattice(domain, min, max, coord)
                                || containsOnLattice(domain, foldedMin, foldedMax, coord),
                        () -> coord + " is covered by neither reading of [" + min + ", " + max + "] " + in(transformer));
            }
        }

        @Test
        void regionsOverlapAgreesWithTheLatticeOfWorldCopies() {
            Random random = new Random(SEED);
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
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
            assertFalse(WorldLoopTransformer.NOOP.regionsOverlap(pastTheTop, atTheBottom));
        }
    }

    @Nested
    class UnwrapPairing {
        private void checkUnwrappedAxis(WrapDomain domain, int ref, int wrapped, int unwrapped,
                String axis, WorldLoopTransformer transformer) {
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
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    ChunkPos ref = new ChunkPos(
                            sampleChunk(random, transformer.chunks.x),
                            sampleChunk(random, transformer.chunks.z));
                    ChunkPos wrapped = transformer.chunks.wrap(new ChunkPos(
                            sampleChunk(random, transformer.chunks.x),
                            sampleChunk(random, transformer.chunks.z)));

                    ChunkPos unwrapped = transformer.chunks.unwrap(ref, wrapped);

                    assertEquals(wrapped, transformer.chunks.wrap(unwrapped),
                            () -> "Chunk.wrap(unwrap(" + ref + ", " + wrapped + ")) " + in(transformer));
                    checkUnwrappedAxis(transformer.chunks.x, ref.x(), wrapped.x(), unwrapped.x(), "chunk X", transformer);
                    checkUnwrappedAxis(transformer.chunks.z, ref.z(), wrapped.z(), unwrapped.z(), "chunk Z", transformer);
                }
            }
        }

        @Test
        void blockUnwrapPairsWithWrapKeepsYAndLandsOnTheNearestCopy() {
            Random random = new Random(SEED);
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    BlockPos ref = new BlockPos(
                            sampleBlockInt(random, transformer.coords.x),
                            random.nextInt(384) - 64,
                            sampleBlockInt(random, transformer.coords.z));
                    BlockPos wrapped = transformer.blocks.wrap(new BlockPos(
                            sampleBlockInt(random, transformer.coords.x),
                            random.nextInt(384) - 64,
                            sampleBlockInt(random, transformer.coords.z)));

                    BlockPos unwrapped = transformer.blocks.unwrap(ref, wrapped);

                    assertEquals(wrapped.getY(), unwrapped.getY(),
                            () -> "Block.unwrap moved Y " + in(transformer));
                    assertEquals(wrapped, transformer.blocks.wrap(unwrapped),
                            () -> "Block.wrap(unwrap(" + ref + ", " + wrapped + ")) " + in(transformer));
                    checkUnwrappedAxis(transformer.coords.x, ref.getX(), wrapped.getX(), unwrapped.getX(),
                            "block X", transformer);
                    checkUnwrappedAxis(transformer.coords.z, ref.getZ(), wrapped.getZ(), unwrapped.getZ(),
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
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
                for (int i = 0; i < SAMPLES; i++) {
                    int x = sampleBlockInt(random, transformer.coords.x);
                    int y = random.nextInt(384) - 64;
                    int z = sampleBlockInt(random, transformer.coords.z);

                    long wrapped = transformer.wrapBlockNode(BlockPos.asLong(x, y, z));

                    assertEquals(transformer.coords.x.wrap(x), BlockPos.getX(wrapped),
                            () -> "wrapBlockNode X of (" + x + ", " + y + ", " + z + ") " + in(transformer));
                    assertEquals(y, BlockPos.getY(wrapped),
                            () -> "wrapBlockNode moved Y of (" + x + ", " + y + ", " + z + ") " + in(transformer));
                    assertEquals(transformer.coords.z.wrap(z), BlockPos.getZ(wrapped),
                            () -> "wrapBlockNode Z of (" + x + ", " + y + ", " + z + ") " + in(transformer));
                }
            }
        }

        @Test
        void anInBoundsNodeComesBackUnchanged() {
            long node = BlockPos.asLong(5, 70, 7);
            for (WorldLoopTransformer transformer : TRANSFORMERS) {
                assertEquals(node, transformer.wrapBlockNode(node), () -> in(transformer));
            }
        }

        @Test
        void maxViewDistanceIsHalfTheNarrowerWorldMinusTheBuffer() {
            assertEquals(29, EVEN.maxViewDistance());
            assertEquals(29, X_ONLY.maxViewDistance());
            assertEquals(5, UNEVEN.maxViewDistance());
        }

        @Test
        void viewDistanceIsClampedOnlyWhenItExceedsTheCap() {
            assertEquals(29, EVEN.limitViewDistance(32));
            assertEquals(8, EVEN.limitViewDistance(8));
            assertEquals(29, X_ONLY.limitViewDistance(64));
            assertEquals(32, WorldLoopTransformer.NOOP.limitViewDistance(32));
        }

        @Test
        void aWorldNarrowerThanTheBufferStillRendersOneChunk() {
            assertEquals(1, ODD.maxViewDistance());
            assertEquals(1, ODD.limitViewDistance(32));
        }
    }
}
