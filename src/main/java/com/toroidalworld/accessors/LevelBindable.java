package com.toroidalworld.accessors;

import net.minecraft.server.level.ServerLevel;

// Vanilla's ticket graph never sees the level it belongs to, so the level is handed to it once, at construction.
// It is the level and not the transformer that is bound: the transformer is derived from saved data that may not
// exist yet at that point, and the attachment resolves (and caches) it on first use.
public interface LevelBindable {
    default void toroidal$bindLevel(ServerLevel level) {
    }
}
