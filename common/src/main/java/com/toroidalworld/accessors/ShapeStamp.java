package com.toroidalworld.accessors;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.shape.FlatShape;

public interface ShapeStamp {
    default @Nullable FlatShape toroidal$stampedShape() {
        return null;
    }

    default @Nullable WorldFold toroidal$stampedTransformer() {
        return null;
    }

    default boolean toroidal$stampedClimateCompression() {
        return WorldFolds.CLIMATE_COMPRESSION_DEFAULT;
    }

    default void toroidal$stamp(FlatShape shape, boolean climateCompression) {
    }

    default void toroidal$clearStamp() {
    }
}
