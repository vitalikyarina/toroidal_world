package com.toroidalworld.accessors;

import com.toroidalworld.core.WorldLoopTransformer;

// Implemented by mixins on vanilla objects that have to carry the level's transformer with them because the vanilla
// code they live in never sees the level (ChunkTrackingView.of is a static interface method).
public interface TransformerHolder {
    default WorldLoopTransformer toroidal$transformer() {
        return WorldLoopTransformer.NOOP;
    }

    default void toroidal$setTransformer(WorldLoopTransformer transformer) {
    }
}
