package com.toroidalworld.compat.create;

import static com.toroidalworld.compat.CompatFoldFixture.DECK_TORUS;
import static com.toroidalworld.compat.CompatFoldFixture.MIRRORED;
import static com.toroidalworld.compat.CompatFoldFixture.PER_AXIS;
import static com.toroidalworld.compat.CompatFoldFixture.SKEWED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.simibubi.create.api.packager.InventoryIdentifier;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

class CreateInventoryFoldTest {
    private static final List<WorldFold> ALL = List.of(PER_AXIS, DECK_TORUS, SKEWED, MIRRORED);

    private static final BlockPos PAST_THE_BOUNDS = new BlockPos(300, 64, 10);
    private static final BlockPos ALSO_PAST_THE_BOUNDS = new BlockPos(302, 64, 10);
    private static final BlockPos INSIDE = new BlockPos(10, 64, 10);

    private static BlockFace face(BlockPos pos) {
        return new BlockFace(pos, Direction.NORTH);
    }

    @Test
    void createItselfRefusesAFaceNamedPastTheBounds() {
        InventoryIdentifier canonical = new InventoryIdentifier.Single(PER_AXIS.fold(PAST_THE_BOUNDS));

        assertFalse(canonical.contains(face(PAST_THE_BOUNDS)));
    }

    @Test
    void aFoldedSingleAnswersForEitherNamingOfItsBlock() {
        for (WorldFold fold : ALL) {
            InventoryIdentifier folded = CreateInventoryFold.fold(fold, new InventoryIdentifier.Single(
                    PAST_THE_BOUNDS));

            assertTrue(folded.contains(face(PAST_THE_BOUNDS)), "raw naming in " + fold);
            assertTrue(folded.contains(face(fold.fold(PAST_THE_BOUNDS))), "canonical naming in " + fold);
            assertFalse(folded.contains(face(INSIDE)), "a different block in " + fold);
        }
    }

    @Test
    void aFoldedMultiFaceKeepsItsSidesAndAnswersForEitherNaming() {
        for (WorldFold fold : ALL) {
            InventoryIdentifier folded = CreateInventoryFold.fold(fold, new InventoryIdentifier.MultiFace(
                    PAST_THE_BOUNDS, Set.of(Direction.NORTH, Direction.UP)));

            assertTrue(folded.contains(face(PAST_THE_BOUNDS)), "raw naming in " + fold);
            assertTrue(folded.contains(face(fold.fold(PAST_THE_BOUNDS))), "canonical naming in " + fold);
            assertFalse(folded.contains(new BlockFace(PAST_THE_BOUNDS, Direction.SOUTH)), "an absent side in " + fold);
        }
    }

    @Test
    void aFoldedPairAnswersForBothOfItsBlocks() {
        for (WorldFold fold : ALL) {
            InventoryIdentifier folded = CreateInventoryFold.fold(fold, new InventoryIdentifier.Pair(
                    PAST_THE_BOUNDS, ALSO_PAST_THE_BOUNDS));

            assertTrue(folded.contains(face(PAST_THE_BOUNDS)), "first, raw naming in " + fold);
            assertTrue(folded.contains(face(ALSO_PAST_THE_BOUNDS)), "second, raw naming in " + fold);
            assertTrue(folded.contains(face(fold.fold(ALSO_PAST_THE_BOUNDS))), "second, canonical in " + fold);
            assertFalse(folded.contains(face(INSIDE)), "a different block in " + fold);
        }
    }

    @Test
    void aBoundsCrossingTheSeamAnswersForBlocksOnEitherSide() {
        BoundingBox crossing = new BoundingBox(250, 60, 0, 260, 70, 5);
        for (WorldFold fold : ALL) {
            assertTrue(fold.crossesBounds(crossing), "the fixture must cross the seam in " + fold);
            InventoryIdentifier folded = CreateInventoryFold.fold(fold, new InventoryIdentifier.Bounds(crossing));

            assertTrue(folded.contains(face(new BlockPos(252, 65, 2))), "the inbounds side in " + fold);
            assertTrue(folded.contains(face(new BlockPos(258, 65, 2))), "the wrapped side in " + fold);
            assertFalse(folded.contains(face(new BlockPos(270, 65, 2))), "past the box in " + fold);
        }
    }

    @Test
    void aBoundsInsideTheWorldIsLeftAsCreateWroteIt() {
        BoundingBox inside = new BoundingBox(0, 60, 0, 10, 70, 5);
        for (WorldFold fold : ALL) {
            InventoryIdentifier folded = CreateInventoryFold.fold(fold, new InventoryIdentifier.Bounds(inside));

            assertTrue(folded.contains(face(new BlockPos(5, 65, 2))), "in " + fold);
            assertFalse(folded.contains(face(new BlockPos(20, 65, 2))), "in " + fold);
        }
    }

    @Test
    void twoIdentitiesOfOneBlockAreEqualWhicheverTransformerFoldedThem() {
        InventoryIdentifier single = new InventoryIdentifier.Single(PAST_THE_BOUNDS);
        InventoryIdentifier folded = CreateInventoryFold.fold(PER_AXIS, single);
        InventoryIdentifier alsoFolded = CreateInventoryFold.fold(DECK_TORUS, single);

        assertEquals(folded, alsoFolded);
        assertEquals(folded.hashCode(), alsoFolded.hashCode());
    }

    @Test
    void anIdentityKeepsTheCanonicalNameItPrintsWith() {
        InventoryIdentifier canonical = new InventoryIdentifier.Single(PER_AXIS.fold(PAST_THE_BOUNDS));
        InventoryIdentifier folded = CreateInventoryFold.fold(PER_AXIS, new InventoryIdentifier.Single(
                PAST_THE_BOUNDS));

        assertEquals(canonical.toString(), folded.toString());
    }

    @Test
    void anUnwrappedWorldAnswersWhatCreateWouldHaveAnswered() {
        InventoryIdentifier single = new InventoryIdentifier.Single(PAST_THE_BOUNDS);
        InventoryIdentifier folded = CreateInventoryFold.fold(WorldFolds.NOOP, single);

        assertTrue(folded.contains(face(PAST_THE_BOUNDS)));
        assertFalse(folded.contains(face(INSIDE)));
    }

    @Test
    void aLevelCarryingNoTransformerGivesTheIdentifierBackByIdentity() {
        InventoryIdentifier single = new InventoryIdentifier.Single(PAST_THE_BOUNDS);

        assertSame(single, CreateInventoryFold.fold((WorldFold) null, single));
    }

    @Test
    void aMissingIdentifierStaysMissing() {
        assertNull(CreateInventoryFold.fold(PER_AXIS, null));
        assertNull(CreateInventoryFold.fold((WorldFold) null, null));
    }
}
