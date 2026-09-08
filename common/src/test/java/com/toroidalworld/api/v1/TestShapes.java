package com.toroidalworld.api.v1;

import com.toroidalworld.core.WorldFold;

public final class TestShapes {
    public static ToroidalShape of(WorldFold fold) {
        return new WorldFoldToroidalShape(fold);
    }

    private TestShapes() {
    }
}
