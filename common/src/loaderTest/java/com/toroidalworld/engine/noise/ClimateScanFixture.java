package com.toroidalworld.engine.noise;

import java.util.List;

import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.GenerationOptions;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldLoopBounds;
import com.toroidalworld.shape.WorldOptionSetup;
import com.toroidalworld.shape.torus.ClimateScale;
import com.toroidalworld.shape.torus.CompactBiomes;
import com.toroidalworld.shape.torus.GuaranteedLand;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class ClimateScanFixture {
    public static final int SCAN_Y_BLOCKS = 64;

    public static final long SEED_BASE = 0x5EED5EED5L;

    public record WorldType(String name, ResourceKey<NoiseGeneratorSettings> settings,
            ResourceKey<MultiNoiseBiomeSourceParameterList> biomes, boolean nether, boolean gated) {
    }

    public static final List<WorldType> TYPES = List.of(
            new WorldType("default", NoiseGeneratorSettings.OVERWORLD,
                    MultiNoiseBiomeSourceParameterLists.OVERWORLD, false, true),
            new WorldType("large biomes", NoiseGeneratorSettings.LARGE_BIOMES,
                    MultiNoiseBiomeSourceParameterLists.OVERWORLD, false, true),
            new WorldType("amplified", NoiseGeneratorSettings.AMPLIFIED,
                    MultiNoiseBiomeSourceParameterLists.OVERWORLD, false, true),
            new WorldType("nether", NoiseGeneratorSettings.NETHER,
                    MultiNoiseBiomeSourceParameterLists.NETHER, true, false));

    private static HolderLookup.Provider holders;
    private static HolderGetter<NormalNoise.NoiseParameters> noises;

    public static void bootstrapVanilla() {
        if (holders != null) {
            return;
        }

        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        WorldOptionSetup.registerAll(false);
        GenerationHookSetup.registerAll();
        holders = VanillaRegistries.createLookup();
        noises = holders.lookupOrThrow(Registries.NOISE);
    }

    public static MultiNoiseBiomeSource biomeSource(WorldType type) {
        Holder<MultiNoiseBiomeSourceParameterList> preset = holders
                .lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).getOrThrow(type.biomes());
        return MultiNoiseBiomeSource.createFromPreset(preset);
    }

    public static WorldFold torusOfWidth(int widthBlocks) {
        return WorldFolds.of(
                FlatShape.torus(WorldLoopBounds.ofWidth(widthBlocks / 16)));
    }

    public static WorldFold strongTorusOfWidth(int widthBlocks) {
        return WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(widthBlocks / 16)),
                GenerationOptions.DEFAULT.with(CompactBiomes.OPTION, ClimateScale.STRONG));
    }

    public static WorldFold guaranteedTorusOfWidth(int widthBlocks) {
        return WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(widthBlocks / 16)),
                GenerationOptions.DEFAULT.with(GuaranteedLand.OPTION, true));
    }

    public static WorldFold uncompressedTorusOfWidth(int widthBlocks) {
        return WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(widthBlocks / 16)),
                GenerationOptions.DEFAULT.with(CompactBiomes.OPTION, ClimateScale.OFF));
    }

    public static WorldFold cylinderOfWidth(int widthBlocks) {
        return WorldFolds.of(FlatShape.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.X, widthBlocks / 16)));
    }

    public static NormalNoise.NoiseParameters noiseParameters(ResourceKey<NormalNoise.NoiseParameters> key) {
        return noises.getOrThrow(key).value();
    }

    public static NoiseGeneratorSettings settingsOf(WorldType type) {
        return holders.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(type.settings()).value();
    }

    public static RandomState randomState(WorldType type, WorldFold fold, long seed) {
        return randomState(settingsOf(type), fold, seed);
    }

    public static RandomState randomState(NoiseGeneratorSettings settings, WorldFold fold, long seed) {
        return GenerationTransformerContext.withRouterBuild(fold.isWrapped() ? fold : null,
                () -> RandomState.create(settings, noises, seed));
    }

    private ClimateScanFixture() {
    }
}
