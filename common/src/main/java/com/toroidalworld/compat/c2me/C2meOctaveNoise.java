package com.toroidalworld.compat.c2me;

import com.mojang.logging.LogUtils;
import com.toroidalworld.compat.ModPresence;
import com.toroidalworld.compat.ModSymbol;

public final class C2meOctaveNoise {
    static final ModSymbol OCTAVE_SAMPLER_VALUE = new ModSymbol(
            "com/ishland/c2me/opts/math/mixin/MixinOctavePerlinNoiseSampler", "getValue", "(DDD)D");

    private static final ModPresence GATE = ModPresence.of(LogUtils.getLogger(),
            "com/ishland/c2me/opts/math/mixin/MixinOctavePerlinNoiseSampler.class",
            "[c2me-compat] gate octave_noise_present", OCTAVE_SAMPLER_VALUE);

    public static boolean present() {
        return GATE.present();
    }

    private C2meOctaveNoise() {
    }
}
