package com.toroidalworld.client;

import com.toroidalworld.accessors.ClientBoundsHolder;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.options.WorldLoopBounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public final class WorldLoopClientNetwork {
    public static void apply(WorldLoopBounds settings) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            ((ClientBoundsHolder) level).toroidal$setClientBounds(new WorldLoopTransformer(settings));
        }
    }

    private WorldLoopClientNetwork() {
    }
}
