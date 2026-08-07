package com.toroidalworld.accessors;

import net.minecraft.server.level.ServerLevel;

// Vanilla keeps ChunkMap's level private, and the chunk sender only ever receives the map — this hands the level back.
public interface LevelHolder {
    ServerLevel toroidal$level();
}
