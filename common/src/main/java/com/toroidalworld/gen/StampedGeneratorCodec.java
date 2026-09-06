package com.toroidalworld.gen;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.ToroidalWorld;
import com.toroidalworld.accessors.ShapeStamp;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.ClimateScale;
import com.toroidalworld.shape.FlatShape;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;

import net.minecraft.world.level.chunk.ChunkGenerator;

public final class StampedGeneratorCodec {
    static final String SHAPE_KEY = ToroidalWorld.MODID + ":" + ShapedChunkGenerator.WRAPPING_KEY;
    static final String CLIMATE_SCALE_KEY =
            ToroidalWorld.MODID + ":" + ShapedChunkGenerator.CLIMATE_SCALE_KEY;

    private static final MapCodec<ClimateScale> CLIMATE_SCALE_CODEC =
            ClimateScale.CODEC.optionalFieldOf(CLIMATE_SCALE_KEY, WorldFolds.CLIMATE_SCALE_DEFAULT);

    public static Codec<ChunkGenerator> over(Codec<ChunkGenerator> dispatch) {
        return new StampCarrying(dispatch);
    }

    private static @Nullable ShapeStamp stampToCarry(ChunkGenerator generator) {
        if (generator instanceof ShapedChunkGenerator) {
            return null;
        }

        return generator instanceof ShapeStamp stamp && stamp.toroidal$stampedShape() != null ? stamp : null;
    }

    private static @Nullable ShapeStamp stampToFill(ChunkGenerator generator) {
        if (generator instanceof ShapedChunkGenerator) {
            return null;
        }

        return generator instanceof ShapeStamp stamp ? stamp : null;
    }

    private record StampCarrying(Codec<ChunkGenerator> dispatch) implements Codec<ChunkGenerator> {
        @Override
        public <T> DataResult<T> encode(ChunkGenerator input, DynamicOps<T> ops, T prefix) {
            DataResult<T> encoded = this.dispatch.encode(input, ops, prefix);
            ShapeStamp stamp = stampToCarry(input);
            if (stamp == null) {
                return encoded;
            }

            FlatShape shape = stamp.toroidal$stampedShape();
            ClimateScale climateScale = stamp.toroidal$stampedClimateScale();
            return encoded.flatMap(map -> ShapedChunkGenerator.SHAPE_CODEC.encodeStart(ops, shape)
                    .flatMap(value -> ops.mergeToMap(map, ops.createString(SHAPE_KEY), value)))
                    .flatMap(map -> CLIMATE_SCALE_CODEC.encode(climateScale, ops, ops.mapBuilder())
                            .build(map));
        }

        @Override
        public <T> DataResult<Pair<ChunkGenerator, T>> decode(DynamicOps<T> ops, T input) {
            return this.dispatch.decode(ops, input).flatMap(decoded -> fillStamp(ops, input, decoded));
        }

        private static <T> DataResult<Pair<ChunkGenerator, T>> fillStamp(DynamicOps<T> ops, T input,
                Pair<ChunkGenerator, T> decoded) {
            ShapeStamp stamp = stampToFill(decoded.getFirst());
            MapLike<T> map = ops.getMap(input).result().orElse(null);
            T carried = map == null ? null : map.get(SHAPE_KEY);
            if (stamp == null || carried == null) {
                return DataResult.success(decoded);
            }

            return ShapedChunkGenerator.SHAPE_CODEC.parse(ops, carried)
                    .flatMap(shape -> CLIMATE_SCALE_CODEC.decode(ops, map).map(climateScale -> {
                        stamp.toroidal$stamp(shape, climateScale);
                        return decoded;
                    }));
        }
    }

    private StampedGeneratorCodec() {
    }
}
