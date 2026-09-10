package com.toroidalworld.api.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.toroidalworld.api.v1.ToroidalShape.Orientation;
import com.toroidalworld.api.v1.ToroidalShape.Oriented;
import com.toroidalworld.core.DeckGroupFold;
import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldLoopBounds;
import com.toroidalworld.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

class ToroidalShapeContractTest {
    private static final int UNIT = 16;
    private static final int MIN_CHUNK = -8;
    private static final int MAX_CHUNK = 8;
    private static final int WIDTH = (MAX_CHUNK - MIN_CHUNK) * UNIT;
    private static final int LOWER = MIN_CHUNK * UNIT;

    private static final AxisBounds.Looped LOOPED = new AxisBounds.Looped(MIN_CHUNK, MAX_CHUNK);
    private static final WorldLoopBounds BOTH = new WorldLoopBounds(LOOPED, LOOPED);
    private static final WorldLoopBounds X_ONLY = new WorldLoopBounds(LOOPED, AxisBounds.Unbounded.INSTANCE);

    private static ToroidalShape torus() {
        return TestShapes.of(WorldFolds.of(FlatShape.torus(BOTH)));
    }

    private static ToroidalShape mobius() {
        return TestShapes.of(
                new DeckGroupFold(FlatShape.mirrored(X_ONLY, Direction.Axis.Z, 0)));
    }

    private static ToroidalShape klein() {
        return TestShapes.of(
                new DeckGroupFold(FlatShape.mirrored(BOTH, Direction.Axis.Z, 3)));
    }

    private static ToroidalShape latticeTorus() {
        return TestShapes.of(new DeckGroupFold(FlatShape.latticeTorus(BOTH, 3)));
    }

    @Nested
    class TheTorusIsUnchanged {
        @Test
        void itDecomposesPerAxisAndKeepsLocalIndices() {
            ToroidalShape shape = torus();
            assertTrue(shape.decomposesPerAxis(), "the torus stopped decomposing per axis");
            assertTrue(shape.preservesLocalIndices(), "the torus stopped preserving local indices");
            assertTrue(shape.loops(Direction.Axis.X), "the torus stopped looping x");
            assertFalse(shape.loops(Direction.Axis.Y), "y loops");
            assertEquals(MIN_CHUNK, shape.minChunk(Direction.Axis.X), "the first chunk moved");
            assertEquals(WIDTH, shape.widthBlocks(Direction.Axis.Z), "the world width moved");
        }

        @Test
        void everyFoldStillReportsTheIdentity() {
            ToroidalShape shape = torus();
            Random random = new Random(0x7150L);
            for (int sample = 0; sample < 200; sample++) {
                int x = LOWER - 3 * WIDTH + random.nextInt(6 * WIDTH);
                int z = LOWER - 3 * WIDTH + random.nextInt(6 * WIDTH);
                Oriented<BlockPos> folded = shape.foldOriented(new BlockPos(x, 64, z));
                assertTrue(folded.isIdentity(), "an unmirrored fold reported a flip");
                assertEquals(shape.fold(new BlockPos(x, 64, z)), folded.value(),
                        "the oriented fold disagrees with the plain one");
            }
        }

        @Test
        void theOldPerAxisMembersStillAnswer() {
            ToroidalShape shape = torus();
            assertEquals(LOWER, shape.foldBlock(Direction.Axis.X, LOWER + WIDTH), "the per-axis block fold moved");
            assertEquals(MIN_CHUNK, shape.foldChunk(Direction.Axis.Z, MAX_CHUNK), "the per-axis chunk fold moved");
            assertEquals(5.5, shape.foldCoord(Direction.Axis.Y, 5.5), "y stopped passing through");
            assertEquals(1.5, shape.nearestCoord(Direction.Axis.Y, 900.0, 1.5), "y stopped passing through");
        }
    }

    @Nested
    class ACoupledShapeClosesThePerAxisDoor {
        @Test
        void theSkewedLatticeAndTheBottleRefuseThePerAxisMembers() {
            for (ToroidalShape shape : new ToroidalShape[] {latticeTorus(), klein(), mobius()}) {
                assertFalse(shape.decomposesPerAxis(), "a coupled shape claims to decompose per axis");
                assertThrows(IllegalStateException.class, () -> shape.foldBlock(Direction.Axis.X, 900),
                        "the per-axis block fold answered on a coupled shape");
                assertThrows(IllegalStateException.class, () -> shape.foldChunk(Direction.Axis.Z, 90),
                        "the per-axis chunk fold answered on a coupled shape");
                assertThrows(IllegalStateException.class, () -> shape.foldCoord(Direction.Axis.X, 900.0),
                        "the per-axis coordinate fold answered on a coupled shape");
                assertThrows(IllegalStateException.class,
                        () -> shape.nearestCoord(Direction.Axis.Z, 0.0, 900.0),
                        "the per-axis nearest coordinate answered on a coupled shape");
            }
        }

        @Test
        void theWholePositionFoldsStillAnswerThere() {
            for (ToroidalShape shape : new ToroidalShape[] {latticeTorus(), klein(), mobius()}) {
                BlockPos folded = shape.fold(new BlockPos(LOWER + 3 * WIDTH, 64, LOWER + 5));
                assertTrue(folded.getX() >= LOWER && folded.getX() < LOWER + WIDTH,
                        "the whole-position fold left the world on x");
            }
        }

        @Test
        void yStillPassesThroughEvenThere() {
            ToroidalShape shape = klein();
            assertEquals(5.5, shape.foldCoord(Direction.Axis.Y, 5.5), "y stopped passing through on a bottle");
            assertEquals(7, shape.foldBlock(Direction.Axis.Y, 7), "y stopped passing through on a bottle");
            assertFalse(shape.loops(Direction.Axis.Y), "y loops on a bottle");
        }

        @Test
        void theExtentsStillAnswerBecauseTheDomainIsStillARectangle() {
            ToroidalShape shape = latticeTorus();
            assertEquals(MIN_CHUNK, shape.minChunk(Direction.Axis.X), "a coupled shape lost its x extent");
            assertEquals(WIDTH, shape.widthBlocks(Direction.Axis.Z), "a coupled shape lost its z width");
            assertThrows(IllegalArgumentException.class, () -> shape.minChunk(Direction.Axis.Y),
                    "y reported an extent");
        }
    }

    @Nested
    class SeatingABox {
        @Test
        void aBoxAWorldAwayComesBackBesideTheReference() {
            ToroidalShape shape = torus();
            AABB seated = shape.nearestCopy(new Vec3(LOWER + WIDTH - 7.5, 64.0, 0.5),
                    new AABB(LOWER + 6.0, 60.0, -1.0, LOWER + 10.0, 62.0, 1.0));

            assertEquals(new AABB(LOWER + WIDTH + 6.0, 60.0, -1.0, LOWER + WIDTH + 10.0, 62.0, 1.0), seated,
                    "the box was not carried into the copy beside the reference");
        }

        @Test
        void aBoxAlreadyNearestIsHandedBack() {
            ToroidalShape shape = torus();
            AABB box = new AABB(LOWER + 6.0, 60.0, -1.0, LOWER + 10.0, 62.0, 1.0);

            assertSame(box, shape.nearestCopy(new Vec3(LOWER + 8.0, 64.0, 0.5), box),
                    "a box that moved nothing was rebuilt");
        }

        @Test
        void aBoxStraddlingTheSeamStaysWhole() {
            ToroidalShape shape = torus();
            AABB box = new AABB(LOWER + WIDTH - 2.0, 60.0, -1.0, LOWER + WIDTH + 2.0, 62.0, 1.0);
            AABB seated = shape.nearestCopy(new Vec3(LOWER + WIDTH - 4.0, 64.0, 0.5), box);

            assertSame(box, seated, "a box across the seam was cut into the pieces the world would make of it");
        }

        @Test
        void aBoxCarriedAcrossAMirroredSeamKeepsItsSizeAndReportsTheFlip() {
            ToroidalShape shape = mobius();
            Vec3 ref = new Vec3(LOWER + WIDTH - 1.5, 64.0, 100.5);
            AABB box = new AABB(LOWER + 0.5, 60.0, -102.5, LOWER + 2.5, 62.0, -98.5);
            Oriented<AABB> seated = shape.nearestCopyOriented(ref, box);

            assertTrue(seated.orientation().flipsZ(), "the flipped copy did not report its flip");
            assertEquals(shape.nearestCopy(ref, box), seated.value(),
                    "the oriented seating disagrees with the plain one");
            assertEquals(box.getXsize(), seated.value().getXsize(), "the box changed width across the seam");
            assertEquals(box.getZsize(), seated.value().getZsize(), "the box changed depth across the seam");
            assertEquals(box.getYsize(), seated.value().getYsize(), "the box changed height across the seam");
        }
    }

    @Nested
    class ShiftingARigidGroup {
        @Test
        void oneShiftHoldsTogetherAGroupThatSeatingApartWouldTear() {
            ToroidalShape shape = torus();
            Vec3 ref = new Vec3(LOWER + WIDTH - 7.5, 64.0, 0.5);
            Vec3 anchor = new Vec3(-6.0, 64.0, 0.5);
            Vec3 member = new Vec3(-10.0, 64.0, 0.5);
            double apart = anchor.x - member.x;

            assertNotEquals(apart, shape.nearestCopy(ref, anchor).x - shape.nearestCopy(ref, member).x,
                    "the pair no longer straddles half a world width, so it proves nothing");

            SeamShift shift = shape.shiftToNearestCopy(ref, anchor);
            assertEquals(apart, shift.apply(anchor).x - shift.apply(member).x, "one shift tore the group");
            assertEquals(shape.nearestCopy(ref, anchor), shift.apply(anchor),
                    "the shift lands the anchor somewhere else than seating it does");
        }

        @Test
        void aGroupAlreadyInTheRightCopyGetsTheIdentity() {
            ToroidalShape shape = torus();
            Vec3 member = new Vec3(6.0, 64.0, 0.5);
            SeamShift shift = shape.shiftToNearestCopy(new Vec3(0.0, 64.0, 0.5), new Vec3(2.0, 64.0, 0.5));

            assertTrue(shift.isIdentity(), "a group already in the right copy was moved");
            assertTrue(shift.orientation().isIdentity(), "an identity shift reported a turn");
            assertSame(member, shift.apply(member), "an identity shift rebuilt its argument");
        }

        @Test
        void theBlockGridFormAgreesWithSeatingTheAnchor() {
            ToroidalShape shape = torus();
            BlockPos ref = new BlockPos(LOWER + WIDTH - 8, 64, 0);
            BlockPos anchor = new BlockPos(-6, 64, 0);
            BlockPos member = new BlockPos(-10, 64, 0);
            SeamShift shift = shape.shiftToNearestCopy(ref, anchor);

            assertEquals(shape.nearestCopy(ref, anchor), shift.apply(anchor),
                    "the shift lands the anchor somewhere else than seating it does");
            assertEquals(anchor.getX() - member.getX(), shift.apply(anchor).getX() - shift.apply(member).getX(),
                    "one shift tore the group on the block grid");
        }

        @Test
        void aShiftAcrossAMirroredSeamCarriesItsTurnAndKeepsABoxRigid() {
            ToroidalShape shape = mobius();
            Vec3 ref = new Vec3(LOWER + WIDTH - 1.5, 64.0, 100.5);
            Vec3 anchor = new Vec3(LOWER + 1.5, 64.0, -100.5);
            SeamShift shift = shape.shiftToNearestCopy(ref, anchor);

            assertTrue(shift.orientation().flipsZ(), "the shift across a mirrored seam reported no turn");
            assertEquals(shape.nearestCopy(ref, anchor), shift.apply(anchor),
                    "the shift lands the anchor somewhere else than seating it does");

            AABB box = new AABB(LOWER + 0.5, 60.0, -102.5, LOWER + 2.5, 62.0, -98.5);
            AABB moved = shift.apply(box);
            assertEquals(box.getXsize(), moved.getXsize(), "the box changed width under the shift");
            assertEquals(box.getZsize(), moved.getZsize(), "the box changed depth under the shift");
        }
    }

    @Nested
    class Mirroring {
        @Test
        void aLapAcrossAMirroredSeamIsReported() {
            ToroidalShape shape = mobius();
            int inside = LOWER + WIDTH / 2;
            Oriented<BlockPos> oneLap = shape.foldOriented(new BlockPos(inside + WIDTH, 64, inside));
            assertFalse(oneLap.isIdentity(), "one lap across a mirrored seam reported no flip");
            assertTrue(oneLap.orientation().flipsZ(), "the flip is not on z");
            assertFalse(oneLap.orientation().flipsX(), "the glide axis was flipped");
            assertFalse(oneLap.orientation().preservesHandedness(), "a single mirror kept handedness");

            Oriented<BlockPos> twoLaps = shape.foldOriented(new BlockPos(inside + 2 * WIDTH, 64, inside));
            assertTrue(twoLaps.isIdentity(), "two laps did not come back upright");
        }

        @Test
        void theNearestCopyAcrossAMirroredSeamCarriesItsOrientation() {
            ToroidalShape shape = mobius();
            Oriented<Vec3> nearest = shape.nearestCopyOriented(
                    new Vec3(LOWER + WIDTH - 1.5, 64.0, 100.5), new Vec3(LOWER + 1.5, 64.0, -100.5));
            assertTrue(nearest.orientation().flipsZ(), "the flipped copy did not report its flip");
            assertEquals(shape.nearestCopy(new Vec3(LOWER + WIDTH - 1.5, 64.0, 100.5),
                            new Vec3(LOWER + 1.5, 64.0, -100.5)),
                    nearest.value(), "the oriented nearest copy disagrees with the plain one");
        }

        @Test
        void aDeltaCarriedAcrossTheSeamIsTurnedWithIt() {
            Orientation flipped = new Orientation(false, true);
            assertEquals(new Vec3(1.0, 2.0, -3.0), flipped.applyToDelta(new Vec3(1.0, 2.0, 3.0)),
                    "a mirrored delta was not turned");

            Vec3 delta = new Vec3(1.0, 2.0, 3.0);
            assertSame(delta, Orientation.IDENTITY.applyToDelta(delta), "an upright delta was rebuilt");
            assertTrue(Orientation.IDENTITY.isIdentity(), "the identity orientation is not identity");
            assertTrue(new Orientation(true, true).preservesHandedness(), "a half turn reverses handedness");
        }

        @Test
        void onlyAMirroredShapeLosesItsLocalIndices() {
            assertTrue(torus().preservesLocalIndices(), "the torus lost its local indices");
            assertTrue(latticeTorus().preservesLocalIndices(), "a skew lost the local indices");
            assertFalse(mobius().preservesLocalIndices(), "a band kept its local indices");
            assertFalse(klein().preservesLocalIndices(), "a bottle kept its local indices");
        }
    }
}
