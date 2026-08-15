package com.toroidalworld.compat.c2me;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

// Whether C2ME's density function compiler owns the noise router. Its opts/dfc module replaces every router function
// with bytecode it generates from an AST of its own, so DensityFunctions$Noise.compute and $ShiftedNoise.compute —
// the two methods this mod wraps to hand the noise raw coordinates and park the horizontal scale — are never called
// again, and the climate field stops tiling at the seam with nothing in the log.
//
// Presence of the router replacement is the whole condition: the module's own compiler switch only decides whether
// that mixin compiles anything, and when it compiles nothing there is no AST for the compat mixins to rewrite.
public final class C2meDfc {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String ROUTER_MIXIN_CLASS = "com/ishland/c2me/opts/dfc/mixin/MixinNoiseConfig.class";

    private static final boolean PRESENT = probe();

    public static boolean present() {
        return PRESENT;
    }

    private static boolean probe() {
        boolean present = C2meDfc.class.getClassLoader().getResource(ROUTER_MIXIN_CLASS) != null;
        LOGGER.info("[c2me-compat] gate dfc_present={}", present);
        return present;
    }

    private C2meDfc() {
    }
}
