package com.toroidalworld.accessors;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;

public interface LevelBindable {
    default void toroidal$bindLevel(ServerLevel level) {
    }

    default @Nullable ServerLevel toroidal$boundLevel() {
        return null;
    }
}
