package com.toroidalworld.engine.seam.circumnavigation;

import com.toroidalworld.core.WorldLoopAttachments;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class CircumnavigationTracker {
    public static void sample(ServerPlayer player) {
        Level level = player.level();
        SeamTravel.Step step = SeamTravel.of(player)
                .advance(WorldLoopAttachments.wrappedTransformerOf(level), level.dimension(), player.position());
        if (!step.closed().isEmpty()) {
            WorldLoopCriteria.CIRCUMNAVIGATE.trigger(player);
        }
    }

    private CircumnavigationTracker() {
    }
}
