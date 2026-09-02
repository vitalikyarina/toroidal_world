package com.toroidalworld.compat.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.DeckGroupFold;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

class CreateSeamFoldTest {
    private static final int WORLD_CHUNKS = 16;
    private static final int WORLD_BLOCKS = WORLD_CHUNKS * 2 * 16;
    private static final int SKEW_CHUNKS = 4;
    private static final int MIRROR_LINE_CHUNK = 3;

    private static final WorldLoopBounds BOUNDS =
            new WorldLoopBounds(-WORLD_CHUNKS, WORLD_CHUNKS, -WORLD_CHUNKS, WORLD_CHUNKS);

    private static final WorldFold PER_AXIS = WorldFolds.of(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));
    private static final WorldFold DECK_TORUS = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));
    private static final WorldFold SKEWED = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, SKEW_CHUNKS));
    private static final WorldFold MIRRORED =
            new DeckGroupFold(FlatShape.mirrored(BOUNDS, Direction.Axis.Z, MIRROR_LINE_CHUNK));

    private static final List<WorldFold> UNSKEWED = List.of(PER_AXIS, DECK_TORUS);
    private static final List<WorldFold> TRANSLATING = List.of(PER_AXIS, DECK_TORUS, SKEWED);
    private static final List<WorldFold> ALL = List.of(PER_AXIS, DECK_TORUS, SKEWED, MIRRORED);

    private static final BlockPos ANCHOR = new BlockPos(250, 64, 10);
    private static final BlockPos TARGET_ACROSS = new BlockPos(-254, 64, 10);
    private static final BlockPos PAST_THE_BOUNDS = new BlockPos(300, 64, 10);

    private static BlockPos rawDelta(BlockPos anchor, BlockPos target) {
        return target.subtract(anchor);
    }

    @Test
    void aDeltaAcrossTheSeamRunsFromTheAnchorToTheNearestCopy() {
        for (WorldFold fold : ALL) {
            BlockPos nearest = fold.nearestCopy(ANCHOR, TARGET_ACROSS);
            BlockPos delta = CreateSeamFold.delta(fold, ANCHOR, TARGET_ACROSS, rawDelta(ANCHOR, TARGET_ACROSS));

            assertEquals(nearest, ANCHOR.offset(delta), "in " + fold);
        }
    }

    @Test
    void theDeltaKeepsTheSignOfTheSubtractionItReplaces() {
        for (WorldFold fold : UNSKEWED) {
            BlockPos delta = CreateSeamFold.delta(fold, ANCHOR, TARGET_ACROSS, rawDelta(ANCHOR, TARGET_ACROSS));

            assertEquals(new BlockPos(WORLD_BLOCKS - 504, 0, 0), delta, "in " + fold);
        }
    }

    @Test
    void aTargetAlreadyNearestGivesTheRawDeltaBackByIdentity() {
        BlockPos target = new BlockPos(240, 64, 10);
        BlockPos raw = rawDelta(ANCHOR, target);
        for (WorldFold fold : ALL) {
            assertSame(raw, CreateSeamFold.delta(fold, ANCHOR, target, raw), "in " + fold);
        }
    }

    @Test
    void anUnwrappedWorldGivesTheRawDeltaBackByIdentity() {
        BlockPos raw = rawDelta(ANCHOR, TARGET_ACROSS);

        assertSame(raw, CreateSeamFold.delta(WorldFolds.NOOP, ANCHOR, TARGET_ACROSS, raw));
        assertSame(raw, CreateSeamFold.delta(null, ANCHOR, TARGET_ACROSS, raw));
        assertSame(raw, CreateSeamFold.foldDelta((Level) null, ANCHOR, TARGET_ACROSS, raw));
    }

    @Test
    void theFarEndDeltaIsTheDeltaTheFarEndStores() {
        for (WorldFold fold : ALL) {
            BlockPos forward = CreateSeamFold.delta(fold, ANCHOR, TARGET_ACROSS, rawDelta(ANCHOR, TARGET_ACROSS));
            BlockPos stored = CreateSeamFold.delta(fold, TARGET_ACROSS, ANCHOR, rawDelta(TARGET_ACROSS, ANCHOR));

            assertEquals(stored, CreateSeamFold.farEndDelta(fold, ANCHOR, forward, forward.multiply(-1)),
                    "in " + fold);
        }
    }

    @Test
    void aNegatedDeltaIsTheFarEndsOnlyWhereTheFoldIsATranslation() {
        for (WorldFold fold : TRANSLATING) {
            BlockPos forward = CreateSeamFold.delta(fold, ANCHOR, TARGET_ACROSS, rawDelta(ANCHOR, TARGET_ACROSS));
            BlockPos stored = CreateSeamFold.delta(fold, TARGET_ACROSS, ANCHOR, rawDelta(TARGET_ACROSS, ANCHOR));

            assertEquals(forward.multiply(-1), stored, "in " + fold);
        }

        BlockPos forward = CreateSeamFold.delta(MIRRORED, ANCHOR, TARGET_ACROSS, rawDelta(ANCHOR, TARGET_ACROSS));
        BlockPos stored = CreateSeamFold.delta(MIRRORED, TARGET_ACROSS, ANCHOR, rawDelta(TARGET_ACROSS, ANCHOR));

        assertNotEquals(forward.multiply(-1), stored);
    }

    @Test
    void aConnectionInsideTheBoundsGivesTheNegationBackByIdentity() {
        BlockPos delta = rawDelta(ANCHOR, new BlockPos(240, 64, 10));
        BlockPos raw = delta.multiply(-1);
        for (WorldFold fold : ALL) {
            assertSame(raw, CreateSeamFold.farEndDelta(fold, ANCHOR, delta, raw), "in " + fold);
        }
    }

    @Test
    void anUnwrappedWorldGivesTheFarEndDeltaBackByIdentity() {
        BlockPos delta = rawDelta(ANCHOR, TARGET_ACROSS);
        BlockPos raw = delta.multiply(-1);

        assertSame(raw, CreateSeamFold.farEndDelta(WorldFolds.NOOP, ANCHOR, delta, raw));
        assertSame(raw, CreateSeamFold.farEndDelta((WorldFold) null, ANCHOR, delta, raw));
        assertSame(raw, CreateSeamFold.farEndDelta((Level) null, ANCHOR, delta, raw));
    }

    @Test
    void aPositionIsFoldedTowardTheBoxCentreAndNotItsCorner() {
        BoundingBox box = new BoundingBox(-250, 0, -10, 250, 10, 10);
        BlockPos corner = new BlockPos(box.minX(), box.minY(), box.minZ());
        BlockPos beyondTheCorner = new BlockPos(255, 5, 0);
        for (WorldFold fold : ALL) {
            assertSame(beyondTheCorner, CreateSeamFold.foldPositionToBox(fold, box, beyondTheCorner), "in " + fold);
            assertNotEquals(CreateSeamFold.nearest(fold, corner, beyondTheCorner),
                    CreateSeamFold.foldPositionToBox(fold, box, beyondTheCorner), "in " + fold);
        }
    }

    @Test
    void aPositionPastHalfAWorldFromTheBoxCentreIsFolded() {
        BoundingBox box = new BoundingBox(-250, 0, -10, 250, 10, 10);
        BlockPos beyond = new BlockPos(-300, 5, 0);
        for (WorldFold fold : ALL) {
            BlockPos folded = CreateSeamFold.foldPositionToBox(fold, box, beyond);

            assertEquals(fold.nearestCopy(box.getCenter(), beyond), folded, "in " + fold);
            assertNotEquals(beyond, folded, "in " + fold);
        }
    }

    @Test
    void anUnwrappedWorldLeavesTheBoxPositionUntouched() {
        BoundingBox box = new BoundingBox(-250, 0, -10, 250, 10, 10);
        BlockPos beyond = new BlockPos(-300, 5, 0);

        assertSame(beyond, CreateSeamFold.foldPositionToBox(WorldFolds.NOOP, box, beyond));
        assertSame(beyond, CreateSeamFold.foldPositionToBox((WorldFold) null, box, beyond));
        assertSame(beyond, CreateSeamFold.foldPositionToBox((Level) null, box, beyond));
    }

    @Test
    void aHitPastTheBoundsIsReSeatedOntoTheWrappedBlockKeepingItsOffset() {
        Vec3 offsetInBlock = new Vec3(0.25, 0.5, 0.75);
        BlockHitResult hit = new BlockHitResult(
                Vec3.atLowerCornerOf(PAST_THE_BOUNDS).add(offsetInBlock), Direction.NORTH, PAST_THE_BOUNDS, true);

        for (WorldFold fold : UNSKEWED) {
            BlockHitResult folded = CreateSeamFold.canonical(fold, hit);

            assertEquals(new BlockPos(PAST_THE_BOUNDS.getX() - WORLD_BLOCKS, 64, 10), folded.getBlockPos(),
                    "in " + fold);
            assertEquals(Vec3.atLowerCornerOf(folded.getBlockPos()).add(offsetInBlock), folded.getLocation(),
                    "in " + fold);
            assertEquals(Direction.NORTH, folded.getDirection(), "in " + fold);
            assertTrue(folded.isInside(), "in " + fold);
            assertEquals(HitResult.Type.BLOCK, folded.getType(), "in " + fold);
        }
    }

    @Test
    void theHitLandsOnWhicheverBlockTheFoldNames() {
        Vec3 offsetInBlock = new Vec3(0.25, 0.5, 0.75);
        BlockHitResult hit = new BlockHitResult(
                Vec3.atLowerCornerOf(PAST_THE_BOUNDS).add(offsetInBlock), Direction.NORTH, PAST_THE_BOUNDS, true);

        for (WorldFold fold : ALL) {
            BlockHitResult folded = CreateSeamFold.canonical(fold, hit);

            assertEquals(fold.fold(PAST_THE_BOUNDS), folded.getBlockPos(), "in " + fold);
            assertTrue(folded.isInside(), "in " + fold);
        }
    }

    @Test
    void aMissPastTheBoundsStaysAMiss() {
        Vec3 offsetInBlock = new Vec3(0.25, 0.5, 0.75);
        BlockHitResult miss = BlockHitResult.miss(
                Vec3.atLowerCornerOf(PAST_THE_BOUNDS).add(offsetInBlock), Direction.NORTH, PAST_THE_BOUNDS);

        for (WorldFold fold : ALL) {
            BlockHitResult folded = CreateSeamFold.canonical(fold, miss);

            assertEquals(HitResult.Type.MISS, folded.getType(), "in " + fold);
            assertEquals(fold.fold(PAST_THE_BOUNDS), folded.getBlockPos(), "in " + fold);
        }
    }

    @Test
    void aHitInsideTheBoundsIsGivenBackByIdentity() {
        BlockHitResult hit = new BlockHitResult(
                new Vec3(10.25, 64.5, 10.75), Direction.NORTH, new BlockPos(10, 64, 10), true);

        for (WorldFold fold : ALL) {
            assertSame(hit, CreateSeamFold.canonical(fold, hit), "in " + fold);
        }
    }

    @Test
    void anUnwrappedWorldGivesTheHitBackByIdentity() {
        BlockHitResult hit = new BlockHitResult(
                new Vec3(300.25, 64.5, 10.75), Direction.NORTH, PAST_THE_BOUNDS, true);

        assertSame(hit, CreateSeamFold.canonical(WorldFolds.NOOP, hit));
        assertSame(hit, CreateSeamFold.canonical((WorldFold) null, hit));
        assertSame(hit, CreateSeamFold.canonical((ServerLevel) null, hit));
    }

    @Test
    void anUnwrappedWorldGivesTheCanonicalPositionBackByIdentity() {
        assertSame(PAST_THE_BOUNDS, CreateSeamFold.canonical(WorldFolds.NOOP, PAST_THE_BOUNDS));
        assertSame(PAST_THE_BOUNDS, CreateSeamFold.canonical((WorldFold) null, PAST_THE_BOUNDS));
        assertSame(PAST_THE_BOUNDS, CreateSeamFold.canonical((ServerLevel) null, PAST_THE_BOUNDS));
    }

    @Test
    void anUnwrappedWorldLeavesFoldPositionUntouched() {
        assertSame(TARGET_ACROSS, CreateSeamFold.nearest(WorldFolds.NOOP, ANCHOR, TARGET_ACROSS));
        assertSame(TARGET_ACROSS, CreateSeamFold.nearest(null, ANCHOR, TARGET_ACROSS));
        assertSame(TARGET_ACROSS, CreateSeamFold.foldPosition((Level) null, ANCHOR, TARGET_ACROSS));
    }
}
