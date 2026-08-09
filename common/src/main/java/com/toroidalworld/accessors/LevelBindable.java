package com.toroidalworld.accessors;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;

// Vanilla's ticket graph never sees the level it belongs to, so the level is handed to it once, at construction.
// It is the level and not the transformer that is bound: the transformer is derived from saved data that may not
// exist yet at that point, and the attachment resolves (and caches) it on first use.
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
