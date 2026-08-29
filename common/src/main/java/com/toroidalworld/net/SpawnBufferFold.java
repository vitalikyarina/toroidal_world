package com.toroidalworld.net;

import net.minecraft.nbt.CompoundTag;

public final class SpawnBufferFold {
    private static final TagPositions.Table TABLE = new TagPositions.Table();

    public static void register(Class<?> entityType, TagPositions.PositionShape shape, String... keys) {
        TABLE.register(entityType, shape, keys);
    }

    public static boolean carriesPositions(Class<?> entityType) {
        return TABLE.carriesPositions(entityType);
    }

    public static CompoundTag seatedIn(TagPositions.Seat seat, Class<?> entityType, CompoundTag tag) {
        return TABLE.seatedIn(seat, entityType, tag);
    }

    private SpawnBufferFold() {
    }
}
