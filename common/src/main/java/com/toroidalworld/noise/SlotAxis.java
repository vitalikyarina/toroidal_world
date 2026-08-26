package com.toroidalworld.noise;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WrapDomain;
import com.toroidalworld.noise.GenerationTransformerContext.Context;

import net.minecraft.core.Direction;

public enum SlotAxis {
    X,
    Z,
    NONE;

    private static final WrapDomain UNWRAPPED = new WrapDomain.Noop();

    private static final double UNDIVIDED = 1.0;

    public boolean carriesWorldAxis() {
        return this != NONE;
    }

    public double samplerInput(double coord, double uniformScale) {
        return carriesWorldAxis() ? coord : coord * uniformScale;
    }

    public WrapDomain domainOf(WorldFold transformer) {
        return switch (this) {
            case X -> transformer.blockDomain(Direction.Axis.X);
            case Z -> transformer.blockDomain(Direction.Axis.Z);
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
