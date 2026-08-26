package com.toroidalworld.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.world.level.levelgen.structure.BoundingBox;

class SeamSpansTest {
    private static final long SEED = 0x5EA45L;
    private static final int SAMPLES = 800;
    private static final int LAPS = 16;

    private static final WorldFold EVEN = torus(-32, 32, -32, 32);
    private static final WorldFold ODD = torus(-2, 3, -2, 3);
    private static final WorldFold UNEVEN = torus(-48, 16, 0, 16);
    private static final WorldFold X_ONLY = WorldFolds.of(FlatShape.cylinder(
            new WorldLoopBounds(new AxisBounds.Looped(-32, 32), AxisBounds.Unbounded.INSTANCE)));

    private static final List<WorldFold> FOLDS = List.of(EVEN, ODD, UNEVEN, X_ONLY, WorldFolds.NOOP);

    private static WorldFold torus(int xChunkMin, int xChunkMax, int zChunkMin, int zChunkMax) {
        return WorldFolds.of(FlatShape.latticeTorus(
                new WorldLoopBounds(xChunkMin, xChunkMax, zChunkMin, zChunkMax), FlatShape.NO_SKEW));
    }

    private record Axis(boolean looped, int lower, int width) {
        static Axis of(AxisBounds bounds) {
            return switch (bounds) {
                case AxisBounds.Looped looped -> new Axis(true, looped.minBlock(), looped.blockWidth());
                case AxisBounds.Unbounded() -> new Axis(false, 0, 0);
            };
        }

        int sampleBlock(Random random) {
            int reach = 3 * (this.looped ? Math.min(this.width, 16_000) : 16_000);
            return random.nextInt(2 * reach + 1) - reach;
        }

        int wrap(int coord) {
            return this.looped ? this.lower + Math.floorMod(coord - this.lower, this.width) : coord;
        }

        boolean isOver(int coord) {
            return this.looped && (coord < this.lower || coord >= this.lower + this.width);
        }

        boolean spansNaive(int min, int max) {
            return this.looped && 2 * Math.abs((long) max - min) > this.width;
        }

        boolean containsOnLattice(long min, long max, long coord) {
            for (int laps = -LAPS; laps <= LAPS; laps++) {
                long shifted = coord + (long) laps * this.width;
                if (shifted >= min && shifted <= max) {
                    return true;
                }
            }

            return false;
        }
    }

    private static Axis xAxis(WorldFold fold) {
        return Axis.of(fold.bounds().x());
    }

    private static Axis zAxis(WorldFold fold) {
        return Axis.of(fold.bounds().z());
    }

    private static BoundingBox sampleRegion(Random random, WorldFold fold) {
        Axis x = xAxis(fold);
        Axis z = zAxis(fold);
        int minX = x.sampleBlock(random);
        int minZ = z.sampleBlock(random);
        int minY = random.nextInt(320) - 64;
        return new BoundingBox(
                minX, minY, minZ,
                minX + random.nextInt(2 * (x.looped() ? Math.min(x.width(), 16_000) : 16_000) + 1),
                minY + random.nextInt(32),
                minZ + random.nextInt(2 * (z.looped() ? Math.min(z.width(), 16_000) : 16_000) + 1));
    }

    private static BoundingBox sampleWrappedCornerRegion(Random random, WorldFold fold) {
        Axis x = xAxis(fold);
        Axis z = zAxis(fold);
        int x1 = x.wrap(x.sampleBlock(random));
        int x2 = x.wrap(x.sampleBlock(random));
        int z1 = z.wrap(z.sampleBlock(random));
        int z2 = z.wrap(z.sampleBlock(random));
        int minY = random.nextInt(320) - 64;
        return new BoundingBox(
                Math.min(x1, x2), minY, Math.min(z1, z2),
                Math.max(x1, x2), minY + random.nextInt(32), Math.max(z1, z2));
    }

    private static String in(WorldFold fold) {
        return "in " + fold;
    }

    @Test
    void crossesSeamIsTheDoubledWidthTestOnEitherHorizontalAxis() {
        Random random = new Random(SEED);
        for (WorldFold fold : FOLDS) {
            for (int i = 0; i < SAMPLES; i++) {
                BoundingBox region = sampleRegion(random, fold);
                boolean expected = xAxis(fold).spansNaive(region.minX(), region.maxX())
                        || zAxis(fold).spansNaive(region.minZ(), region.maxZ());
                assertEquals(expected, SeamSpans.crossesSeam(fold, region),
                        () -> "crossesSeam(" + region + ") " + in(fold));
            }
        }
    }

    @Test
    void aRegionOnNeitherSeamComesBackAsTheSameInstance() {
        BoundingBox region = new BoundingBox(0, 0, 0, 4, 4, 4);
        for (WorldFold fold : FOLDS) {
            assertSame(region, SeamSpans.foldAcrossSeam(fold, region), () -> in(fold));
        }
    }

    @Test
    void foldingKeepsYFoldsOnlySpanningAxesAndCoversTheComplement() {
        Random random = new Random(SEED);
        for (WorldFold fold : FOLDS) {
            for (int i = 0; i < SAMPLES; i++) {
                BoundingBox region = sampleWrappedCornerRegion(random, fold);
                BoundingBox folded = SeamSpans.foldAcrossSeam(fold, region);

                assertEquals(region.minY(), folded.minY(), () -> in(fold));
                assertEquals(region.maxY(), folded.maxY(), () -> in(fold));
                checkFoldedAxis(random, xAxis(fold), region.minX(), region.maxX(),
                        folded.minX(), folded.maxX(), fold);
                checkFoldedAxis(random, zAxis(fold), region.minZ(), region.maxZ(),
                        folded.minZ(), folded.maxZ(), fold);
            }
        }
    }

    private static void checkFoldedAxis(Random random, Axis axis, int min, int max,
            int foldedMin, int foldedMax, WorldFold fold) {
        if (!axis.spansNaive(min, max)) {
            assertEquals(min, foldedMin, () -> "non-spanning axis moved its start " + in(fold));
            assertEquals(max, foldedMax, () -> "non-spanning axis moved its end " + in(fold));
            return;
        }

        assertEquals(axis.width(), (foldedMax - foldedMin) + (max - min),
                () -> "folded [" + min + ", " + max + "] " + in(fold));
        assertFalse(axis.isOver(foldedMin),
                () -> "folded [" + min + ", " + max + "] starts outside the world " + in(fold));
        assertTrue(foldedMax >= foldedMin,
                () -> "folded [" + min + ", " + max + "] runs backwards " + in(fold));
        for (int p = 0; p < 8; p++) {
            int coord = axis.wrap(axis.sampleBlock(random));
            assertTrue(axis.containsOnLattice(min, max, coord) || axis.containsOnLattice(foldedMin, foldedMax, coord),
                    () -> coord + " is covered by neither reading of [" + min + ", " + max + "] " + in(fold));
        }
    }

    @Test
    void aShapeThatDoesNotDecomposeRefusesTheCornerPairReading() {
        WorldFold skewed = new DeckGroupFold(FlatShape.latticeTorus(WorldLoopBounds.ofWidth(64), 5));
        BoundingBox region = new BoundingBox(-500, 0, -500, 500, 4, 500);

        assertThrows(IllegalStateException.class, () -> SeamSpans.crossesSeam(skewed, region));
        assertThrows(IllegalStateException.class, () -> SeamSpans.foldAcrossSeam(skewed, region));
    }
}
