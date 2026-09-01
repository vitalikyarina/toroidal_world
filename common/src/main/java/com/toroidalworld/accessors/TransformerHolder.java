package com.toroidalworld.accessors;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;

public interface TransformerHolder {
    default WorldFold toroidal$transformer() {
        return WorldFolds.NOOP;
    }

    default void toroidal$setTransformer(WorldFold transformer) {
    }
}
