package com.toroidalworld.shape.torus;

import com.toroidalworld.accessors.ClimateCompressionCache;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.engine.noise.ClimateScaleCompression;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class ClimateCompression {

    public record Resolved(WorldFold fold, double baseScale, double verticalShare, double factor) {
        boolean covers(WorldFold fold, double baseScale, double verticalShare) {
            return this.fold == fold && this.baseScale == baseScale && this.verticalShare == verticalShare;
        }
    }

    public static double resolve(ClimateCompressionCache cache, WorldFold fold, boolean climateField,
            DoubleList amplitudes, double lowestFreqInputFactor, double baseScale, double verticalShare) {
        Resolved resolved = cache.toroidal$climateCompression();
        if (resolved == null || !resolved.covers(fold, baseScale, verticalShare)) {
            resolved = new Resolved(fold, baseScale, verticalShare,
                    factor(fold, climateField, amplitudes, lowestFreqInputFactor, baseScale, verticalShare));
            cache.toroidal$climateCompression(resolved);
        }

        return resolved.factor();
    }

    public static double warpDivisor(DensityFunction.NoiseHolder noise, WorldFold fold, double xzScale,
            double verticalShare) {
        return xzScale * factorOf(noise, fold, xzScale, verticalShare);
    }

    public static double factor(WorldFold fold, boolean climateField, DoubleList amplitudes,
            double lowestFreqInputFactor, double baseScale, double verticalShare) {
        ClimateScale scale = fold.generationOptions().get(CompactBiomes.OPTION);
        if (scale.mode() == ClimateScale.Mode.OFF
                || !ClimateScaleCompression.compressible(fold, verticalShare)) {
            return ClimateScaleCompression.NO_COMPRESSION;
        }

        if (scale.isFixed() && !climateField) {
            return ClimateScaleCompression.NO_COMPRESSION;
        }

        if (scale.mode() == ClimateScale.Mode.CUSTOM) {
            return scale.factor();
        }

        double fitted = ClimateScaleCompression.fitted(fold, amplitudes, lowestFreqInputFactor, baseScale);
        return scale.mode() == ClimateScale.Mode.STRONG
                ? Math.max(fitted, ClimateScale.STRONG_FACTOR)
                : fitted;
    }

    private static double factorOf(DensityFunction.NoiseHolder noise, WorldFold fold, double baseScale,
            double verticalShare) {
        NormalNoise.NoiseParameters parameters = noise.noiseData().value();
        boolean climateField = noise.noiseData().unwrapKey().filter(ClimateFields::isClimate).isPresent();

        return factor(fold, climateField, parameters.amplitudes(), Math.pow(2.0, parameters.firstOctave()),
                baseScale, verticalShare);
    }

    private ClimateCompression() {
    }
}
