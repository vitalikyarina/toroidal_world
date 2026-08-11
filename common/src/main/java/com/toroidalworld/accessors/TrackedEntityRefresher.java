package com.toroidalworld.accessors;

import net.minecraft.server.level.ServerPlayer;

// The entity half of what a teleport owes the per-player traffic state. Vanilla decides which entities a player is
// shown from the player's position, and refreshes that decision synchronously whenever the player moves — but the
// teleport path never calls the refresh (ChunkMap.move is reached from handleMovePlayer and the camera, not from
// ServerGamePacketListenerImpl.teleport). Vanilla can afford the tick of staleness because a stale entity update is
// drawn where it belongs and then removed; here the mirror has already jumped, so that update is folded around an
// anchor it was never gated on and can name the copy a world away from the one the client holds.
public interface TrackedEntityRefresher {
    // Re-run vanilla's own visibility decision for this player against every tracked entity. Call after the teleport
    // has moved the mirror: entities the player has left behind leave through a packet that carries no coordinates,
    // and any that the new position brings into view are placed around the mirror they belong to.
    void toroidal$refreshTrackedEntities(ServerPlayer player);
}
