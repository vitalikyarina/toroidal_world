package com.toroidalworld.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

class IdentityFastPathTest {
    private static final long SEED = 0x1DEA5L;
    private static final int SAMPLES = 600;

    private static final WorldLoopTransformer EVEN = transformer(-32, 32, -32, 32);
    private static final WorldLoopTransformer ODD = transformer(-2, 3, -2, 3);
    private static final WorldLoopTransformer UNEVEN = transformer(-48, 16, 0, 16);
    private static final WorldLoopTransformer X_ONLY = new WorldLoopTransformer(
            new WorldLoopBounds(new AxisBounds.Looped(-32, 32), AxisBounds.Unbounded.INSTANCE));

    private static final List<WorldLoopTransformer> TRANSFORMERS =
            List.of(EVEN, ODD, UNEVEN, X_ONLY, WorldLoopTransformer.NOOP);

    @Test
    void blockWrapKeepsItsValueAndReturnsTheArgumentUntouched() {
        forEachTransformer((transformer, random) -> {
            BlockPos pos = sampleBlockPos(random, transformer);
            BlockPos reference = new BlockPos(
                    transformer.coords.x.wrap(pos.getX()), pos.getY(), transformer.coords.z.wrap(pos.getZ()));
            BlockPos wrapped = transformer.blocks.wrap(pos);

            assertEquals(reference, wrapped, () -> "blocks.wrap(" + pos + ") " + in(transformer));
            if (reference.equals(pos)) {
                assertSame(pos, wrapped, () -> "an in-bounds " + pos + " must come back as itself " + in(transformer));
            }
        });
    }

    @Test
    void blockUnwrapKeepsItsValueAndReturnsTheArgumentUntouched() {
        forEachTransformer((transformer, random) -> {
            BlockPos anchor = sampleBlockPos(random, transformer);
            BlockPos wrapped = insideBlockPos(random, transformer);
            BlockPos reference = new BlockPos(
                    transformer.coords.x.unwrap(anchor.getX(), wrapped.getX()), wrapped.getY(),
                    transformer.coords.z.unwrap(anchor.getZ(), wrapped.getZ()));
            BlockPos unwrapped = transformer.blocks.unwrap(anchor, wrapped);

            assertEquals(reference, unwrapped,
                    () -> "blocks.unwrap(" + anchor + ", " + wrapped + ") " + in(transformer));
            if (reference.equals(wrapped)) {
                assertSame(wrapped, unwrapped, () -> "an unshifted " + wrapped + " must come back as itself "
                        + in(transformer));
            }
        });
    }

    @Test
    void chunkWrapKeepsItsValueAndReturnsTheArgumentUntouched() {
        forEachTransformer((transformer, random) -> {
            ChunkPos pos = sampleChunkPos(random, transformer);
            ChunkPos reference = new ChunkPos(
                    transformer.chunks.x.wrap(pos.x()), transformer.chunks.z.wrap(pos.z()));
            ChunkPos wrapped = transformer.chunks.wrap(pos);

            assertEquals(reference, wrapped, () -> "chunks.wrap(" + pos + ") " + in(transformer));
            if (reference.equals(pos)) {
                assertSame(pos, wrapped, () -> "an in-bounds " + pos + " must come back as itself " + in(transformer));
            }
        });
    }

    @Test
    void chunkUnwrapKeepsItsValueAndReturnsTheArgumentUntouched() {
        forEachTransformer((transformer, random) -> {
            ChunkPos anchor = sampleChunkPos(random, transformer);
            ChunkPos wrapped = insideChunkPos(random, transformer);
            ChunkPos reference = new ChunkPos(
                    transformer.chunks.x.unwrap(anchor.x(), wrapped.x()),
                    transformer.chunks.z.unwrap(anchor.z(), wrapped.z()));
            ChunkPos unwrapped = transformer.chunks.unwrap(anchor, wrapped);

            assertEquals(reference, unwrapped,
                    () -> "chunks.unwrap(" + anchor + ", " + wrapped + ") " + in(transformer));
            if (reference.equals(wrapped)) {
                assertSame(wrapped, unwrapped, () -> "an unshifted " + wrapped + " must come back as itself "
                        + in(transformer));
            }
        });
    }

    @Test
    void vectorWrapKeepsItsValueAndReturnsTheArgumentUntouched() {
        forEachTransformer((transformer, random) -> {
            Vec3 vec = sampleVec(random, transformer);
            Vec3 reference = new Vec3(
                    transformer.coords.x.wrap(vec.x), vec.y, transformer.coords.z.wrap(vec.z));
            Vec3 wrapped = transformer.vectors.wrap(vec);

            assertEquals(reference, wrapped, () -> "vectors.wrap(" + vec + ") " + in(transformer));
            if (reference.equals(vec)) {
                assertSame(vec, wrapped, () -> "an in-bounds " + vec + " must come back as itself " + in(transformer));
            }
        });
    }

    @Test
    void nearestCopyKeepsItsValueAndReturnsTheTargetUntouched() {
        forEachTransformer((transformer, random) -> {
            Vec3 ref = sampleVec(random, transformer);
            Vec3 target = sampleVec(random, transformer);
            Vec3 reference = new Vec3(
                    nearestCopy(transformer.coords.x, ref.x, target.x), target.y,
                    nearestCopy(transformer.coords.z, ref.z, target.z));
            Vec3 nearest = transformer.vectors.nearestCopy(ref, target);

            assertEquals(reference, nearest,
                    () -> "nearestCopy(" + ref + ", " + target + ") " + in(transformer));
            if (reference.equals(target)) {
                assertSame(target, nearest, () -> "an unshifted " + target + " must come back as itself "
                        + in(transformer));
            }
        });
    }

    @Test
    void blockNearestCopyKeepsItsValueAndReturnsTheTargetUntouched() {
        forEachTransformer((transformer, random) -> {
            BlockPos ref = sampleBlockPos(random, transformer);
            BlockPos target = sampleBlockPos(random, transformer);
            BlockPos reference = new BlockPos(
                    nearestCopy(transformer.coords.x, ref.getX(), target.getX()), target.getY(),
                    nearestCopy(transformer.coords.z, ref.getZ(), target.getZ()));
            BlockPos nearest = transformer.blocks.nearestCopy(ref, target);

            assertEquals(reference, nearest,
                    () -> "blocks.nearestCopy(" + ref + ", " + target + ") " + in(transformer));
            if (reference.equals(target)) {
                assertSame(target, nearest, () -> "an unshifted " + target + " must come back as itself "
                        + in(transformer));
            }
        });
    }

    @Test
    void crossesBoundsAgreesWithWhetherTheSplitChangesAnything() {
        forEachTransformer((transformer, random) -> {
            AABB box = sampleBox(random, transformer);
            List<AABB> pieces = transformer.splitAcrossBounds(box);
            boolean untouched = pieces.size() == 1 && pieces.getFirst() == box;

            assertEquals(untouched, !transformer.crossesBounds(box),
                    () -> "crossesBounds(" + box + ") " + in(transformer));
        });
    }

    @Test
    void aBoxWhoseFarEdgeRestsOnTheUpperBoundCrossesNothing() {
        AABB box = new AABB(500.0, 60.0, 500.0, 512.0, 62.0, 512.0);

        assertSame(box, EVEN.splitAcrossBounds(box).getFirst());
    }

    @Test
    void aBoxOfNoWidthOnTheUpperBoundStillFolds() {
        AABB box = new AABB(512.0, 60.0, 512.0, 512.0, 60.0, 512.0);
        AABB folded = EVEN.splitAcrossBounds(box).getFirst();

        assertNotSame(box, folded);
        assertEquals(-512.0, folded.minX);
        assertEquals(-512.0, folded.minZ);
    }

    @Test
    void aBoxRunningPastTheBoundsIsStillCutApart() {
        AABB box = new AABB(500.0, 60.0, -100.0, 520.0, 62.0, -90.0);

        assertEquals(2, EVEN.splitAcrossBounds(box).size());
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
        void check(WorldLoopTransformer transformer, Random random);
    }

    private static void forEachTransformer(Case body) {
        for (WorldLoopTransformer transformer : TRANSFORMERS) {
            Random random = new Random(SEED);
            for (int sample = 0; sample < SAMPLES; sample++) {
                body.check(transformer, random);
            }
        }
    }

    private static WorldLoopTransformer transformer(int xChunkMin, int xChunkMax, int zChunkMin, int zChunkMax) {
        return new WorldLoopTransformer(new WorldLoopBounds(xChunkMin, xChunkMax, zChunkMin, zChunkMax));
    }

    private static int reach(WrapDomain domain, int cap) {
        return domain instanceof WrapDomain.Noop ? cap : Math.min(3 * domain.domainLength, cap);
    }

    private static int sampleCoord(Random random, WrapDomain domain, int cap) {
        int span = reach(domain, cap);
        return random.nextInt(2 * span + 1) - span;
    }

    private static int insideCoord(Random random, WrapDomain domain, int cap) {
        return domain instanceof WrapDomain.Noop
                ? sampleCoord(random, domain, cap)
                : domain.lowerBound + random.nextInt(domain.domainLength);
    }

    private static BlockPos sampleBlockPos(Random random, WorldLoopTransformer transformer) {
        return new BlockPos(sampleCoord(random, transformer.coords.x, 16_000), random.nextInt(384) - 64,
                sampleCoord(random, transformer.coords.z, 16_000));
    }

    private static BlockPos insideBlockPos(Random random, WorldLoopTransformer transformer) {
        return new BlockPos(insideCoord(random, transformer.coords.x, 16_000), random.nextInt(384) - 64,
                insideCoord(random, transformer.coords.z, 16_000));
    }

    private static ChunkPos sampleChunkPos(Random random, WorldLoopTransformer transformer) {
        return new ChunkPos(sampleCoord(random, transformer.chunks.x, 1_000),
                sampleCoord(random, transformer.chunks.z, 1_000));
    }

    private static ChunkPos insideChunkPos(Random random, WorldLoopTransformer transformer) {
        return new ChunkPos(insideCoord(random, transformer.chunks.x, 1_000),
                insideCoord(random, transformer.chunks.z, 1_000));
    }

    private static Vec3 sampleVec(Random random, WorldLoopTransformer transformer) {
        return new Vec3(sampleCoord(random, transformer.coords.x, 16_000) + random.nextDouble(),
                random.nextInt(384) - 64 + random.nextDouble(),
                sampleCoord(random, transformer.coords.z, 16_000) + random.nextDouble());
    }

    private static AABB sampleBox(Random random, WorldLoopTransformer transformer) {
        double minX = sampleCoord(random, transformer.coords.x, 16_000) + random.nextDouble();
        double minZ = sampleCoord(random, transformer.coords.z, 16_000) + random.nextDouble();
        double minY = random.nextInt(384) - 64;
        return new AABB(minX, minY, minZ,
                minX + random.nextDouble() * 24.0, minY + random.nextDouble() * 4.0,
                minZ + random.nextDouble() * 24.0);
    }

    private static String in(WorldLoopTransformer transformer) {
        return "in " + transformer;
    }
}
