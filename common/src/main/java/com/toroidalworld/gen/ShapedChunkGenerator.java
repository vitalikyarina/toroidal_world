package com.toroidalworld.gen;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.accessors.ShapeStamp;
import com.toroidalworld.accessors.TransformerHolder;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.options.ClimateScale;
import com.toroidalworld.shape.FlatShape;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;

public interface ShapedChunkGenerator {
    String SETTINGS_KEY = "settings";
    String WRAPPING_KEY = "wrapping";
    String CLIMATE_SCALE_KEY = "climate_compression";

    Codec<FlatShape> SHAPE_CODEC = FlatShape.CODEC
            .validate(WorldFolds::verifyFoldable)
            .validate(WorldFolds::verifyGeneratable);

    MapCodec<ClimateScale> CLIMATE_SCALE_CODEC =
            ClimateScale.CODEC.optionalFieldOf(CLIMATE_SCALE_KEY, WorldFolds.CLIMATE_SCALE_DEFAULT);

    FlatShape shape();

    ClimateScale climateScale();

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

    static ClimateScale climateScaleOf(ChunkGenerator generator) {
        if (generator instanceof ShapedChunkGenerator shaped) {
            return shaped.climateScale();
        }

        return generator instanceof ShapeStamp stamp
                ? stamp.toroidal$stampedClimateScale()
                : WorldFolds.CLIMATE_SCALE_DEFAULT;
    }

    private static @Nullable WorldFold wrapped(@Nullable WorldFold transformer) {
        return transformer != null && transformer.isWrapped() ? transformer : null;
    }
}
