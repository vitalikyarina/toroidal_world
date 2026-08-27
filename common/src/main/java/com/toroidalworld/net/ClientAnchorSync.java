package com.toroidalworld.net;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.player.ClientPosition;
import com.toroidalworld.player.ClientPosition.BorderCenter;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;

public final class ClientAnchorSync {
    public static void refresh(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            return;
        }

        ClientPosition clientPosition = WorldLoopAttachments.clientPositionOf(player);
        if (!clientPosition.describes(level.dimension())) {
            return;
        }

        refreshSpawn(player, level, transformer, clientPosition);
        refreshBorderCenter(player, level, transformer, clientPosition);
        refreshCacheCenter(player, level, transformer, clientPosition);
    }

    private static void refreshSpawn(ServerPlayer player, ServerLevel level, WorldFold transformer,
            ClientPosition clientPosition) {
        if (!Level.OVERWORLD.equals(level.dimension())) {
            return;
        }

        BlockPos held = clientPosition.heldSpawn();
        BlockPos spawnPos = level.getSharedSpawnPos();
        BlockPos want = PacketTranslator.nearestCopyBlock(transformer, clientPosition.chunk(), spawnPos);
        if (want.equals(held)) {
            return;
        }

        player.connection.send(
                new ClientboundSetDefaultSpawnPositionPacket(spawnPos, level.getSharedSpawnAngle()));
    }

    private static void refreshBorderCenter(ServerPlayer player, ServerLevel level, WorldFold transformer,
            ClientPosition clientPosition) {
        WorldBorder border = level.getWorldBorder();
        BorderCenter held = clientPosition.heldBorderCenter();
        BorderCenter want = PacketTranslator.nearestCopyCenter(transformer, clientPosition,
                new BorderCenter(border.getCenterX(), border.getCenterZ()));
        if (want.equals(held)) {
            return;
        }

        player.connection.send(new ClientboundSetBorderCenterPacket(border));
    }

    private static void refreshCacheCenter(ServerPlayer player, ServerLevel level, WorldFold transformer,
            ClientPosition clientPosition) {
        ChunkPos held = clientPosition.heldCacheCenter();
        if (held == null || !(player.getChunkTrackingView() instanceof ChunkTrackingView.Positioned view)) {
            return;
        }

        ChunkPos mirrorChunk = clientPosition.chunk();
        ChunkPos want = transformer.nearestCopy(mirrorChunk, view.center());
        if (want.equals(held)) {
            return;
        }

        player.connection.send(new ClientboundSetChunkCacheCenterPacket(view.center().x(), view.center().z()));
    }

    private ClientAnchorSync() {
    }
}
