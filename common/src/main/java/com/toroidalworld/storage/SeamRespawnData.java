package com.toroidalworld.storage;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldLoopTransformer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelData;

public final class SeamRespawnData {
    public static LevelData.RespawnData insideBounds(
            @Nullable MinecraftServer server, LevelData.RespawnData respawnData) {
        if (server == null) {
            return respawnData;
        }

        ServerLevel level = server.getLevel(respawnData.dimension());
        if (level == null) {
            return respawnData;
        }

        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            return respawnData;
        }

        BlockPos pos = respawnData.pos();
        if (!transformer.coords.x.isOver(pos.getX()) && !transformer.coords.z.isOver(pos.getZ())) {
            return respawnData;
        }

        return new LevelData.RespawnData(
                GlobalPos.of(respawnData.dimension(), transformer.blocks.wrap(pos)),
                respawnData.yaw(),
                respawnData.pitch());
    }

    private SeamRespawnData() {
    }
}
