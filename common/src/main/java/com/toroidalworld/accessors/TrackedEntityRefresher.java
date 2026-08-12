package com.toroidalworld.accessors;

import net.minecraft.server.level.ServerPlayer;

// The entity half of what a moved player — or a moved view distance — owes the per-player traffic state. Vanilla
// decides which entities a player is shown from the player's position and the radius in force, and refreshes that
// decision synchronously whenever the player moves. Three paths change one of those two and never ask again:
// ServerGamePacketListenerImpl.teleport (ChunkMap.move is reached from handleMovePlayer and the camera, not from
// there), ServerPlayer.updateOptions (which refreshes nothing at all), and ChunkMap.setServerViewDistance (which
// refreshes the chunk view only). Vanilla can afford the tick of staleness because a stale entity update is drawn
// where it belongs and then removed; here the mirror has already jumped, or the radius the traffic is judged against
// has already shrunk, so that update is measured against a bound it was never gated on.
public interface TrackedEntityRefresher {
    // Re-run vanilla's own visibility decision for this player against every tracked entity. Call once the mirror has
    // moved, or at the moment the view distance moves: entities the player has left behind leave through a packet that
    // carries no coordinates, and any that the new position or the wider view brings into sight are placed around the
    // mirror they belong to.
    void toroidal$refreshTrackedEntities(ServerPlayer player);
}
