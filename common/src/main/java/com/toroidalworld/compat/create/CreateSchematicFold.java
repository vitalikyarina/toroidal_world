package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;

import com.simibubi.create.AllDataComponents;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

public final class CreateSchematicFold {
    public static ItemStack anchoredNear(@Nullable Level level, BlockPos reference, ItemStack blueprint) {
        return anchoredNear(seamTransformer(level), AllDataComponents.SCHEMATIC_ANCHOR, reference, blueprint);
    }

    static ItemStack anchoredNear(@Nullable WorldFold transformer, DataComponentType<BlockPos> anchorComponent,
            BlockPos reference, ItemStack blueprint) {
        BlockPos anchor = blueprint.get(anchorComponent);
        if (anchor == null) {
            return blueprint;
        }

        BlockPos folded = CreateSeamFold.nearest(transformer, reference, anchor);
        if (folded.equals(anchor)) {
            return blueprint;
        }

        ItemStack rebased = blueprint.copy();
        rebased.set(anchorComponent, folded);
        return rebased;
    }

    public static BlockPos visitedInSchematicFrame(@Nullable Level level, ItemStack blueprint, BlockPos visited) {
        return visitedInSchematicFrame(seamTransformer(level), AllDataComponents.SCHEMATIC_ANCHOR, blueprint,
                visited);
    }

    static BlockPos visitedInSchematicFrame(@Nullable WorldFold transformer,
            DataComponentType<BlockPos> anchorComponent, ItemStack blueprint, BlockPos visited) {
        BlockPos anchor = blueprint.get(anchorComponent);
        if (anchor == null) {
            return visited;
        }

        return CreateSeamFold.nearest(transformer, anchor, visited);
    }

    public static AABB glueInScanFrame(@Nullable Level level, AABB scanBox, AABB glueBox) {
        WorldFold transformer = seamTransformer(level);
        if (transformer == null) {
            return glueBox;
        }

        return transformer.foldBox(scanBox.getCenter(), glueBox).value();
    }

    public static BlockPos scannedControllerNear(@Nullable Level level, BlockPos lastKnown, BlockPos controller) {
        return CreateSeamFold.nearestCopy(level, lastKnown, controller);
    }

    public static boolean regionExceedsWorld(@Nullable Level level, BlockPos first, BlockPos second) {
        return regionExceedsWorld(seamTransformer(level), first, second);
    }

    static boolean regionExceedsWorld(@Nullable WorldFold transformer, BlockPos first, BlockPos second) {
        if (transformer == null) {
            return false;
        }

        return transformer.foldsOntoItself(BoundingBox.fromCorners(first, second));
    }

    private static @Nullable WorldFold seamTransformer(@Nullable Level level) {
        return level == null ? null : WorldLoopAttachments.wrappedTransformerOfReader(level);
    }

    private CreateSchematicFold() {
    }
}
