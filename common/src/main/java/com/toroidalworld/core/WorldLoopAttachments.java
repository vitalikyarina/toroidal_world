package com.toroidalworld.core;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.accessors.ClientBoundsHolder;
import com.toroidalworld.accessors.CrumbSweepCache;
import com.toroidalworld.accessors.TransformerCache;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;

public final class WorldLoopAttachments {
    public static WorldFold transformerOf(Level level) {
        return ((TransformerCache) level).toroidal$transformer();
    }

    public static boolean sweepsCrumbs(Level level) {
        return ((CrumbSweepCache) level).toroidal$sweepsCrumbs();
    }

    public static @Nullable WorldFold wrappedTransformerOf(@Nullable Level level) {
        if (level == null) {
            return null;
        }

        WorldFold transformer = transformerOf(level);
        return transformer.isWrapped() ? transformer : null;
    }

    public static @Nullable WorldFold wrappedTransformerOf(
            @Nullable MinecraftServer server, ResourceKey<Level> dimension) {
        return server != null ? wrappedTransformerOf(server.getLevel(dimension)) : null;
    }

    private static WorldFold clientBoundsTransformerOf(Level level) {
        return level instanceof ClientBoundsHolder holder ? holder.toroidal$clientBounds() : WorldFolds.NOOP;
    }

    public static @Nullable WorldFold wrappedClientBoundsTransformerOf(@Nullable Level level) {
        if (level == null) {
            return null;
        }

        WorldFold transformer = clientBoundsTransformerOf(level);
        return transformer.isWrapped() ? transformer : null;
    }

    private static @Nullable Level levelOf(@Nullable LevelReader reader) {
        if (reader instanceof Level level) {
            return level;
        }

        return reader != null ? serverLevelOf(reader) : null;
    }

    public static WorldFold transformerOfReader(@Nullable LevelReader reader) {
        Level level = levelOf(reader);
        if (level == null) {
            return WorldFolds.NOOP;
        }

        WorldFold clientBounds = wrappedClientBoundsTransformerOf(level);
        return clientBounds != null ? clientBounds : transformerOf(level);
    }

    public static WorldFold noiseTransformerOf(Level level) {
        return transformerOfReader(level);
    }

    public static @Nullable WorldFold noiseTransformerOfReader(LevelReader reader) {
        Level level = levelOf(reader);
        return level != null ? transformerOfReader(level) : null;
    }

    public static @Nullable ServerLevel serverLevelOf(LevelReader reader) {
        if (reader instanceof ServerLevel level) {
            return level;
        }

        if (reader instanceof Level level && level.isClientSide()) {
            return null;
        }

        return reader instanceof ServerLevelAccessor accessor ? accessor.getLevel() : null;
    }

    public static @Nullable WorldFold wrappedTransformerOfReader(@Nullable LevelReader reader) {
        Level level = reader == null ? null : levelOf(reader);
        if (level == null) {
            return null;
        }

        WorldFold clientBounds = wrappedClientBoundsTransformerOf(level);
        return clientBounds != null ? clientBounds : wrappedTransformerOf(level);
    }

    private WorldLoopAttachments() {
    }
}
