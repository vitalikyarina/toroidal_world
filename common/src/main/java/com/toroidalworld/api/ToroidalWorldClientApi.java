package com.toroidalworld.api;

import java.util.Optional;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.client.multiplayer.ClientLevel;

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
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedClientBoundsTransformerOf(level);
        return transformer == null ? Optional.empty() : Optional.of(new TransformerToroidalShape(transformer));
    }

    private ToroidalWorldClientApi() {
    }
}
