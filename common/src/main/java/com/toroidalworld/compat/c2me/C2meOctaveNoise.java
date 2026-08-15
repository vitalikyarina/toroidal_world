package com.toroidalworld.compat.c2me;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

// Whether C2ME's math optimisations own PerlinNoise. Its opts/math module overwrites the three-argument getValue with
// an octave loop of its own, which no longer delegates to the five-argument overload this mod wraps — so the periodic
// walk stops being called without a single line in the log, and the low-frequency fields stop tiling at the seam.
//
// Presence is the whole condition: the module carries no config key of its own.
public final class C2meOctaveNoise {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String OCTAVE_MIXIN_CLASS =
            "com/ishland/c2me/opts/math/mixin/MixinOctavePerlinNoiseSampler.class";

    private static final boolean PRESENT = probe();

    public static boolean present() {
        return PRESENT;
    }

    private static boolean probe() {
        boolean present = C2meOctaveNoise.class.getClassLoader().getResource(OCTAVE_MIXIN_CLASS) != null;
        LOGGER.info("[c2me-compat] gate octave_noise_present={}", present);
        return present;
    }

    private C2meOctaveNoise() {
    }
}
