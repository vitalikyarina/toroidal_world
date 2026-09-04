package com.toroidalworld.gen;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.accessors.ShapeStamp;
import com.toroidalworld.accessors.TransformerHolder;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.shape.FlatShape;
import com.mojang.serialization.Codec;

import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;

public interface ShapedChunkGenerator {
    String SETTINGS_KEY = "settings";
    String WRAPPING_KEY = "wrapping";

    Codec<FlatShape> SHAPE_CODEC = FlatShape.CODEC
            .validate(WorldFolds::verifyFoldable)
            .validate(WorldFolds::verifyGeneratable);

    FlatShape shape();

    WorldFold transformer();

    ChunkGenerator unshaped();

    default ChunkGeneratorStructureState stampTransformer(ChunkGeneratorStructureState state) {
        ((TransformerHolder) (Object) state).toroidal$setTransformer(transformer());
        return state;
    }

    static @Nullable FlatShape wrappedShapeOf(ChunkGenerator generator) {
        if (generator instanceof ShapedChunkGenerator shaped) {
            return shaped.transformer().isWrapped() ? shaped.shape() : null;
        }

        return generator instanceof ShapeStamp stamp && wrapped(stamp.toroidal$stampedTransformer()) != null
                ? stamp.toroidal$stampedShape()
                : null;
    }

    static @Nullable WorldFold wrappedTransformerOf(ChunkGenerator generator) {
        if (generator instanceof ShapedChunkGenerator shaped) {
            return wrapped(shaped.transformer());
        }

        return generator instanceof ShapeStamp stamp ? wrapped(stamp.toroidal$stampedTransformer()) : null;
    }

    private static @Nullable WorldFold wrapped(@Nullable WorldFold transformer) {
        return transformer != null && transformer.isWrapped() ? transformer : null;
    }
}
