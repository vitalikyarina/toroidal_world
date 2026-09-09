package com.toroidalworld.engine.level;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelData;

public final class SeamRespawnData {
    public static LevelData.RespawnData insideBounds(
            @Nullable MinecraftServer server, LevelData.RespawnData respawnData) {
        WorldFold transformer =
                WorldLoopAttachments.wrappedTransformerOf(server, respawnData.dimension());
        if (transformer == null) {
            return respawnData;
        }

        BlockPos pos = respawnData.pos();
        BlockPos folded = transformer.fold(pos);
        if (folded == pos) {
            return respawnData;
        }

        return new LevelData.RespawnData(
                GlobalPos.of(respawnData.dimension(), folded),
                respawnData.yaw(),
                respawnData.pitch());
    }

    private SeamRespawnData() {
    }
}
