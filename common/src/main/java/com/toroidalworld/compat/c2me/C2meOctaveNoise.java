package com.toroidalworld.compat.c2me;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

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
