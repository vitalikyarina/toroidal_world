package com.toroidalworld.api;

import java.util.Optional;
import java.util.OptionalDouble;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Entry point for reading a level's toroidal geometry on the logical server (or any {@link Level} whose own
 * engine knows its bounds). On the client the level is deliberately told the world is infinite, so its geometry
 * is read through {@link ToroidalWorldClientApi} instead.
 */
public final class ToroidalWorldApi {

    /**
     * The toroidal shape of {@code level}, or empty when no axis of that level loops. The view is immutable and
     * cheap; callers may hold it for as long as the level lives.
     */
    public static Optional<ToroidalShape> shapeOf(Level level) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        return transformer == null ? Optional.empty() : Optional.of(new WorldFoldToroidalShape(transformer));
    }

    /**
     * How far {@code player} has travelled along {@code axis} toward the next lap of the dimension they are in,
     * in blocks, signed with the direction of travel. Pacing back and forth cancels out, crossing the seam does
     * not count as a world width, and one whole width is taken off each time a lap closes. Empty when that
     * dimension does not wrap on that axis; {@link Direction.Axis#Y} is always empty.
     */
    public static OptionalDouble travelOf(ServerPlayer player, Direction.Axis axis) {
        if (axis == Direction.Axis.Y) {
            return OptionalDouble.empty();
        }

        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(player.level());
        if (transformer == null || !transformer.bounds().loops(axis)) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(
                WorldLoopAttachments.travelOf(player).in(player.level().dimension()).on(axis));
    }

    private ToroidalWorldApi() {
    }
}
