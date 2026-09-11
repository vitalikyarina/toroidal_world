package com.toroidalworld.compat.jmi;

import com.toroidalworld.compat.ftbchunks.FtbChunksFold;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;

public final class JmiFold {
    public static ChunkPos foldChunk(ChunkPos chunk) {
        return new ChunkPos(FtbChunksFold.foldChunk(Direction.Axis.X, chunk.x),
                FtbChunksFold.foldChunk(Direction.Axis.Z, chunk.z));
    }

    public static ChunkPos seatNearPlayer(ChunkPos folded) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return folded;
        }

        ChunkPos anchor = foldChunk(player.chunkPosition());
        return new ChunkPos(FtbChunksFold.nearestChunk(Direction.Axis.X, anchor.x, folded.x),
                FtbChunksFold.nearestChunk(Direction.Axis.Z, anchor.z, folded.z));
    }

    private JmiFold() {
    }
}
