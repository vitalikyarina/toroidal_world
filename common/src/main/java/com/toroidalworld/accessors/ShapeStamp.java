package com.toroidalworld.accessors;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.shape.FlatShape;

public interface ShapeStamp {
    default @Nullable FlatShape toroidal$stampedShape() {
        return null;
    }

    default @Nullable WorldFold toroidal$stampedTransformer() {
        return null;
    }

    default void toroidal$stamp(FlatShape shape) {
    }

    default void toroidal$clearStamp() {
    }
}
