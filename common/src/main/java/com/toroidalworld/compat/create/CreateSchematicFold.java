package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;

import com.simibubi.create.AllDataComponents;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

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
        WorldLoopTransformer transformer = seamTransformer(level);
        if (transformer == null) {
            return glueBox;
        }

        return transformer.foldBoxToward(scanBox.getCenter(), glueBox);
    }

    public static BlockPos scannedControllerNear(@Nullable Level level, BlockPos lastKnown, BlockPos controller) {
        return CreateSeamFold.foldClientPosition(level, lastKnown, controller);
    }

    public static boolean regionExceedsWorld(@Nullable Level level, BlockPos first, BlockPos second) {
        WorldLoopTransformer transformer = seamTransformer(level);
        if (transformer == null) {
            return false;
        }

        return transformer.exceedsWorld(BoundingBox.fromCorners(first, second));
    }

    private static @Nullable WorldLoopTransformer seamTransformer(@Nullable Level level) {
        if (level == null) {
            return null;
        }

        WorldLoopTransformer clientBounds = WorldLoopAttachments.wrappedClientBoundsTransformerOf(level);
        return clientBounds != null ? clientBounds : WorldLoopAttachments.wrappedTransformerOf(level);
    }

    private CreateSchematicFold() {
    }
}
