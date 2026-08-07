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
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.storage.LevelData;

// The absolute coordinates the client is given once and then keeps: the world spawn and the border's centre. Vanilla
// sends each on the way into a level and afterwards only when someone moves it, while the client's own unbounded
// coordinate drifts a whole world width per lap — so after a lap a stored coordinate names the copy a world behind and
// the client goes on using it. The compass points the long way round; the border's wall is drawn, and client-side
// block breaking and placement gated, a world from where the server measures them.
//
// Each tick (driven from ServerPlayerMixin) the copy the client should hold is recomputed around its mirror; the
// moment one flips, the packet is re-sent and the translator lays it into the fresh copy — which also picks up a value
// changed server-side without a broadcast reaching this player. Both checks are a handful of ops against the cached
// transformer, and an actual re-send only happens when the player crosses half a world from the anchor. Standing
// exactly on that antipode a step back and forth re-sends each tick: it is the one place the two copies are equally
// near, and the farthest the player can be from what the coordinate names.
public final class ClientAnchorSync {
    public static void refresh(ServerPlayer player) {
        ServerLevel level = player.level();
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            return;
        }

        // Between placement and the first rebase the mirror describes nothing — reading it would fail loudly.
        ClientPosition clientPosition = WorldLoopAttachments.clientPositionOf(player);
        if (!clientPosition.describes(level.dimension())) {
            return;
        }

        refreshSpawn(player, level, transformer, clientPosition);
        refreshBorderCenter(player, level, transformer, clientPosition);
    }

    private static void refreshSpawn(ServerPlayer player, ServerLevel level, WorldLoopTransformer transformer,
            ClientPosition clientPosition) {
        // A spawn in another dimension has no copy in this world's wrap — the packet passes untranslated and there is
        // nothing to refresh.
        LevelData.RespawnData respawnData = level.getRespawnData();
        if (!respawnData.dimension().equals(level.dimension())) {
            return;
        }

        BlockPos held = clientPosition.heldSpawn();
        BlockPos spawnPos = respawnData.pos();
        int anchorChunkX = SectionPos.blockToSectionCoord(clientPosition.x());
        int anchorChunkZ = SectionPos.blockToSectionCoord(clientPosition.z());
        int wantX = PacketTranslator.nearestCopyBlockX(transformer, anchorChunkX, spawnPos.getX());
        int wantZ = PacketTranslator.nearestCopyBlockZ(transformer, anchorChunkZ, spawnPos.getZ());
        if (held != null && held.getX() == wantX && held.getY() == spawnPos.getY() && held.getZ() == wantZ) {
            return;
        }

        player.connection.send(new ClientboundSetDefaultSpawnPositionPacket(respawnData));
    }

    // The border always belongs to the level the player stands in — it is that level's own saved data, and there is no
    // foreign-dimension case to skip the way the spawn has one.
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
