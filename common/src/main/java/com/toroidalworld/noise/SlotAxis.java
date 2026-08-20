package com.toroidalworld.noise;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.core.WrapDomain;

public enum SlotAxis {
    X,
    Z,
    NONE;

    private static final WrapDomain UNWRAPPED = new WrapDomain.Noop();

    public boolean carriesWorldAxis() {
        return this != NONE;
    }

    public WrapDomain domainOf(WorldLoopTransformer transformer) {
        return switch (this) {
            case X -> transformer.coords.x;
            case Z -> transformer.coords.z;
            case NONE -> UNWRAPPED;
        };
    }
}
