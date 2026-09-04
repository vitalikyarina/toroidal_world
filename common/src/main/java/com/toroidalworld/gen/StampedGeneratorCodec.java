package com.toroidalworld.gen;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.ToroidalWorld;
import com.toroidalworld.accessors.ShapeStamp;
import com.toroidalworld.shape.FlatShape;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import net.minecraft.world.level.chunk.ChunkGenerator;

public final class StampedGeneratorCodec {
    static final String SHAPE_KEY = ToroidalWorld.MODID + ":" + ShapedChunkGenerator.WRAPPING_KEY;

    public static Codec<ChunkGenerator> over(Codec<ChunkGenerator> dispatch) {
        return new StampCarrying(dispatch);
    }

    private static @Nullable FlatShape stampToCarry(ChunkGenerator generator) {
        if (generator instanceof ShapedChunkGenerator) {
            return null;
        }

        return generator instanceof ShapeStamp stamp ? stamp.toroidal$stampedShape() : null;
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
            FlatShape shape = stampToCarry(input);
            if (shape == null) {
                return encoded;
            }

            return encoded.flatMap(map -> ShapedChunkGenerator.SHAPE_CODEC.encodeStart(ops, shape)
                    .flatMap(value -> ops.mergeToMap(map, ops.createString(SHAPE_KEY), value)));
        }

        @Override
        public <T> DataResult<Pair<ChunkGenerator, T>> decode(DynamicOps<T> ops, T input) {
            return this.dispatch.decode(ops, input).flatMap(decoded -> fillStamp(ops, input, decoded));
        }

        private static <T> DataResult<Pair<ChunkGenerator, T>> fillStamp(DynamicOps<T> ops, T input,
                Pair<ChunkGenerator, T> decoded) {
            ShapeStamp stamp = stampToFill(decoded.getFirst());
            T carried = ops.getMap(input).result().map(map -> map.get(SHAPE_KEY)).orElse(null);
            if (stamp == null || carried == null) {
                return DataResult.success(decoded);
            }

            return ShapedChunkGenerator.SHAPE_CODEC.parse(ops, carried).map(shape -> {
                stamp.toroidal$stamp(shape);
                return decoded;
            });
        }
    }

    private StampedGeneratorCodec() {
    }
}
