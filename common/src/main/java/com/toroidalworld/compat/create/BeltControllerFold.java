package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class BeltControllerFold {
    public static BlockPos inFrameOf(@Nullable Level level, BlockPos worldPosition, BlockPos controller) {
        if (level == null) {
            return controller;
        }

        if (level.isClientSide) {
            WorldLoopTransformer clientTransformer = WorldLoopAttachments.wrappedClientBoundsTransformerOf(level);
            return clientTransformer == null
                    ? controller
                    : clientTransformer.blocks.nearestCopy(worldPosition, controller);
        }

        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        return transformer == null ? controller : transformer.blocks.wrap(controller);
    }

    private BeltControllerFold() {
    }
}
