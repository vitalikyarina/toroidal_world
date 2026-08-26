package com.toroidalworld.entity;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class SeamBlockName {
    public static BlockPos canonical(Level level, BlockPos pos) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        return transformer == null ? pos : transformer.fold(pos);
    }

    private SeamBlockName() {
    }
}
