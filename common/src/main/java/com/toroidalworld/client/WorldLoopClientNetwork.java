package com.toroidalworld.client;

import com.toroidalworld.accessors.ClientBoundsHolder;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class WorldLoopClientNetwork {
    public static void apply(ResourceKey<Level> dimension, FlatShape shape) {
        WorldFold fold = WorldFolds.of(shape);
        PublishedShapes.publish(dimension, fold);
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && level.dimension() == dimension) {
            ((ClientBoundsHolder) level).toroidal$setClientBounds(fold);
        }
    }

    private WorldLoopClientNetwork() {
    }
}
