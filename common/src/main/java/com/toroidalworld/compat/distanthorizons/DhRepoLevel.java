package com.toroidalworld.compat.distanthorizons;

import com.toroidalworld.api.v1.ToroidalShape;
import com.seibel.distanthorizons.core.level.IDhLevel;

public interface DhRepoLevel {
    static ToroidalShape shapeOf(Object repo) {
        return ((DhRepoLevel) repo).toroidal$shape();
    }

    default void toroidal$bindLevel(IDhLevel level) {
    }

    default ToroidalShape toroidal$shape() {
        return null;
    }
}
