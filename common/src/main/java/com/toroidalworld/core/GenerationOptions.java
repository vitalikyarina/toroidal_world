package com.toroidalworld.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

public final class GenerationOptions {
    public static final GenerationOptions DEFAULT = new GenerationOptions(Map.of(), false);

    public static final GenerationOptions NONE = new GenerationOptions(Map.of(), true);

    private final Map<WorldOption<?>, Object> chosen;

    private final boolean inert;

    private GenerationOptions(Map<WorldOption<?>, Object> chosen, boolean inert) {
        this.chosen = chosen;
        this.inert = inert;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(WorldOption<T> option) {
        if (this.inert) {
            return option.inertValue();
        }

        Object value = this.chosen.get(option);
        return value == null ? option.defaultValue() : (T) value;
    }

    public <T> GenerationOptions with(WorldOption<T> option, T value) {
        if (this.inert) {
            return this;
        }

        Map<WorldOption<?>, Object> grown = new HashMap<>(this.chosen);
        grown.put(option, value);
        return new GenerationOptions(Map.copyOf(grown), false);
    }

    public static MapCodec<GenerationOptions> mapCodec(String keyPrefix) {
        return new Assembled(keyPrefix);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof GenerationOptions options
                && this.inert == options.inert
                && this.chosen.equals(options.chosen);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.chosen, this.inert);
    }

    private static final class Assembled extends MapCodec<GenerationOptions> {
        private final String keyPrefix;

        private Assembled(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return WorldOptions.all().stream().map(option -> ops.createString(this.keyPrefix + option.key()));
        }

        @Override
        public <T> DataResult<GenerationOptions> decode(DynamicOps<T> ops, MapLike<T> input) {
            DataResult<GenerationOptions> decoded = DataResult.success(DEFAULT);
            for (WorldOption<?> option : WorldOptions.all()) {
                decoded = decoded.flatMap(options -> read(options, option, this.keyPrefix, ops, input));
            }

            return decoded;
        }

        @Override
        public <T> RecordBuilder<T> encode(GenerationOptions input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            RecordBuilder<T> builder = prefix;
            for (WorldOption<?> option : WorldOptions.all()) {
                builder = write(input, option, this.keyPrefix, ops, builder);
            }

            return builder;
        }

        private static <T, V> DataResult<GenerationOptions> read(GenerationOptions into, WorldOption<V> option,
                String keyPrefix, DynamicOps<T> ops, MapLike<T> input) {
            T value = input.get(keyPrefix + option.key());
            return value == null
                    ? DataResult.success(into)
                    : option.codec().parse(ops, value).map(parsed -> into.with(option, parsed));
        }

        private static <T, V> RecordBuilder<T> write(GenerationOptions from, WorldOption<V> option,
                String keyPrefix, DynamicOps<T> ops, RecordBuilder<T> builder) {
            V value = from.get(option);
            return value.equals(option.defaultValue())
                    ? builder
                    : builder.add(keyPrefix + option.key(), option.codec().encodeStart(ops, value));
        }
    }
}
