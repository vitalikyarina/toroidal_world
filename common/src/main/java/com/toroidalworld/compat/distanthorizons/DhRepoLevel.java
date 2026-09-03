package com.toroidalworld.compat.distanthorizons;

import com.toroidalworld.api.ToroidalShape;
import com.seibel.distanthorizons.core.level.IDhLevel;

public interface DhRepoLevel {
    default void toroidal$bindLevel(IDhLevel level) {
    }

    default IDhLevel toroidal$level() {
        return null;
    }

    default ToroidalShape toroidal$shape() {
        return null;
    }
}
