package com.toroidalworld.storage;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.accessors.ClientBoundsHolder;
import com.toroidalworld.accessors.ClientPositionHolder;
import com.toroidalworld.accessors.SeamTravelHolder;
import com.toroidalworld.accessors.TransformerCache;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.player.ClientPosition;
import com.toroidalworld.player.SeamTravel;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

public final class WorldLoopAttachments {
    public static WorldFold transformerOf(Level level) {
        return ((TransformerCache) level).toroidal$transformer();
    }

    public static @Nullable WorldFold wrappedTransformerOf(Level level) {
        WorldFold transformer = transformerOf(level);
        return transformer.isWrapped() ? transformer : null;
    }

    private static WorldFold clientBoundsTransformerOf(Level level) {
        return level instanceof ClientBoundsHolder holder ? holder.toroidal$clientBounds() : WorldFolds.NOOP;
    }

    public static @Nullable WorldFold wrappedClientBoundsTransformerOf(Level level) {
        WorldFold transformer = clientBoundsTransformerOf(level);
        return transformer.isWrapped() ? transformer : null;
    }

    public static WorldFold noiseTransformerOf(Level level) {
        WorldFold clientBounds = wrappedClientBoundsTransformerOf(level);
        return clientBounds != null ? clientBounds : transformerOf(level);
    }

    public static @Nullable WorldFold noiseTransformerOfReader(LevelReader reader) {
        if (reader instanceof Level level) {
            return noiseTransformerOf(level);
        }

        if (reader instanceof ServerLevelAccessor accessor) {
            return noiseTransformerOf(accessor.getLevel());
        }

        return null;
    }

    public static SeamTravel travelOf(ServerPlayer player) {
        return ((SeamTravelHolder) player).toroidal$travel();
    }

    public static ClientPosition clientPositionOf(ServerPlayer player) {
        return ((ClientPositionHolder) player.connection).toroidal$clientPosition();
    }

    public static void rebaseClientPositionOf(ServerPlayer player) {
        if (player.connection == null) {
            return;
        }

        WorldFold transformer = transformerOf(player.level());
        Vec3 folded = transformer.fold(player.position());
        clientPositionOf(player).rebase(
                folded.x,
                folded.z,
                player.level().dimension(),
                transformer);
    }

    private WorldLoopAttachments() {
    }
}
