package com.toroidalworld.core;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.accessors.ShapeStamp;
import com.toroidalworld.accessors.TransformerHolder;
import com.mojang.serialization.MapCodec;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;

public interface ShapedChunkGenerator {
    String SETTINGS_KEY = "settings";
    String NO_KEY_PREFIX = "";

    MapCodec<CarriedShape> CARRIED_CODEC = CarriedShape.mapCodec(NO_KEY_PREFIX);

    CarriedShape carriedShape();

    ChunkGenerator unshaped();

    default WorldFold transformer() {
        return carriedShape().fold();
    }

    default ChunkGeneratorStructureState stampTransformer(ChunkGeneratorStructureState state) {
        ((TransformerHolder) (Object) state).toroidal$setTransformer(transformer());
        return state;
    }

    static @Nullable CarriedShape carriedShapeOf(ChunkGenerator generator) {
        if (generator instanceof ShapedChunkGenerator shaped) {
            return wrapped(shaped.carriedShape());
        }

        return generator instanceof ShapeStamp stamp ? wrapped(stamp.toroidal$carriedShape()) : null;
    }

    static @Nullable CarriedShape carriedShapeOf(ServerLevel level) {
        return carriedShapeOf(level.getChunkSource().getGenerator());
    }

    static @Nullable WorldFold wrappedTransformerOf(ChunkGenerator generator) {
        CarriedShape carried = carriedShapeOf(generator);
        return carried == null ? null : carried.fold();
    }

    static WorldFold transformerOf(ChunkGenerator generator) {
        WorldFold transformer = wrappedTransformerOf(generator);
        return transformer != null ? transformer : WorldFolds.NOOP;
    }

    private static @Nullable CarriedShape wrapped(@Nullable CarriedShape carried) {
        return carried != null && carried.fold().isWrapped() ? carried : null;
    }
}
