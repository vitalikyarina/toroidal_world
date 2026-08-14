package com.toroidalworld.api;

import java.util.Optional;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

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
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        return transformer == null ? Optional.empty() : Optional.of(new TransformerToroidalShape(transformer));
    }

    private ToroidalWorldApi() {
    }
}
