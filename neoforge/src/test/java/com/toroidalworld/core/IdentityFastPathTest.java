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
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

class IdentityFastPathTest {
    private static final long SEED = 0x1DEA5L;
    private static final int SAMPLES = 600;
    private static final int CHUNK_BLOCKS = 16;

    private static final WorldFold EVEN = transformer(-32, 32, -32, 32);
    private static final WorldFold ODD = transformer(-2, 3, -2, 3);
    private static final WorldFold UNEVEN = transformer(-48, 16, 0, 16);
    private static final WorldFold X_ONLY = new WorldLoopTransformer(
            new WorldLoopBounds(new AxisBounds.Looped(-32, 32), AxisBounds.Unbounded.INSTANCE));

    private static final List<WorldFold> TRANSFORMERS =
            List.of(EVEN, ODD, UNEVEN, X_ONLY, WorldFolds.NOOP);

    @Test
    void blockWrapKeepsItsValueAndReturnsTheArgumentUntouched() {
        forEachTransformer((transformer, random) -> {
            BlockPos pos = sampleBlockPos(random, transformer);
            BlockPos reference = new BlockPos(
                    blockX(transformer).wrap(pos.getX()), pos.getY(), blockZ(transformer).wrap(pos.getZ()));
            BlockPos wrapped = transformer.fold(pos);

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
                    blockX(transformer).unwrap(anchor.getX(), wrapped.getX()), wrapped.getY(),
                    blockZ(transformer).unwrap(anchor.getZ(), wrapped.getZ()));
            BlockPos unwrapped = transformer.nearestCopy(anchor, wrapped);

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
                    chunkX(transformer).wrap(pos.x), chunkZ(transformer).wrap(pos.z));
            ChunkPos wrapped = transformer.fold(pos);

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
                    chunkX(transformer).unwrap(anchor.x, wrapped.x),
                    chunkZ(transformer).unwrap(anchor.z, wrapped.z));
            ChunkPos unwrapped = transformer.nearestCopy(anchor, wrapped);

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
                    blockX(transformer).wrap(vec.x), vec.y, blockZ(transformer).wrap(vec.z));
            Vec3 wrapped = transformer.fold(vec);

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
                    nearestCopy(blockX(transformer), ref.x, target.x), target.y,
                    nearestCopy(blockZ(transformer), ref.z, target.z));
            Vec3 nearest = transformer.nearestCopy(ref, target);

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
                    nearestCopy(blockX(transformer), ref.getX(), target.getX()), target.getY(),
                    nearestCopy(blockZ(transformer), ref.getZ(), target.getZ()));
            BlockPos nearest = transformer.nearestCopy(ref, target);

            assertEquals(reference, nearest,
                    () -> "blocks.nearestCopy(" + ref + ", " + target + ") " + in(transformer));
            if (reference.equals(target)) {
                assertSame(target, nearest, () -> "an unshifted " + target + " must come back as itself "
                        + in(transformer));
            }
        });
    }

    @Test
    void reseatKeepsTheLowBitsAndReturnsTheArgumentUntouchedInItsOwnChunk() {
        forEachTransformer((transformer, random) -> {
            BlockPos pos = sampleBlockPos(random, transformer);
            ChunkPos chunk = ChunkPos.containing(pos);
            ChunkPos copy = new ChunkPos(
                    chunk.x() + lapsOf(random, chunkX(transformer)), chunk.z() + lapsOf(random, chunkZ(transformer)));
            BlockPos reference = new BlockPos(
                    copy.getMinBlockX() + Math.floorMod(pos.getX(), CHUNK_BLOCKS), pos.getY(),
                    copy.getMinBlockZ() + Math.floorMod(pos.getZ(), CHUNK_BLOCKS));
            BlockPos reseated = transformer.reseat(pos, copy);

            assertEquals(reference, reseated, () -> "reseat(" + pos + ", " + copy + ") " + in(transformer));
            if (copy.equals(chunk)) {
                assertSame(pos, reseated, () -> "a reseat of " + pos + " into its own chunk must come back as itself "
                        + in(transformer));
            }
        });
    }

    @Test
    void theDeckTransformationOfAChunkOntoItselfIsTheIdentityInstance() {
        forEachTransformer((transformer, random) -> {
            ChunkPos chunk = sampleChunkPos(random, transformer);
            BoundingBox box = new BoundingBox(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(),
                    chunk.getMaxBlockX(), 10, chunk.getMaxBlockZ());

            assertSame(DeckTransformation.IDENTITY, transformer.deckTransformation(chunk, chunk),
                    () -> "a chunk carried onto itself " + in(transformer));
            assertSame(box, DeckTransformation.IDENTITY.apply(box), "the identity rebuilt a box");
            assertSame(chunk, DeckTransformation.IDENTITY.apply(chunk), "the identity rebuilt a chunk");
        });
    }

    @Test
    void crossesBoundsAgreesWithWhetherTheSplitChangesAnything() {
        forEachTransformer((transformer, random) -> {
            AABB box = sampleBox(random, transformer);
            List<WorldFold.Folded<AABB>> pieces = transformer.split(box);
            boolean untouched = pieces.size() == 1 && pieces.getFirst().value() == box;

            assertEquals(untouched, !transformer.crossesBounds(box),
                    () -> "crossesBounds(" + box + ") " + in(transformer));
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

    private static int lapsOf(Random random, WrapDomain domain) {
        return domain instanceof WrapDomain.Noop ? 0 : (random.nextInt(5) - 2) * domain.domainLength;
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
        void check(WorldFold transformer, Random random);
    }

    private static void forEachTransformer(Case body) {
        for (WorldFold transformer : TRANSFORMERS) {
            Random random = new Random(SEED);
            for (int sample = 0; sample < SAMPLES; sample++) {
                body.check(transformer, random);
            }
        }
    }

    private static WorldFold transformer(int xChunkMin, int xChunkMax, int zChunkMin, int zChunkMax) {
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

    private static BlockPos sampleBlockPos(Random random, WorldFold transformer) {
        return new BlockPos(sampleCoord(random, blockX(transformer), 16_000), random.nextInt(384) - 64,
                sampleCoord(random, blockZ(transformer), 16_000));
    }

    private static BlockPos insideBlockPos(Random random, WorldFold transformer) {
        return new BlockPos(insideCoord(random, blockX(transformer), 16_000), random.nextInt(384) - 64,
                insideCoord(random, blockZ(transformer), 16_000));
    }

    private static ChunkPos sampleChunkPos(Random random, WorldFold transformer) {
        return new ChunkPos(sampleCoord(random, chunkX(transformer), 1_000),
                sampleCoord(random, chunkZ(transformer), 1_000));
    }

    private static ChunkPos insideChunkPos(Random random, WorldFold transformer) {
        return new ChunkPos(insideCoord(random, chunkX(transformer), 1_000),
                insideCoord(random, chunkZ(transformer), 1_000));
    }

    private static Vec3 sampleVec(Random random, WorldFold transformer) {
        return new Vec3(sampleCoord(random, blockX(transformer), 16_000) + random.nextDouble(),
                random.nextInt(384) - 64 + random.nextDouble(),
                sampleCoord(random, blockZ(transformer), 16_000) + random.nextDouble());
    }

    private static AABB sampleBox(Random random, WorldFold transformer) {
        double minX = sampleCoord(random, blockX(transformer), 16_000) + random.nextDouble();
        double minZ = sampleCoord(random, blockZ(transformer), 16_000) + random.nextDouble();
        double minY = random.nextInt(384) - 64;
        return new AABB(minX, minY, minZ,
                minX + random.nextDouble() * 24.0, minY + random.nextDouble() * 4.0,
                minZ + random.nextDouble() * 24.0);
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

    private static String in(WorldFold transformer) {
        return "in " + transformer;
    }
}
