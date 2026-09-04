package com.toroidalworld.map;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.CurrentServer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public final class MapSeamFold {
    public static @Nullable WorldFold transformerFor(@Nullable LevelAccessor level, ResourceKey<Level> dimension) {
        if (level instanceof Level actualLevel) {
            return WorldLoopAttachments.wrappedTransformerOf(actualLevel);
        }

        MinecraftServer server = CurrentServer.get();
        if (server == null) {
            return null;
        }

        return WorldLoopAttachments.wrappedTransformerOf(server.getLevel(dimension));
    }

    private MapSeamFold() {
    }
}
