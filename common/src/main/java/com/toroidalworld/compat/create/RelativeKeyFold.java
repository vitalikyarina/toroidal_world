package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;

public final class RelativeKeyFold {
    public static BlockPos shortWay(@Nullable Level level, BlockPos owner, Vec3i partner, BlockPos rawKey) {
        if (level == null) {
            return rawKey;
        }

        return shortWay(WorldLoopAttachments.wrappedTransformerOfReader(level), owner, partner, rawKey);
    }

    static BlockPos shortWay(@Nullable WorldFold transformer, BlockPos owner, Vec3i partner, BlockPos rawKey) {
        BlockPos partnerPos = new BlockPos(partner);
        BlockPos nearest = CreateSeamFold.nearest(transformer, owner, partnerPos);
        if (nearest.equals(partnerPos)) {
            return rawKey;
        }

        return owner.subtract(nearest);
    }

    public static BlockPos normalize(@Nullable Level level, BlockPos owner, BlockPos storedKey) {
        return shortWay(level, owner, owner.subtract(storedKey), storedKey);
    }

    static BlockPos normalize(@Nullable WorldFold transformer, BlockPos owner, BlockPos storedKey) {
        return shortWay(transformer, owner, owner.subtract(storedKey), storedKey);
    }

    private RelativeKeyFold() {
    }
}
