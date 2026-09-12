package com.toroidalworld.engine.gen;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.ToroidalWorld;
import com.toroidalworld.accessors.ShapeStamp;
import com.toroidalworld.core.CarriedShape;
import com.toroidalworld.core.ShapedChunkGenerator;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;

import net.minecraft.world.level.chunk.ChunkGenerator;

public final class StampedGeneratorCodec {
    private static final String KEY_PREFIX = ToroidalWorld.MODID + ":";

    static final String SHAPE_KEY = KEY_PREFIX + CarriedShape.WRAPPING_KEY;

    private static final MapCodec<CarriedShape> CARRIED_CODEC = CarriedShape.mapCodec(KEY_PREFIX);

    public static Codec<ChunkGenerator> over(Codec<ChunkGenerator> dispatch) {
        return new StampCarrying(dispatch);
    }

    private static @Nullable ShapeStamp stampOf(ChunkGenerator generator) {
        if (generator instanceof ShapedChunkGenerator) {
            return null;
        }

        return generator instanceof ShapeStamp stamp ? stamp : null;
    }

    private record StampCarrying(Codec<ChunkGenerator> dispatch) implements Codec<ChunkGenerator> {
        @Override
        public <T> DataResult<T> encode(ChunkGenerator input, DynamicOps<T> ops, T prefix) {
            DataResult<T> encoded = this.dispatch.encode(input, ops, prefix);
            ShapeStamp stamp = stampOf(input);
            CarriedShape carried = stamp == null ? null : stamp.toroidal$carriedShape();
            if (carried == null) {
                return encoded;
            }

            return encoded.flatMap(map -> CARRIED_CODEC.encode(carried, ops, ops.mapBuilder()).build(map));
        }

        @Override
        public <T> DataResult<Pair<ChunkGenerator, T>> decode(DynamicOps<T> ops, T input) {
            return this.dispatch.decode(ops, input).flatMap(decoded -> fillStamp(ops, input, decoded));
        }

        private static <T> DataResult<Pair<ChunkGenerator, T>> fillStamp(DynamicOps<T> ops, T input,
                Pair<ChunkGenerator, T> decoded) {
            ShapeStamp stamp = stampOf(decoded.getFirst());
            MapLike<T> map = ops.getMap(input).result().orElse(null);
            if (stamp == null || map == null || map.get(SHAPE_KEY) == null) {
                return DataResult.success(decoded);
            }

            return CARRIED_CODEC.decode(ops, map).map(carried -> {
                stamp.toroidal$stamp(carried);
                return decoded;
            });
        }
    }

    private StampedGeneratorCodec() {
    }
}
