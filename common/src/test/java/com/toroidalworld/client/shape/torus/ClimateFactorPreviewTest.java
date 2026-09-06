package com.toroidalworld.client.shape.torus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.NoiseHolder;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class ClimateFactorPreviewTest {
    private static HolderLookup.Provider worldgen;

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        worldgen = VanillaRegistries.createLookup();
    }

    private static DensityFunction temperatureOf(ResourceKey<NoiseGeneratorSettings> settings) {
        return worldgen.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(settings).value()
                .noiseRouter().temperature();
    }

    private static ResourceKey<NormalNoise.NoiseParameters> foundIn(ResourceKey<NoiseGeneratorSettings> settings) {
        NoiseHolder found = ClimateFactorPreview.climateNoiseOf(temperatureOf(settings));
        assertNotNull(found, settings.identifier() + ": the router carries no climate noise to find");
        return found.noiseData().unwrapKey().orElseThrow();
    }

    @Test
    void theRoutersOwnTemperatureIsFoundPastTheShiftNoisesThatPrecedeIt() {
        for (ResourceKey<NoiseGeneratorSettings> settings : List.of(
                NoiseGeneratorSettings.OVERWORLD, NoiseGeneratorSettings.AMPLIFIED)) {
            assertEquals(Noises.TEMPERATURE, foundIn(settings), settings.identifier().toString());
        }
    }

    @Test
    void largeBiomesIsFoundOnItsOwnCoarserLadder() {
        assertEquals(Noises.TEMPERATURE_LARGE, foundIn(NoiseGeneratorSettings.LARGE_BIOMES),
                NoiseGeneratorSettings.LARGE_BIOMES.identifier().toString());
    }

    @Test
    void theNethersTemperatureIsFoundToo() {
        assertEquals(Noises.TEMPERATURE_NETHER, foundIn(NoiseGeneratorSettings.NETHER),
                NoiseGeneratorSettings.NETHER.identifier().toString());
    }

    @Test
    void aRouterWithoutAClimateNoiseIsAnswerlessRatherThanWrong() {
        assertNull(ClimateFactorPreview.climateNoiseOf(temperatureOf(NoiseGeneratorSettings.END)),
                "the End router carries no climate noise, so nothing may be reported as one");
    }
}
