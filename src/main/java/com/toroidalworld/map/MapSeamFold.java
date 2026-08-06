package com.toroidalworld.map;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

// A map computes every canvas position as a delta from its stored center, but its callers do not always hand it a
// level to ask about the world: banners and frames reloaded from NBT re-add their decorations inside the map's own
// constructor, where the level parameter is null. The map does carry its dimension key, and a saved map only ever
// loads on a running server — so when the level is absent the transformer is resolved through the current server.
// On a client without an integrated server there is no server and no fold, which is right: client-built map data is
// canvas-space and never computes a world delta.
public final class MapSeamFold {
    public static @Nullable WorldLoopTransformer transformerFor(@Nullable LevelAccessor level, ResourceKey<Level> dimension) {
        if (level instanceof Level actualLevel) {
            return WorldLoopAttachments.wrappedTransformerOf(actualLevel);
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }

        ServerLevel serverLevel = server.getLevel(dimension);
        return serverLevel == null ? null : WorldLoopAttachments.wrappedTransformerOf(serverLevel);
    }

    private MapSeamFold() {
    }
}
