package com.toroidalworld.accessors;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;

//
// The level is readable back because a subclass mixin may need it without binding one of its own — binding twice
// would override the base implementation and leave the base copy unset.
public interface LevelBindable {
    default void toroidal$bindLevel(ServerLevel level) {
    }

    default @Nullable ServerLevel toroidal$boundLevel() {
        return null;
    }
}
