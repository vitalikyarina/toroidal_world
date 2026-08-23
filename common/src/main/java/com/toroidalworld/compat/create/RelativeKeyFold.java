package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;

public final class RelativeKeyFold {
    public static BlockPos shortWay(@Nullable Level level, BlockPos owner, Vec3i partner, BlockPos rawKey) {
        BlockPos partnerPos = new BlockPos(partner);
        BlockPos nearest = level != null && level.isClientSide
                ? CreateSeamFold.foldClientPosition(level, owner, partnerPos)
                : CreateSeamFold.foldPosition(level, owner, partnerPos);
        if (nearest.equals(partnerPos)) {
            return rawKey;
        }

        return owner.subtract(nearest);
    }

    public static BlockPos normalize(@Nullable Level level, BlockPos owner, BlockPos storedKey) {
        return shortWay(level, owner, owner.subtract(storedKey), storedKey);
    }

    private RelativeKeyFold() {
    }
}
