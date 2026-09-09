package com.toroidalworld.engine.noise;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WrapDomain;

import net.minecraft.world.level.levelgen.DensityFunction;

public final class DomainWarp {
    public record Divisor(WorldFold fold, double value) {
    }

    public static double apply(WrapDomain domain, int block, double shift, double divisor) {
        return domain.wrap(block) + shift / divisor;
    }

    public static double divisor(DensityFunction.NoiseHolder noise, WorldFold fold, double xzScale,
            double verticalShare) {
        return xzScale * ClimateScaleCompression.factorOf(noise, fold, xzScale, verticalShare);
    }

    private DomainWarp() {
    }
}
