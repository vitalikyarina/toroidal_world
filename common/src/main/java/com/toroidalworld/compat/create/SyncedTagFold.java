package com.toroidalworld.compat.create;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.net.TagPositions;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class SyncedTagFold {
    private static final TagPositions.Table TABLE = new TagPositions.Table();

    public static void register(Class<?> blockEntityType, TagPositions.PositionShape shape, String... keys) {
        TABLE.register(blockEntityType, shape, keys);
    }

    public static CompoundTag inFrameOf(BlockEntity blockEntity, CompoundTag tag) {
        Level level = blockEntity.getLevel();
        if (level == null || !level.isClientSide) {
            return tag;
        }

        WorldFold clientTransformer = WorldLoopAttachments.wrappedClientBoundsTransformerOf(level);
        return clientTransformer == null
                ? tag
                : seatedIn(clientTransformer, blockEntity.getBlockPos(), blockEntity.getClass(), tag);
    }

    static CompoundTag seatedIn(WorldFold fold, BlockPos worldPosition, Class<?> blockEntityType, CompoundTag tag) {
        return TABLE.seatedIn(around(fold, worldPosition), blockEntityType, tag);
    }

    private static TagPositions.Seat around(WorldFold fold, BlockPos worldPosition) {
        return new TagPositions.Seat() {
            @Override
            public BlockPos seat(BlockPos stored) {
                return fold.nearestCopy(worldPosition, stored);
            }

            @Override
            public Vec3 seat(Vec3 stored) {
                return fold.nearestCopy(Vec3.atCenterOf(worldPosition), stored);
            }
        };
    }

    private SyncedTagFold() {
    }
}
