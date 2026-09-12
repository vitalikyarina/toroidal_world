package com.toroidalworld.shape.torus;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.StringRepresentable;

public record ClimateScale(Mode mode, int factor) {
    private static final String MODE_KEY = "mode";
    private static final String FACTOR_KEY = "factor";

    public static final int STRONG_FACTOR = 4;
    public static final int CUSTOM_MIN = 1;
    public static final int CUSTOM_MAX = 16;

    public static final ClimateScale OFF = new ClimateScale(Mode.OFF, STRONG_FACTOR);
    public static final ClimateScale AUTO = new ClimateScale(Mode.AUTO, STRONG_FACTOR);
    public static final ClimateScale STRONG = new ClimateScale(Mode.STRONG, STRONG_FACTOR);

    private static final Codec<ClimateScale> RECORD_CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Mode.CODEC.fieldOf(MODE_KEY).forGetter(ClimateScale::mode),
                    Codec.intRange(CUSTOM_MIN, CUSTOM_MAX).optionalFieldOf(FACTOR_KEY, STRONG_FACTOR)
                            .forGetter(ClimateScale::factor)
            ).apply(instance, ClimateScale::new));

    public static final Codec<ClimateScale> CODEC = Codec.either(Codec.BOOL, RECORD_CODEC)
            .xmap(either -> either.map(ClimateScale::ofBoolean, scale -> scale), ClimateScale::toEither);

    public ClimateScale {
        if (mode != Mode.CUSTOM) {
            factor = STRONG_FACTOR;
        }
    }

    public static ClimateScale ofBoolean(boolean compression) {
        return compression ? AUTO : OFF;
    }

    public static ClimateScale custom(int factor) {
        return new ClimateScale(Mode.CUSTOM, factor);
    }

    public boolean isFixed() {
        return this.mode == Mode.STRONG || this.mode == Mode.CUSTOM;
    }

    public ClimateScale withMode(Mode chosen) {
        return new ClimateScale(chosen, this.factor);
    }

    private Either<Boolean, ClimateScale> toEither() {
        return switch (this.mode) {
            case OFF -> Either.left(false);
            case AUTO -> Either.left(true);
            case STRONG, CUSTOM -> Either.right(this);
        };
    }

    public enum Mode implements StringRepresentable {
        OFF("off"),
        AUTO("auto"),
        STRONG("strong"),
        CUSTOM("custom");

        public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);

        private final String serializedName;

        Mode(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return this.serializedName;
        }
    }
}
