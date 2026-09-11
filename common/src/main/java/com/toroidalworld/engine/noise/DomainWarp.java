package com.toroidalworld.engine.noise;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WrapDomain;

public final class DomainWarp {
    public record Divisor(WorldFold fold, double value) {
    }

    public static double apply(WrapDomain domain, int block, double shift, double divisor) {
        return domain.wrap(block) + shift / divisor;
    }

    private DomainWarp() {
    }
}
