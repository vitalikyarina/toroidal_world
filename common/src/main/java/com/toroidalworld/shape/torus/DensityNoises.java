package com.toroidalworld.shape.torus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.NoiseHolder;
import net.minecraft.world.level.levelgen.DensityFunction.Visitor;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class DensityNoises {

    public static List<NoiseHolder> matching(DensityFunction function,
            Predicate<ResourceKey<NormalNoise.NoiseParameters>> key) {
        List<NoiseHolder> found = new ArrayList<>();
        function.mapAll(new Visitor() {
            @Override
            public DensityFunction apply(DensityFunction input) {
                return input;
            }

            @Override
            public NoiseHolder visitNoise(NoiseHolder noise) {
                if (noise.noiseData().unwrapKey().filter(key).isPresent()) {
                    found.add(noise);
                }

                return noise;
            }
        });

        return found;
    }

    private DensityNoises() {
    }
}
