package com.toroidalworld.client.shape.torus;

import java.util.List;
import java.util.OptionalDouble;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.FlatShape;
import com.toroidalworld.api.v1.option.GenerationOptions;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldLoopBounds;
import com.toroidalworld.shape.torus.ClimateCompression;
import com.toroidalworld.shape.torus.ClimateFields;
import com.toroidalworld.shape.torus.DensityNoises;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.NoiseHolder;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

final class ClimateFactorPreview {
    private static final double CLIMATE_XZ_SCALE = 0.25;

    private static final double HORIZONTAL_SHARE = 0.0;

    static OptionalDouble temperatureFactor(Screen parent, GenerationOptions generationOptions, int chunkWidth) {
        NoiseHolder temperature = temperatureNoise(parent);
        if (temperature == null) {
            return OptionalDouble.empty();
        }

        NormalNoise.NoiseParameters parameters = temperature.noiseData().value();
        boolean climateField = temperature.noiseData().unwrapKey().filter(ClimateFields::isClimate).isPresent();
        return OptionalDouble.of(ClimateCompression.factor(
                WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(chunkWidth)), generationOptions),
                climateField,
                parameters.amplitudes(),
                Math.pow(2.0, parameters.firstOctave()),
                CLIMATE_XZ_SCALE,
                HORIZONTAL_SHARE));
    }

    private static @Nullable NoiseHolder temperatureNoise(Screen parent) {
        if (!(parent instanceof CreateWorldScreen create)) {
            return null;
        }

        ChunkGenerator overworld = create.getUiState().getSettings().selectedDimensions()
                .get(LevelStem.OVERWORLD)
                .map(LevelStem::generator)
                .orElse(null);
        if (!(overworld instanceof NoiseBasedChunkGenerator noise)) {
            return null;
        }

        return climateNoiseOf(noise.generatorSettings().value().noiseRouter().temperature());
    }

    static @Nullable NoiseHolder climateNoiseOf(DensityFunction function) {
        List<NoiseHolder> climate = DensityNoises.matching(function, ClimateFields::isClimate);
        return climate.isEmpty() ? null : climate.getFirst();
    }

    private ClimateFactorPreview() {
    }
}
