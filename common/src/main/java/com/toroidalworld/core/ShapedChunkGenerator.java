package com.toroidalworld.core;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.accessors.ShapeStamp;
import com.toroidalworld.accessors.TransformerHolder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;

public interface ShapedChunkGenerator {
    String SETTINGS_KEY = "settings";
    String WRAPPING_KEY = "wrapping";
    String NO_KEY_PREFIX = "";

    Codec<FlatShape> SHAPE_CODEC = FlatShape.CODEC
            .validate(WorldFolds::verifyFoldable)
            .validate(WorldFolds::verifyGeneratable);

    MapCodec<GenerationOptions> GENERATION_OPTIONS_CODEC = GenerationOptions.mapCodec(NO_KEY_PREFIX);

    FlatShape shape();

    GenerationOptions generationOptions();

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

    static WorldFold transformerOf(ChunkGenerator generator) {
        WorldFold transformer = wrappedTransformerOf(generator);
        return transformer != null ? transformer : WorldFolds.NOOP;
    }

    static GenerationOptions generationOptionsOf(ChunkGenerator generator) {
        if (generator instanceof ShapedChunkGenerator shaped) {
            return shaped.generationOptions();
        }

        return generator instanceof ShapeStamp stamp
                ? stamp.toroidal$stampedGenerationOptions()
                : GenerationOptions.DEFAULT;
    }

    private static @Nullable WorldFold wrapped(@Nullable WorldFold transformer) {
        return transformer != null && transformer.isWrapped() ? transformer : null;
    }
}
