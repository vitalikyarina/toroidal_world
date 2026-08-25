package com.toroidalworld.gen;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.accessors.TransformerHolder;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.shape.FlatShape;
import com.mojang.serialization.Codec;

import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;

public interface ShapedChunkGenerator {
    String SETTINGS_KEY = "settings";
    String WRAPPING_KEY = "wrapping";

    Codec<FlatShape> SHAPE_CODEC = FlatShape.CODEC.validate(WorldFolds::verifyDecomposable);

    FlatShape shape();

    WorldLoopTransformer transformer();

    default ChunkGeneratorStructureState stampTransformer(ChunkGeneratorStructureState state) {
        ((TransformerHolder) (Object) state).toroidal$setTransformer(transformer());
        return state;
    }

    static @Nullable FlatShape wrappedShapeOf(ChunkGenerator generator) {
        return wrappedTransformerOf(generator) == null ? null : ((ShapedChunkGenerator) generator).shape();
    }

    static @Nullable WorldLoopTransformer wrappedTransformerOf(ChunkGenerator generator) {
        if (!(generator instanceof ShapedChunkGenerator shaped)) {
            return null;
        }

        WorldLoopTransformer transformer = shaped.transformer();
        return transformer.isWrapped() ? transformer : null;
    }
}
