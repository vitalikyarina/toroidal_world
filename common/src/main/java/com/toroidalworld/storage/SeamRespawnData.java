package com.toroidalworld.storage;

import com.toroidalworld.core.WorldLoopTransformer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class SeamRespawnData {
    public static BlockPos insideBounds(ServerLevel level, BlockPos pos) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        return transformer == null ? pos : transformer.blocks.wrap(pos);
    }

    private SeamRespawnData() {
    }
}
