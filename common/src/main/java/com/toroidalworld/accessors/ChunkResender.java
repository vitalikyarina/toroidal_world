package com.toroidalworld.accessors;

import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public interface ChunkResender {
    void toroidal$dropTrackedChunks(ServerPlayer player);

    void toroidal$resendTrackedChunks(ServerPlayer player);

    void toroidal$dropChunks(ServerPlayer player, List<ChunkPos> chunks);

    void toroidal$resendChunks(ServerPlayer player, List<ChunkPos> chunks);
}
