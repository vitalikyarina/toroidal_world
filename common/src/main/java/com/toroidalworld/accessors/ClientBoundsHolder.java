package com.toroidalworld.accessors;

import com.toroidalworld.core.WorldLoopTransformer;

// The bounds the server told this client, carried by the client level itself — deliberately NOT TransformerHolder:
// its toroidal$transformer() would override the TransformerCache method LevelMixin already adds to every Level, and
// the client level's engine transformer MUST stay NOOP — the client is told the world is infinite, which is what
// keeps rendering and chunk loading working across the seam. Different method names keep the two answers apart:
// the engine asks toroidal$transformer(), a bounds reader (the debug overlay, the compass) asks toroidal$clientBounds().
public interface ClientBoundsHolder {
    default WorldLoopTransformer toroidal$clientBounds() {
        return WorldLoopTransformer.NOOP;
    }

    default void toroidal$setClientBounds(WorldLoopTransformer transformer) {
    }
}
