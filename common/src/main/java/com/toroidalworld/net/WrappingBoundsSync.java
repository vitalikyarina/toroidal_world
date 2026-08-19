package com.toroidalworld.net;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.platform.Platforms;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.server.level.ServerPlayer;

public final class WrappingBoundsSync {
    public static void sendTo(ServerPlayer player) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(player.level());
        if (transformer != null) {
            Platforms.get().sendWrappingBounds(player, transformer.bounds);
        }
    }

    private WrappingBoundsSync() {
    }
}
