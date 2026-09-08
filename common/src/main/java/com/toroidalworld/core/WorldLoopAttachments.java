package com.toroidalworld.core;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.accessors.ClientBoundsHolder;
import com.toroidalworld.accessors.ClientPositionHolder;
import com.toroidalworld.accessors.CrumbSweepCache;
import com.toroidalworld.accessors.SeamTravelHolder;
import com.toroidalworld.accessors.TransformerCache;
import com.toroidalworld.engine.seam.ClientPosition;
import com.toroidalworld.engine.seam.SeamTravel;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

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
