package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.compat.create.client.CreateClientFrame;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class ControllerFrameFold {
    public static BlockPos inFrameOf(@Nullable Level level, BlockPos controller) {
        if (level == null) {
            return controller;
        }

        if (level.isClientSide) {
            return CreateClientFrame.nearestCopy(level, controller);
        }

        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        return transformer == null ? controller : transformer.fold(controller);
    }

    private ControllerFrameFold() {
    }
}
