package com.toroidalworld.options;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record GenerationOptions(ClimateScale climateScale, boolean guaranteedLand) {
    public static final String CLIMATE_SCALE_KEY = "climate_compression";
    public static final String GUARANTEED_LAND_KEY = "guaranteed_land";

    public static final GenerationOptions DEFAULT = new GenerationOptions(ClimateScale.AUTO, false);

    public static final GenerationOptions NONE = new GenerationOptions(ClimateScale.OFF, false);

    public static MapCodec<GenerationOptions> mapCodec(String climateScaleKey, String guaranteedLandKey) {
        return RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                        ClimateScale.CODEC.optionalFieldOf(climateScaleKey, DEFAULT.climateScale())
                                .forGetter(GenerationOptions::climateScale),
                        Codec.BOOL.optionalFieldOf(guaranteedLandKey, DEFAULT.guaranteedLand())
                                .forGetter(GenerationOptions::guaranteedLand)
                ).apply(instance, GenerationOptions::new));
    }

    public GenerationOptions withClimateScale(ClimateScale chosen) {
        return new GenerationOptions(chosen, this.guaranteedLand);
    }

    public GenerationOptions withGuaranteedLand(boolean chosen) {
        return new GenerationOptions(this.climateScale, chosen);
    }
}
