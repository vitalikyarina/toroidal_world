package com.toroidalworld.engine.noise;

import com.toroidalworld.core.WrapDomain;

public final class DomainWarp {
    public static double apply(WrapDomain domain, int block, double shift, double xzScale) {
        return domain.wrap(block) + shift / xzScale;
    }

    private DomainWarp() {
    }
}
