package com.toroidalworld.compat;

import com.toroidalworld.client.ClientFrame;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.net.TagPositions;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class SyncedTagFold {
    private static final TagPositions.Table TABLE = new TagPositions.Table();

    private static final TagPositions.Seat VIEWER_SEAT = new TagPositions.Seat() {
        @Override
        public BlockPos seat(BlockPos stored) {
            return ClientFrame.nearestToPlayer(stored);
        }

        @Override
        public Vec3 seat(Vec3 stored) {
            return ClientFrame.nearestToPlayer(stored);
        }
    };

    public static void register(Class<?> blockEntityType, TagPositions.PositionShape shape, String... keys) {
        TABLE.register(blockEntityType, shape, keys);
    }

    public static void registerIn(Class<?> blockEntityType, String container, TagPositions.PositionShape shape,
            String... keys) {
        TABLE.registerIn(blockEntityType, container, shape, keys);
    }

    public static void registerInEach(Class<?> blockEntityType, String container, TagPositions.PositionShape shape,
            String... keys) {
        TABLE.registerInEach(blockEntityType, container, shape, keys);
    }

    public static CompoundTag inFrameOf(BlockEntity blockEntity, CompoundTag tag) {
        Level level = blockEntity.getLevel();
        if (level == null || !level.isClientSide || !ClientFrame.isClientLevel(level)) {
            return tag;
        }

        return TABLE.seatedIn(VIEWER_SEAT, blockEntity.getClass(), tag);
    }

    static CompoundTag seatedIn(TagPositions.Table table, WorldFold fold, BlockPos anchor,
            Class<?> blockEntityType, CompoundTag tag) {
        return table.seatedIn(around(fold, anchor), blockEntityType, tag);
    }

    private static TagPositions.Seat around(WorldFold fold, BlockPos anchor) {
        return new TagPositions.Seat() {
            @Override
            public BlockPos seat(BlockPos stored) {
                return fold.nearestCopy(anchor, stored);
            }

            @Override
            public Vec3 seat(Vec3 stored) {
                return fold.nearestCopy(Vec3.atCenterOf(anchor), stored);
            }
        };
    }

    private SyncedTagFold() {
    }
}
