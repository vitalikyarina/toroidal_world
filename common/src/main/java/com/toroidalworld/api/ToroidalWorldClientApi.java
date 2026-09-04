package com.toroidalworld.api;

import java.util.Optional;

import com.toroidalworld.client.PublishedShapes;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Entry point for reading toroidal geometry on the client. A client level's own engine believes the world is
 * infinite — that is what keeps rendering and chunk loading vanilla across the seam — so {@link ToroidalWorldApi}
 * answers empty there. The bounds the server synced ride apart on the client level, and this reads them.
 *
 * <p>Client coordinates near the seam run whole world widths from the server's: the shape's folding operations are
 * exactly what maps them back — fold a coordinate before keying storage by it, take {@code nearestCopy} /
 * {@code shortestDelta} against the player before measuring direction or distance.</p>
 */
public final class ToroidalWorldClientApi {

    /**
     * The toroidal shape the server declared for {@code level}, or empty when the level does not loop (or the
     * bounds have not arrived yet — they are synced on login and dimension change, before the first chunk).
     */
    public static Optional<ToroidalShape> shapeOf(ClientLevel level) {
        WorldFold transformer = WorldLoopAttachments.wrappedClientBoundsTransformerOf(level);
        return transformer == null ? Optional.empty() : Optional.of(new WorldFoldToroidalShape(transformer));
    }

    /**
     * The toroidal shape the server declared for {@code dimension}, whether or not the player is standing in it —
     * what a fullscreen map browsing another dimension folds by. Empty when that dimension does not loop, or when
     * the server never published it: every looping level arrives on login, before the first chunk.
     */
    public static Optional<ToroidalShape> shapeOf(ResourceKey<Level> dimension) {
        WorldFold fold = PublishedShapes.foldOf(dimension);
        return fold == null ? Optional.empty() : Optional.of(new WorldFoldToroidalShape(fold));
    }

    private ToroidalWorldClientApi() {
    }
}
