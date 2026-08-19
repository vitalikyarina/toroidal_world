package com.toroidalworld.accessors;

import com.toroidalworld.core.WorldLoopTransformer;

public interface TransformerHolder {
    default WorldLoopTransformer toroidal$transformer() {
        return WorldLoopTransformer.NOOP;
    }

    default void toroidal$setTransformer(WorldLoopTransformer transformer) {
    }
}
