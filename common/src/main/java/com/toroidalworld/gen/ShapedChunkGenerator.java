package com.toroidalworld.gen;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.accessors.TransformerHolder;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.options.WorldLoopBounds;

import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;

public interface ShapedChunkGenerator {
    String SETTINGS_KEY = "settings";
    String WRAPPING_KEY = "wrapping";

    WorldLoopBounds wrapping();

    WorldLoopTransformer transformer();

    default ChunkGeneratorStructureState stampTransformer(ChunkGeneratorStructureState state) {
        ((TransformerHolder) (Object) state).toroidal$setTransformer(transformer());
        return state;
    }

    static @Nullable WorldLoopTransformer wrappedTransformerOf(ChunkGenerator generator) {
        if (!(generator instanceof ShapedChunkGenerator shaped)) {
            return null;
        }

        WorldLoopTransformer transformer = shaped.transformer();
        return transformer.isWrapped() ? transformer : null;
    }
}
