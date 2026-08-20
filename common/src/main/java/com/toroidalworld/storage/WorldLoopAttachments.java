package com.toroidalworld.storage;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.accessors.ClientBoundsHolder;
import com.toroidalworld.accessors.ClientPositionHolder;
import com.toroidalworld.accessors.TransformerCache;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.player.ClientPosition;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;

public final class WorldLoopAttachments {
    public static WorldLoopTransformer transformerOf(Level level) {
        return ((TransformerCache) level).toroidal$transformer();
    }

    public static @Nullable WorldLoopTransformer wrappedTransformerOf(Level level) {
        WorldLoopTransformer transformer = transformerOf(level);
        return transformer.isWrapped() ? transformer : null;
    }

    private static WorldLoopTransformer clientBoundsTransformerOf(Level level) {
        return level instanceof ClientBoundsHolder holder ? holder.toroidal$clientBounds() : WorldLoopTransformer.NOOP;
    }

    public static @Nullable WorldLoopTransformer wrappedClientBoundsTransformerOf(Level level) {
        WorldLoopTransformer transformer = clientBoundsTransformerOf(level);
        return transformer.isWrapped() ? transformer : null;
    }

    public static @Nullable WorldLoopTransformer noiseTransformerOf(LevelReader reader) {
        if (reader instanceof Level level) {
            WorldLoopTransformer clientBounds = wrappedClientBoundsTransformerOf(level);
            return clientBounds != null ? clientBounds : transformerOf(level);
        }

        if (reader instanceof ServerLevelAccessor accessor) {
            return transformerOf(accessor.getLevel());
        }

        return null;
    }

    public static ClientPosition clientPositionOf(ServerPlayer player) {
        return ((ClientPositionHolder) player.connection).toroidal$clientPosition();
    }

    public static void rebaseClientPositionOf(ServerPlayer player) {
        if (player.connection == null) {
            return;
        }

        WorldLoopTransformer transformer = transformerOf(player.level());
        clientPositionOf(player).rebase(
                transformer.coords.x.wrap(player.getX()),
                transformer.coords.z.wrap(player.getZ()),
                player.level().dimension(),
                transformer);
    }

    private WorldLoopAttachments() {
    }
}
