package com.toroidalworld.accessors;

import com.toroidalworld.player.ClientPosition;

// Where the client believes it is belongs to the client, and the client is the connection. It used to hang off
// ServerPlayer, which death replaces: PlayerList.respawn builds a new player and only assigns it to the listener after
// returning, so every packet the respawn sends on the way — the chunk-cache centre among them — was translated against
// the dead player's mirror while the new one held the truth. The connection outlives that.
public interface ClientPositionHolder {
    ClientPosition toroidal$clientPosition();
}
