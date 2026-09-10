package com.toroidalworld.compat.distanthorizons;

import com.toroidalworld.api.v1.ToroidalShape;
import com.seibel.distanthorizons.core.level.IDhLevel;

public interface DhRepoLevel {
    default void toroidal$bindLevel(IDhLevel level) {
    }

    default ToroidalShape toroidal$shape() {
        return null;
    }
}
