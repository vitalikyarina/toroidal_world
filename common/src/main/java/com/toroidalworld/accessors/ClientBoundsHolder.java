package com.toroidalworld.accessors;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;

public interface ClientBoundsHolder {
    default WorldFold toroidal$clientBounds() {
        return WorldFolds.NOOP;
    }

    default void toroidal$setClientBounds(WorldFold transformer) {
    }
}
