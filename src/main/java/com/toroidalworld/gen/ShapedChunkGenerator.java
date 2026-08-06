package com.toroidalworld.gen;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.accessors.TransformerHolder;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.options.WorldLoopBounds;

import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;

// A chunk generator that carries a dimension's wrap bounds. The wrap engine resolves the transformer through this
// interface — WorldLoopAttachments (the level's transformer) and NoiseBasedChunkGeneratorMixin (the async noise fill)
// — rather than naming the concrete generator, so a second wrap-shaped generator plugs into the whole engine by
// implementing one method.
public interface ShapedChunkGenerator {
    String SETTINGS_KEY = "settings";
    String WRAPPING_KEY = "wrapping";

    WorldLoopBounds wrapping();

    // The one transformer for this dimension: the generator owns the bounds, so everything that wraps reads it from
    // here rather than building an equivalent copy of its own.
    WorldLoopTransformer transformer();

    // The structure state computes the concentric-ring positions but never keeps a reference to the generator that
    // made it, so each generator stamps the world's bounds onto it inside its createState override — the one moment
    // both sides are in hand. The override itself cannot be shared: the two generators extend different vanilla
    // classes whose createState implementations differ, so each must call its own super and hand the result here.
    default ChunkGeneratorStructureState stampTransformer(ChunkGeneratorStructureState state) {
        ((TransformerHolder) (Object) state).toroidal$setTransformer(transformer());
        return state;
    }

    // The generator-side twin of WorldLoopAttachments.wrappedTransformerOf: the transformer only when this generator
    // shapes a world that actually wraps, else null — so a caller can fetch and bail in one step. A mixin reaches it as
    // (ChunkGenerator) (Object) this, because a mixin's own type says nothing about the class it is applied to.
    static @Nullable WorldLoopTransformer wrappedTransformerOf(ChunkGenerator generator) {
        if (!(generator instanceof ShapedChunkGenerator shaped)) {
            return null;
        }

        WorldLoopTransformer transformer = shaped.transformer();
        return transformer.isWrapped() ? transformer : null;
    }
}
