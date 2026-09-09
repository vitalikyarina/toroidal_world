package com.toroidalworld.core;

import java.util.Objects;

import com.toroidalworld.api.v1.option.GenerationOptions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public final class CarriedShape {
    public static final String WRAPPING_KEY = "wrapping";

    public static final Codec<FlatShape> SHAPE_CODEC = FlatShape.CODEC
            .validate(WorldFolds::verifyFoldable)
            .validate(WorldFolds::verifyGeneratable);

    private final FlatShape shape;
    private final GenerationOptions generationOptions;
    private final WorldFold fold;

    public CarriedShape(FlatShape shape) {
        this(shape, GenerationOptions.DEFAULT);
    }

    public CarriedShape(FlatShape shape, GenerationOptions generationOptions) {
        this.shape = shape;
        this.generationOptions = generationOptions;
        this.fold = WorldFolds.of(shape, generationOptions);
    }

    public static MapCodec<CarriedShape> mapCodec(String keyPrefix) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                SHAPE_CODEC.fieldOf(keyPrefix + WRAPPING_KEY).forGetter(CarriedShape::shape),
                GenerationOptions.mapCodec(keyPrefix).forGetter(CarriedShape::generationOptions)
        ).apply(instance, CarriedShape::new));
    }

    public FlatShape shape() {
        return this.shape;
    }

    public GenerationOptions generationOptions() {
        return this.generationOptions;
    }

    public WorldFold fold() {
        return this.fold;
    }

    public CarriedShape withShape(FlatShape shape) {
        return new CarriedShape(shape, this.generationOptions);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CarriedShape carried
                && this.shape.equals(carried.shape)
                && this.generationOptions.equals(carried.generationOptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.shape, this.generationOptions);
    }
}
