package com.toroidalworld.noise;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.core.WrapDomain;
import com.toroidalworld.noise.GenerationTransformerContext.Context;

public enum SlotAxis {
    X,
    Z,
    NONE;

    private static final WrapDomain UNWRAPPED = new WrapDomain.Noop();

    private static final double UNDIVIDED = 1.0;

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

    public double divisorIn(Context context) {
        return switch (this) {
            case X -> context.xDivisor();
            case Z -> context.zDivisor();
            case NONE -> UNDIVIDED;
        };
    }
}
