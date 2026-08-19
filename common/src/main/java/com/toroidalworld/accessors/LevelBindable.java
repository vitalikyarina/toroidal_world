package com.toroidalworld.accessors;

import net.minecraft.server.level.ServerLevel;

public interface LevelBindable {
    default void toroidal$bindLevel(ServerLevel level) {
    }
}
