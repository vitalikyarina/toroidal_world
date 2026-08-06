package com.toroidalworld.net;

import com.toroidalworld.ToroidalWorld;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

// The two moments the client's space changes and it needs the bounds again: joining, and crossing to another dimension
// (the overworld and the nether wrap at different widths). A same-dimension respawn keeps the client level, and with it
// the transformer, so it needs nothing.
@EventBusSubscriber(modid = ToroidalWorld.MODID)
public final class WrappingSync {
    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WorldLoopNetwork.sendTo(player);
        }
    }

    @SubscribeEvent
    static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WorldLoopNetwork.sendTo(player);
        }
    }

    private WrappingSync() {
    }
}
