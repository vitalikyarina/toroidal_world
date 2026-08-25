package com.toroidalworld.client;

import com.toroidalworld.accessors.ClientBoundsHolder;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public final class WorldLoopClientNetwork {
    public static void apply(FlatShape shape) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            ((ClientBoundsHolder) level).toroidal$setClientBounds(WorldFolds.of(shape));
        }
    }

    private WorldLoopClientNetwork() {
    }
}
