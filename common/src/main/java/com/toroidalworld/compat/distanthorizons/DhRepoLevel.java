package com.toroidalworld.compat.distanthorizons;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.api.v1.ToroidalShape;
import com.seibel.distanthorizons.core.level.IDhLevel;

public interface DhRepoLevel {
    static @Nullable ToroidalShape shapeOf(Object repo) {
        return ((DhRepoLevel) repo).toroidal$shape();
    }

    default void toroidal$bindLevel(IDhLevel level) {
    }

    default @Nullable ToroidalShape toroidal$shape() {
        return null;
    }
}
