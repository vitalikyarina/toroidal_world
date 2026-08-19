package com.toroidalworld.accessors;

import com.toroidalworld.core.WorldLoopTransformer;

public interface ClientBoundsHolder {
    default WorldLoopTransformer toroidal$clientBounds() {
        return WorldLoopTransformer.NOOP;
    }

    default void toroidal$setClientBounds(WorldLoopTransformer transformer) {
    }
}
