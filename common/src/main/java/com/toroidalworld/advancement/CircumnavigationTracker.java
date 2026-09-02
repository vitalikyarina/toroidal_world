package com.toroidalworld.advancement;

import com.toroidalworld.player.SeamTravel;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class CircumnavigationTracker {
    public static void sample(ServerPlayer player) {
        Level level = player.level();
        SeamTravel.Step step = WorldLoopAttachments.travelOf(player)
                .advance(WorldLoopAttachments.wrappedTransformerOf(level), level.dimension(), player.position());
        if (!step.closed().isEmpty()) {
            WorldLoopCriteria.CIRCUMNAVIGATE.trigger(player);
        }
    }

    private CircumnavigationTracker() {
    }
}
