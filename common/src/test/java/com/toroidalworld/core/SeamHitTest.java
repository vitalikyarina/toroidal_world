package com.toroidalworld.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

class SeamHitTest {
    private static final AxisBounds.Looped LOOPED = new AxisBounds.Looped(-8, 8);
    private static final AxisBounds UNBOUNDED = AxisBounds.Unbounded.INSTANCE;

    private static final WorldFold MIRRORS_Z = new DeckGroupFold(
            FlatShape.mirrored(new WorldLoopBounds(LOOPED, UNBOUNDED), Direction.Axis.Z, 0));
    private static final WorldFold MIRRORS_X = new DeckGroupFold(
            FlatShape.mirrored(new WorldLoopBounds(UNBOUNDED, LOOPED), Direction.Axis.X, 0));
    private static final WorldFold TORUS = new DeckGroupFold(
            FlatShape.latticeTorus(new WorldLoopBounds(LOOPED, LOOPED), FlatShape.NO_SKEW));
    private static final WorldFold RECTANGLE = new DeckGroupFold(FlatShape.rectangle());

    private static final BlockPos OVER_X = new BlockPos(130, 64, 40);
    private static final BlockPos OVER_Z = new BlockPos(40, 64, 130);
    private static final BlockPos INSIDE = new BlockPos(40, 64, 40);

    private static BlockHitResult hitOn(BlockPos block, Vec3 offset, Direction face) {
        return new BlockHitResult(Vec3.atLowerCornerOf(block).add(offset), face, block, false);
    }

    private static BlockHitResult reseat(WorldFold fold, BlockHitResult hit) {
        return SeamHit.reseat(hit, fold.foldOriented(hit.getBlockPos()));
    }

    private static void assertLocation(Vec3 expected, BlockHitResult hit) {
        assertEquals(expected.x, hit.getLocation().x, 1.0e-9, "x");
        assertEquals(expected.y, hit.getLocation().y, 1.0e-9, "y");
        assertEquals(expected.z, hit.getLocation().z, 1.0e-9, "z");
    }

    @Nested
    class MirrorOnZ {
        @Test
        void theOffsetInsideTheBlockIsMirroredWithIt() {
            BlockHitResult reseated = reseat(MIRRORS_Z,
                    hitOn(OVER_X, new Vec3(0.25, 0.5, 0.75), Direction.NORTH));

            assertEquals(new BlockPos(-126, 64, -41), reseated.getBlockPos());
            assertLocation(new Vec3(-125.75, 64.5, -40.75), reseated);
        }

        @Test
        void aFaceOnTheMirroredAxisTurnsAround() {
            assertEquals(Direction.SOUTH, reseat(MIRRORS_Z,
                    hitOn(OVER_X, new Vec3(0.25, 0.5, 0.75), Direction.NORTH)).getDirection());
        }

        @Test
        void aFaceOffTheMirroredAxisIsLeftAlone() {
            assertEquals(Direction.WEST, reseat(MIRRORS_Z,
                    hitOn(OVER_X, new Vec3(0.25, 0.5, 0.75), Direction.WEST)).getDirection());
            assertEquals(Direction.UP, reseat(MIRRORS_Z,
                    hitOn(OVER_X, new Vec3(0.25, 0.5, 0.75), Direction.UP)).getDirection());
        }
    }

    @Nested
    class MirrorOnX {
        @Test
        void theOffsetInsideTheBlockIsMirroredWithIt() {
            BlockHitResult reseated = reseat(MIRRORS_X,
                    hitOn(OVER_Z, new Vec3(0.75, 0.5, 0.25), Direction.WEST));

            assertEquals(new BlockPos(-41, 64, -126), reseated.getBlockPos());
            assertLocation(new Vec3(-40.75, 64.5, -125.75), reseated);
        }

        @Test
        void aFaceOnTheMirroredAxisTurnsAround() {
            assertEquals(Direction.EAST, reseat(MIRRORS_X,
                    hitOn(OVER_Z, new Vec3(0.75, 0.5, 0.25), Direction.WEST)).getDirection());
            assertEquals(Direction.NORTH, reseat(MIRRORS_X,
                    hitOn(OVER_Z, new Vec3(0.75, 0.5, 0.25), Direction.NORTH)).getDirection());
        }
    }

    @Nested
    class AgreementWithTheCoordinateFold {
        @Test
        void anOffsetInsideTheBlockLandsWhereTheCoordinateFoldPutsIt() {
            for (double offset = 0.05; offset < 1.0; offset += 0.05) {
                BlockHitResult hit = hitOn(OVER_X, new Vec3(0.25, 0.5, offset), Direction.NORTH);
                assertLocation(MIRRORS_Z.fold(hit.getLocation()), reseat(MIRRORS_Z, hit));
            }
        }

        @Test
        void aHitOnTheFarFaceFollowsItsBlockAndNotTheCoordinateLattice() {
            BlockPos lastBlock = new BlockPos(127, 64, 40);
            BlockHitResult hit = hitOn(lastBlock, new Vec3(1.0, 0.5, 0.0), Direction.EAST);

            assertSame(hit, reseat(MIRRORS_Z, hit));
            assertEquals(-128.0, MIRRORS_Z.fold(hit.getLocation()).x, 1.0e-9);
            assertEquals(-40.0, MIRRORS_Z.fold(hit.getLocation()).z, 1.0e-9);
        }
    }

    @Nested
    class WithoutAMirror {
        @Test
        void aTranslationCarriesTheOffsetUnchanged() {
            BlockHitResult reseated = reseat(TORUS,
                    hitOn(OVER_X, new Vec3(0.25, 0.5, 0.75), Direction.NORTH));

            assertEquals(new BlockPos(-126, 64, 40), reseated.getBlockPos());
            assertLocation(new Vec3(-125.75, 64.5, 40.75), reseated);
            assertEquals(Direction.NORTH, reseated.getDirection());
        }

        @Test
        void aHitWithNothingToFoldComesBackItself() {
            BlockHitResult hit = hitOn(INSIDE, new Vec3(0.25, 0.5, 0.75), Direction.NORTH);

            assertSame(hit, reseat(TORUS, hit));
            assertSame(hit, reseat(RECTANGLE, hit));
            assertSame(hit, reseat(MIRRORS_Z, hit));
        }
    }

    @Nested
    class CarriedThrough {
        @Test
        void aMissStaysAMiss() {
            BlockHitResult miss = BlockHitResult.miss(
                    Vec3.atLowerCornerOf(OVER_X).add(0.25, 0.5, 0.75), Direction.NORTH, OVER_X);
            BlockHitResult reseated = reseat(MIRRORS_Z, miss);

            assertEquals(HitResult.Type.MISS, reseated.getType());
            assertEquals(new BlockPos(-126, 64, -41), reseated.getBlockPos());
            assertEquals(Direction.SOUTH, reseated.getDirection());
        }

        @Test
        void theInsideFlagSurvivesTheMirror() {
            BlockHitResult hit = new BlockHitResult(
                    Vec3.atLowerCornerOf(OVER_X).add(0.25, 0.5, 0.75), Direction.NORTH, OVER_X, true);
            BlockHitResult reseated = reseat(MIRRORS_Z, hit);

            assertTrue(reseated.isInside());
            assertFalse(reseated.getType() == HitResult.Type.MISS);
        }
    }
}
