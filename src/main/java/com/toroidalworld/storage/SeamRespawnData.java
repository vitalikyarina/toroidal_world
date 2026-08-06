package com.toroidalworld.storage;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldLoopTransformer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelData;

// A respawn point is one of the few coordinates the server keeps rather than spends. Everything else a command names is
// consumed on the spot — a block written, an entity moved, a packet sent — and each of those paths already folds the
// seam on its way to the world, so a coordinate arriving from past the bounds lands where it means to. This one is
// written down instead: into the player's data or into level.dat, where it outlives the command, the session and the
// restart, and where nothing folds anything.
//
// So this is where the wrap belongs — at the moment of storage, not on the way in. A coordinate is only ever settled
// once, against the bounds of the dimension it names rather than the one the command was run in: /spawnpoint stores a
// point in the sender's level, but the same record is read back by a respawn that may happen anywhere.
//
// Unchanged data is handed straight back, so a spawn point set anywhere inside the world keeps the exact record vanilla
// built — the same instance, which is also what MinecraftServer.setRespawnData compares against to decide whether
// anything happened at all.
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
