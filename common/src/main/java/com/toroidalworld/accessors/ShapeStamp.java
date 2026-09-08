package com.toroidalworld.accessors;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.GenerationOptions;
import com.toroidalworld.core.WorldFold;

public interface ShapeStamp {
    default @Nullable FlatShape toroidal$stampedShape() {
        return null;
    }

    default @Nullable WorldFold toroidal$stampedTransformer() {
        return null;
    }

    default GenerationOptions toroidal$stampedGenerationOptions() {
        return GenerationOptions.DEFAULT;
    }

    default void toroidal$stamp(FlatShape shape, GenerationOptions generationOptions) {
    }

    default void toroidal$clearStamp() {
    }
}
