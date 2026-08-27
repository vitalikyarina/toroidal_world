package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;

import com.simibubi.create.AllDataComponents;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

public final class CreateSchematicFold {
    public static ItemStack anchoredNear(@Nullable Level level, BlockPos reference, ItemStack blueprint) {
        BlockPos anchor = blueprint.get(AllDataComponents.SCHEMATIC_ANCHOR);
        if (anchor == null) {
            return blueprint;
        }

        BlockPos folded = CreateSeamFold.foldPosition(level, reference, anchor);
        if (folded.equals(anchor)) {
            return blueprint;
        }

        ItemStack rebased = blueprint.copy();
        rebased.set(AllDataComponents.SCHEMATIC_ANCHOR, folded);
        return rebased;
    }

    public static BlockPos visitedInSchematicFrame(@Nullable Level level, ItemStack blueprint, BlockPos visited) {
        BlockPos anchor = blueprint.get(AllDataComponents.SCHEMATIC_ANCHOR);
        if (anchor == null) {
            return visited;
        }

        return CreateSeamFold.foldPosition(level, anchor, visited);
    }

    public static AABB glueInScanFrame(@Nullable Level level, AABB scanBox, AABB glueBox) {
        WorldFold transformer = seamTransformer(level);
        if (transformer == null) {
            return glueBox;
        }

        return transformer.foldBox(scanBox.getCenter(), glueBox).value();
    }

    public static BlockPos scannedControllerNear(@Nullable Level level, BlockPos lastKnown, BlockPos controller) {
        return CreateSeamFold.foldPosition(level, lastKnown, controller);
    }

    public static boolean regionExceedsWorld(@Nullable Level level, BlockPos first, BlockPos second) {
        WorldFold transformer = seamTransformer(level);
        if (transformer == null) {
            return false;
        }

        BoundingBox region = BoundingBox.fromCorners(first, second);
        return transformer.blockDomain(Direction.Axis.X).exceedsWorld(region.minX(), region.maxX())
                || transformer.blockDomain(Direction.Axis.Z).exceedsWorld(region.minZ(), region.maxZ());
    }

    private static @Nullable WorldFold seamTransformer(@Nullable Level level) {
        return level == null ? null : WorldLoopAttachments.wrappedTransformerOfReader(level);
    }

    private CreateSchematicFold() {
    }
}
