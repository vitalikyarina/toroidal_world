package com.toroidalworld.engine.fold;

import com.toroidalworld.core.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

public final class ChunkSeat {
    public static BlockPos onto(WorldFold fold, ChunkPos own, BlockPos pos) {
        if (sitsIn(own, pos)) {
            return pos;
        }

        BlockPos nearest = fold.nearestCopy(own.getMiddleBlockPosition(pos.getY()), pos);
        return sitsIn(own, nearest) ? nearest : pos;
    }

    private static boolean sitsIn(ChunkPos own, BlockPos pos) {
        return SectionPos.blockToSectionCoord(pos.getX()) == own.x()
                && SectionPos.blockToSectionCoord(pos.getZ()) == own.z();
    }

    private ChunkSeat() {
    }
}
