package com.toroidalworld.net;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.player.ClientPosition;
import com.toroidalworld.player.ClientPosition.BorderCenter;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;

public final class ClientAnchorSync {
    public static void refresh(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            return;
        }

        ClientPosition clientPosition = WorldLoopAttachments.clientPositionOf(player);
        if (!clientPosition.describes(level.dimension())) {
            return;
        }

        refreshSpawn(player, level, transformer, clientPosition);
        refreshBorderCenter(player, level, transformer, clientPosition);
    }

    private static void refreshSpawn(ServerPlayer player, ServerLevel level, WorldLoopTransformer transformer,
            ClientPosition clientPosition) {
        // The world spawn is a single coordinate stored in the overworld's level data; every other dimension reads that
        // same coordinate back through DerivedLevelData, so it names no place in their wrap and folding it against
        // their bounds would fold it against the wrong world. There is nothing to refresh outside the overworld.
        if (!Level.OVERWORLD.equals(level.dimension())) {
            return;
        }

        BlockPos held = clientPosition.heldSpawn();
        BlockPos spawnPos = level.getSharedSpawnPos();
        int anchorChunkX = SectionPos.blockToSectionCoord(clientPosition.x());
        int anchorChunkZ = SectionPos.blockToSectionCoord(clientPosition.z());
        int wantX = PacketTranslator.nearestCopyBlockX(transformer, anchorChunkX, spawnPos.getX());
        int wantZ = PacketTranslator.nearestCopyBlockZ(transformer, anchorChunkZ, spawnPos.getZ());
        if (held != null && held.getX() == wantX && held.getY() == spawnPos.getY() && held.getZ() == wantZ) {
            return;
        }

        player.connection.send(
                new ClientboundSetDefaultSpawnPositionPacket(spawnPos, level.getSharedSpawnAngle()));
    }

    private static void refreshBorderCenter(ServerPlayer player, ServerLevel level, WorldLoopTransformer transformer,
            ClientPosition clientPosition) {
        WorldBorder border = level.getWorldBorder();
        BorderCenter held = clientPosition.heldBorderCenter();
        double wantX = PacketTranslator.nearestCopyCenterX(transformer, clientPosition, border.getCenterX());
        double wantZ = PacketTranslator.nearestCopyCenterZ(transformer, clientPosition, border.getCenterZ());
        if (held != null && held.x() == wantX && held.z() == wantZ) {
            return;
        }

        player.connection.send(new ClientboundSetBorderCenterPacket(border));
    }

    private ClientAnchorSync() {
    }
}
