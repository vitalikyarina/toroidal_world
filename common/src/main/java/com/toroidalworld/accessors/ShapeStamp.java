package com.toroidalworld.accessors;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.ClimateScale;
import com.toroidalworld.shape.FlatShape;

public interface ShapeStamp {
    default @Nullable FlatShape toroidal$stampedShape() {
        return null;
    }

    default @Nullable WorldFold toroidal$stampedTransformer() {
        return null;
    }

    default ClimateScale toroidal$stampedClimateScale() {
        return WorldFolds.CLIMATE_SCALE_DEFAULT;
    }

    default void toroidal$stamp(FlatShape shape, ClimateScale climateScale) {
    }

    default void toroidal$clearStamp() {
    }
}
