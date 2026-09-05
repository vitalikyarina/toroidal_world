package com.toroidalworld.compat.create;

import static com.toroidalworld.compat.CompatFoldFixture.CYLINDER;
import static com.toroidalworld.compat.CompatFoldFixture.DECK_TORUS;
import static com.toroidalworld.compat.CompatFoldFixture.MIRRORED;
import static com.toroidalworld.compat.CompatFoldFixture.PER_AXIS;
import static com.toroidalworld.compat.CompatFoldFixture.SKEWED;
import static com.toroidalworld.compat.CompatFoldFixture.WORLD_BLOCKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

class CreateSchematicFoldTest {
    private static final int FAR_UNBOUNDED_Z = 100000;

    private static final List<WorldFold> TRANSLATING = List.of(PER_AXIS, DECK_TORUS, SKEWED, CYLINDER);

    private static final List<WorldFold> ALL = List.of(PER_AXIS, DECK_TORUS, SKEWED, MIRRORED, CYLINDER);

    private static final DataComponentType<BlockPos> SCHEMATIC_ANCHOR =
            DataComponentType.<BlockPos>builder().persistent(BlockPos.CODEC).build();

    private static final BlockPos REGION_CORNER = new BlockPos(0, 64, 0);
    private static final BlockPos REFERENCE = new BlockPos(250, 64, 10);
    private static final BlockPos ACROSS_THE_SEAM = new BlockPos(-254, 64, 10);
    private static final BlockPos BESIDE_THE_REFERENCE = new BlockPos(240, 64, 10);

    private static ItemStack blueprintAnchoredAt(BlockPos anchor) {
        ItemStack blueprint = new ItemStack(Items.PAPER);
        blueprint.set(SCHEMATIC_ANCHOR, anchor);
        return blueprint;
    }

    private static BlockPos nearestCopyOfTheSeamCrossing() {
        return ACROSS_THE_SEAM.offset(WORLD_BLOCKS, 0, 0);
    }

    @Test
    void aCornerPairOneLapApartFoldsTheRegionOntoItself() {
        BlockPos opposite = REGION_CORNER.offset(WORLD_BLOCKS, 0, WORLD_BLOCKS);
        for (WorldFold fold : ALL) {
            assertTrue(CreateSchematicFold.regionExceedsWorld(fold, REGION_CORNER, opposite), "in " + fold);
        }
    }

    @Test
    void aCornerPairOneBlockShortOfALapHoldsEveryBlockOnce() {
        BlockPos opposite = REGION_CORNER.offset(WORLD_BLOCKS - 1, 0, WORLD_BLOCKS - 1);
        for (WorldFold fold : ALL) {
            assertFalse(CreateSchematicFold.regionExceedsWorld(fold, REGION_CORNER, opposite), "in " + fold);
        }
    }

    @Test
    void theTwoCornersMayArriveInEitherOrder() {
        BlockPos opposite = REGION_CORNER.offset(WORLD_BLOCKS, 0, WORLD_BLOCKS);
        for (WorldFold fold : ALL) {
            assertTrue(CreateSchematicFold.regionExceedsWorld(fold, opposite, REGION_CORNER), "in " + fold);
        }
    }

    @Test
    void theUnboundedAxisOfACylinderNeverFoldsOntoItself() {
        BlockPos first = new BlockPos(0, 64, -FAR_UNBOUNDED_Z);
        BlockPos second = new BlockPos(WORLD_BLOCKS - 1, 64, FAR_UNBOUNDED_Z);

        assertFalse(CreateSchematicFold.regionExceedsWorld(CYLINDER, first, second));
        assertTrue(CreateSchematicFold.regionExceedsWorld(PER_AXIS, first, second));
    }

    @Test
    void anUnwrappedWorldRefusesNoRegionAtAll() {
        BlockPos opposite = REGION_CORNER.offset(4 * WORLD_BLOCKS, 0, 4 * WORLD_BLOCKS);

        assertFalse(CreateSchematicFold.regionExceedsWorld(WorldFolds.NOOP, REGION_CORNER, opposite));
        assertFalse(CreateSchematicFold.regionExceedsWorld((WorldFold) null, REGION_CORNER, opposite));
        assertFalse(CreateSchematicFold.regionExceedsWorld((Level) null, REGION_CORNER, opposite));
    }

    @Test
    void anAnchorAcrossTheSeamComesBackBesideTheReference() {
        for (WorldFold fold : TRANSLATING) {
            ItemStack rebased = CreateSchematicFold.anchoredNear(fold, SCHEMATIC_ANCHOR, REFERENCE,
                    blueprintAnchoredAt(ACROSS_THE_SEAM));

            assertEquals(nearestCopyOfTheSeamCrossing(), rebased.get(SCHEMATIC_ANCHOR), "in " + fold);
        }
    }

    @Test
    void theAnchorLandsOnWhicheverCopyTheFoldNames() {
        for (WorldFold fold : ALL) {
            ItemStack rebased = CreateSchematicFold.anchoredNear(fold, SCHEMATIC_ANCHOR, REFERENCE,
                    blueprintAnchoredAt(ACROSS_THE_SEAM));

            assertEquals(fold.nearestCopy(REFERENCE, ACROSS_THE_SEAM), rebased.get(SCHEMATIC_ANCHOR), "in " + fold);
            assertNotEquals(ACROSS_THE_SEAM, rebased.get(SCHEMATIC_ANCHOR), "in " + fold);
        }
    }

    @Test
    void aBlueprintWithNoAnchorIsGivenBackByIdentity() {
        ItemStack blueprint = new ItemStack(Items.PAPER);
        for (WorldFold fold : ALL) {
            assertSame(blueprint,
                    CreateSchematicFold.anchoredNear(fold, SCHEMATIC_ANCHOR, REFERENCE, blueprint), "in " + fold);
        }
    }

    @Test
    void anAnchorAlreadyBesideTheReferenceIsGivenBackByIdentity() {
        ItemStack blueprint = blueprintAnchoredAt(BESIDE_THE_REFERENCE);
        for (WorldFold fold : ALL) {
            assertSame(blueprint,
                    CreateSchematicFold.anchoredNear(fold, SCHEMATIC_ANCHOR, REFERENCE, blueprint), "in " + fold);
        }
    }

    @Test
    void anUnwrappedWorldLeavesTheBlueprintUntouched() {
        ItemStack blueprint = blueprintAnchoredAt(ACROSS_THE_SEAM);

        assertSame(blueprint,
                CreateSchematicFold.anchoredNear(WorldFolds.NOOP, SCHEMATIC_ANCHOR, REFERENCE, blueprint));
        assertSame(blueprint,
                CreateSchematicFold.anchoredNear((WorldFold) null, SCHEMATIC_ANCHOR, REFERENCE, blueprint));
    }

    @Test
    void aVisitedPositionComesBackInTheFrameOfTheAnchor() {
        ItemStack blueprint = blueprintAnchoredAt(REFERENCE);
        for (WorldFold fold : TRANSLATING) {
            BlockPos visited =
                    CreateSchematicFold.visitedInSchematicFrame(fold, SCHEMATIC_ANCHOR, blueprint, ACROSS_THE_SEAM);

            assertEquals(nearestCopyOfTheSeamCrossing(), visited, "in " + fold);
        }
    }

    @Test
    void theVisitedPositionLandsOnWhicheverCopyTheFoldNames() {
        ItemStack blueprint = blueprintAnchoredAt(REFERENCE);
        for (WorldFold fold : ALL) {
            BlockPos visited =
                    CreateSchematicFold.visitedInSchematicFrame(fold, SCHEMATIC_ANCHOR, blueprint, ACROSS_THE_SEAM);

            assertEquals(fold.nearestCopy(REFERENCE, ACROSS_THE_SEAM), visited, "in " + fold);
            assertNotEquals(ACROSS_THE_SEAM, visited, "in " + fold);
        }
    }

    @Test
    void aBlueprintWithNoAnchorLeavesTheVisitedPositionAlone() {
        ItemStack blueprint = new ItemStack(Items.PAPER);
        for (WorldFold fold : ALL) {
            assertSame(ACROSS_THE_SEAM, CreateSchematicFold.visitedInSchematicFrame(fold, SCHEMATIC_ANCHOR,
                    blueprint, ACROSS_THE_SEAM), "in " + fold);
        }
    }

    @Test
    void anUnwrappedWorldLeavesTheVisitedPositionAlone() {
        ItemStack blueprint = blueprintAnchoredAt(REFERENCE);

        assertSame(ACROSS_THE_SEAM, CreateSchematicFold.visitedInSchematicFrame(WorldFolds.NOOP, SCHEMATIC_ANCHOR,
                blueprint, ACROSS_THE_SEAM));
        assertSame(ACROSS_THE_SEAM, CreateSchematicFold.visitedInSchematicFrame((WorldFold) null, SCHEMATIC_ANCHOR,
                blueprint, ACROSS_THE_SEAM));
    }
}
