package com.toroidalworld.noise;

import java.util.Set;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class ClimateFields {
    private static final Set<ResourceKey<NormalNoise.NoiseParameters>> KEYS = Set.of(
            Noises.TEMPERATURE,
            Noises.TEMPERATURE_LARGE,
            Noises.TEMPERATURE_NETHER,
            Noises.VEGETATION,
            Noises.VEGETATION_LARGE,
            Noises.VEGETATION_NETHER);

    public static boolean isClimate(ResourceKey<NormalNoise.NoiseParameters> key) {
        return KEYS.contains(key);
    }

    private ClimateFields() {
    }
}
