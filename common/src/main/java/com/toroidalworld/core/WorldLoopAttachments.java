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

    public static WorldFold noiseTransformerOf(Level level) {
        WorldFold clientBounds = wrappedClientBoundsTransformerOf(level);
        return clientBounds != null ? clientBounds : transformerOf(level);
    }

    public static @Nullable WorldFold noiseTransformerOfReader(LevelReader reader) {
        if (reader instanceof Level level) {
            return noiseTransformerOf(level);
        }

        ServerLevel level = serverLevelOf(reader);
        return level != null ? noiseTransformerOf(level) : null;
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
