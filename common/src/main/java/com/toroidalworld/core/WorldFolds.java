package com.toroidalworld.core;

import com.toroidalworld.shape.FlatShape;
import com.mojang.serialization.DataResult;

public final class WorldFolds {

    public static WorldLoopTransformer of(FlatShape shape) {
        if (!shape.decomposesPerAxis()) {
            throw new IllegalArgumentException(refusal(shape));
        }

        return new WorldLoopTransformer(shape.bounds());
    }

    public static DataResult<FlatShape> verifyDecomposable(FlatShape shape) {
        return shape.decomposesPerAxis()
                ? DataResult.success(shape)
                : DataResult.error(() -> refusal(shape));
    }

    private static String refusal(FlatShape shape) {
        return "The wrap engine has no fold for " + shape.identification() + " yet: its axes do not decompose";
    }

    private WorldFolds() {
    }
}
