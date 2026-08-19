package com.toroidalworld.accessors;

import net.minecraft.server.level.ServerPlayer;

public interface TrackedEntityRefresher {
    void toroidal$refreshTrackedEntities(ServerPlayer player);
}
