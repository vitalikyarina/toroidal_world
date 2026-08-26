package com.toroidalworld.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.toroidalworld.api.ToroidalShape.Orientation;
import com.toroidalworld.api.ToroidalShape.Oriented;
import com.toroidalworld.core.DeckGroupFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
        return new WorldFoldToroidalShape(WorldFolds.of(FlatShape.latticeTorus(BOTH, FlatShape.NO_SKEW)));
    }

    private static ToroidalShape mobius() {
        return new WorldFoldToroidalShape(
                new DeckGroupFold(FlatShape.mirrored(X_ONLY, Direction.Axis.Z, 0)));
    }

    private static ToroidalShape klein() {
        return new WorldFoldToroidalShape(
                new DeckGroupFold(FlatShape.mirrored(BOTH, Direction.Axis.Z, 3)));
    }

    private static ToroidalShape latticeTorus() {
        return new WorldFoldToroidalShape(new DeckGroupFold(FlatShape.latticeTorus(BOTH, 3)));
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
