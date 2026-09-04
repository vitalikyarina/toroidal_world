package com.toroidalworld.compat.c2me;

import com.mojang.logging.LogUtils;
import com.toroidalworld.compat.ModPresence;

public final class C2meOctaveNoise {
    private static final ModPresence GATE = ModPresence.of(LogUtils.getLogger(),
            "com/ishland/c2me/opts/math/mixin/MixinOctavePerlinNoiseSampler.class",
            "[c2me-compat] gate octave_noise_present");

    public static boolean present() {
        return GATE.present();
    }

    private C2meOctaveNoise() {
    }
}
