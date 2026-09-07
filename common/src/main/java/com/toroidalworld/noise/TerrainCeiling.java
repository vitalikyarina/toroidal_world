package com.toroidalworld.noise;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;

public final class TerrainCeiling {
    private static final String VANILLA_NAMESPACE = "minecraft";

    private static final List<String> JAGGEDNESS_PATHS = List.of(
            "overworld/jaggedness",
            "overworld_large_biomes/jaggedness");

    private static final double BLOCKS_PER_DENSITY_UNIT = 128.0;

    private static final double BASE_BLOCKS = 40.0;

    private static final double RAMP_BLOCKS = 16.0;

    private static final double PENALTY = 0.25;

    @SuppressWarnings("deprecation")
    public static NoiseGeneratorSettings withCeiling(NoiseGeneratorSettings settings) {
        NoiseRouter ceilinged = withCeiling(settings.noiseRouter());
        return ceilinged == settings.noiseRouter()
                ? settings
                : new NoiseGeneratorSettings(
                        settings.noiseSettings(),
                        settings.defaultBlock(),
                        settings.defaultFluid(),
                        ceilinged,
                        settings.surfaceRule(),
                        settings.spawnTarget(),
                        settings.seaLevel(),
                        settings.disableMobGeneration(),
                        settings.aquifersEnabled(),
                        settings.oreVeinsEnabled(),
                        settings.useLegacyRandomSource());
    }

    public static @Nullable DensityFunction ceiling(NoiseGeneratorSettings settings) {
        return ceilingOf(settings.noiseRouter());
    }

    private static @Nullable DensityFunction ceilingOf(NoiseRouter source) {
        DensityFunctions.TwoArgumentSimpleFunction jaggedness = jaggednessProduct(source.finalDensity());
        if (jaggedness == null) {
            return null;
        }

        DensityFunction spline = jaggednessSpline(jaggedness);
        DensityFunction noise = spline == jaggedness.argument1()
                ? jaggedness.argument2()
                : jaggedness.argument1();
        double lift = BLOCKS_PER_DENSITY_UNIT * noise.maxValue();
        if (!Double.isFinite(lift)) {
            return null;
        }

        DensityFunction headroom = DensityFunctions.mul(DensityFunctions.constant(lift),
                DensityFunctions.max(spline, DensityFunctions.zero()));
        return DensityFunctions.add(
                DensityFunctions.add(DensityFunctions.flatCache(source.initialDensityWithoutJaggedness()),
                        DensityFunctions.constant(BASE_BLOCKS)),
                headroom);
    }

    static NoiseRouter withCeiling(NoiseRouter source) {
        DensityFunction ceiling = ceilingOf(source);
        if (ceiling == null) {
            return source;
        }

        DensityFunction aboveCeiling = DensityFunctions.add(worldY(),
                DensityFunctions.mul(DensityFunctions.constant(-1.0), ceiling));
        DensityFunction ramp = DensityFunctions
                .mul(aboveCeiling, DensityFunctions.constant(1.0 / RAMP_BLOCKS))
                .clamp(0.0, 1.0);

        return withFinalDensity(source, DensityFunctions.add(source.finalDensity(),
                DensityFunctions.mul(ramp, DensityFunctions.constant(-PENALTY))));
    }

    private static NoiseRouter withFinalDensity(NoiseRouter source, DensityFunction finalDensity) {
        return new NoiseRouter(
                source.barrierNoise(),
                source.fluidLevelFloodednessNoise(),
                source.fluidLevelSpreadNoise(),
                source.lavaNoise(),
                source.temperature(),
                source.vegetation(),
                source.continents(),
                source.erosion(),
                source.depth(),
                source.ridges(),
                source.initialDensityWithoutJaggedness(),
                finalDensity,
                source.veinToggle(),
                source.veinRidged(),
                source.veinGap());
    }

    private static DensityFunction worldY() {
        int belowBottom = DimensionType.MIN_Y * 2;
        int aboveTop = DimensionType.MAX_Y * 2;
        return DensityFunctions.yClampedGradient(belowBottom, aboveTop, belowBottom, aboveTop);
    }

    private static DensityFunctions.@Nullable TwoArgumentSimpleFunction jaggednessProduct(DensityFunction root) {
        Deque<DensityFunction> pending = new ArrayDeque<>();
        Set<DensityFunction> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        pending.add(root);
        seen.add(root);

        while (!pending.isEmpty()) {
            DensityFunction node = pending.remove();
            if (node instanceof DensityFunctions.TwoArgumentSimpleFunction candidate
                    && candidate.type() == DensityFunctions.TwoArgumentSimpleFunction.Type.MUL
                    && (isJaggedness(candidate.argument1()) || isJaggedness(candidate.argument2()))) {
                return candidate;
            }

            if (node instanceof DensityFunctions.HolderHolder(Holder<DensityFunction> holder) && !holder.isBound()) {
                continue;
            }

            for (DensityFunction child : DensityFunctionChildren.of(node)) {
                if (seen.add(child)) {
                    pending.add(child);
                }
            }
        }

        return null;
    }

    private static DensityFunction jaggednessSpline(DensityFunctions.TwoArgumentSimpleFunction product) {
        return isJaggedness(product.argument1()) ? product.argument1() : product.argument2();
    }

    private static boolean isJaggedness(DensityFunction function) {
        if (!(function instanceof DensityFunctions.HolderHolder(Holder<DensityFunction> holder))) {
            return false;
        }

        ResourceKey<DensityFunction> key = holder.unwrapKey().orElse(null);
        return key != null
                && key.location().getNamespace().equals(VANILLA_NAMESPACE)
                && JAGGEDNESS_PATHS.contains(key.location().getPath());
    }

    private TerrainCeiling() {
    }
}
