package com.toroidalworld.compat.create;

import static com.toroidalworld.compat.CompatFoldFixture.CYLINDER;
import static com.toroidalworld.compat.CompatFoldFixture.DECK_TORUS;
import static com.toroidalworld.compat.CompatFoldFixture.MIRRORED;
import static com.toroidalworld.compat.CompatFoldFixture.MIRROR_LINE_BLOCKS;
import static com.toroidalworld.compat.CompatFoldFixture.PER_AXIS;
import static com.toroidalworld.compat.CompatFoldFixture.SKEWED;
import static com.toroidalworld.compat.CompatFoldFixture.WORLD_BLOCKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

class CreateSeamFoldTest {
    private static final double FAR_UNBOUNDED_Z = 100000.5;
    private static final double RUN_Y = 64.0;

    private static final List<WorldFold> UNSKEWED = List.of(PER_AXIS, DECK_TORUS);
    private static final List<WorldFold> TRANSLATING = List.of(PER_AXIS, DECK_TORUS, SKEWED);
    private static final List<WorldFold> ALL = List.of(PER_AXIS, DECK_TORUS, SKEWED, MIRRORED);

    private static final List<WorldFold> RUN_TRANSLATING = List.of(PER_AXIS, DECK_TORUS, SKEWED, CYLINDER);
    private static final List<WorldFold> RUN_ALL = List.of(PER_AXIS, DECK_TORUS, SKEWED, MIRRORED, CYLINDER);

    private static final BlockPos ANCHOR = new BlockPos(250, 64, 10);
    private static final BlockPos TARGET_ACROSS = new BlockPos(-254, 64, 10);
    private static final BlockPos PAST_THE_BOUNDS = new BlockPos(300, 64, 10);

    private static BlockPos rawDelta(BlockPos anchor, BlockPos target) {
        return target.subtract(anchor);
    }

    private static Vec3 at(double x, double z) {
        return new Vec3(x, RUN_Y, z);
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
    void anUnwrappedWorldLeavesTheNearestCopyUntouched() {
        assertSame(TARGET_ACROSS, CreateSeamFold.nearest(WorldFolds.NOOP, ANCHOR, TARGET_ACROSS));
        assertSame(TARGET_ACROSS, CreateSeamFold.nearest(null, ANCHOR, TARGET_ACROSS));
        assertSame(TARGET_ACROSS, CreateSeamFold.nearestCopy((Level) null, ANCHOR, TARGET_ACROSS));
    }

    @Test
    void aRunCrossingTheTieKeepsItsShapeWhilePerPointSeatingSplitsIt() {
        Vec3 viewer = at(752.0, 10.0);
        Vec3 anchor = at(250.0, 10.0);
        Vec3 nearPoint = at(495.0, 10.0);
        Vec3 farPoint = at(500.0, 10.0);
        for (WorldFold fold : RUN_TRANSLATING) {
            Vec3 seatedNear = CreateSeamFold.inFrameOf(fold, viewer, anchor, nearPoint);
            Vec3 seatedFar = CreateSeamFold.inFrameOf(fold, viewer, anchor, farPoint);

            assertEquals(at(495.0 + WORLD_BLOCKS, 10.0), seatedNear, "in " + fold);
            assertEquals(at(500.0 + WORLD_BLOCKS, 10.0), seatedFar, "in " + fold);
            assertEquals(farPoint.subtract(nearPoint), seatedFar.subtract(seatedNear), "in " + fold);
            assertNotEquals(fold.nearestCopy(viewer, farPoint), seatedFar,
                    fold + " seated the far point where per-point seating already puts it, so nothing was asserted");
        }
    }

    @Test
    void theUnboundedAxisOfACylinderIsNotMovedByTheRunAnchor() {
        Vec3 seated = CreateSeamFold.inFrameOf(
                CYLINDER, at(752.0, 10.0), at(250.0, 10.0), at(495.0, FAR_UNBOUNDED_Z));

        assertEquals(at(495.0 + WORLD_BLOCKS, FAR_UNBOUNDED_Z), seated);
    }

    @Test
    void aRunSeatedThroughAGlideSeamCarriesTheMirroredOffset() {
        Vec3 seated = CreateSeamFold.inFrameOf(MIRRORED, at(752.0, 10.0), at(250.0, 10.0), at(495.0, 30.0));

        assertEquals(at(495.0 + WORLD_BLOCKS, 2 * MIRROR_LINE_BLOCKS - 30.0), seated);
    }

    @Test
    void aRunWhoseAnchorStaysWhereItIsHandsThePointBack() {
        Vec3 point = at(30.0, 10.0);
        for (WorldFold fold : RUN_ALL) {
            assertSame(point, CreateSeamFold.inFrameOf(fold, at(10.0, 10.0), at(20.0, 10.0), point), "in " + fold);
        }
    }
}
