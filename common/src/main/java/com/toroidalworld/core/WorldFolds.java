package com.toroidalworld.core;

import com.toroidalworld.shape.FlatShape;
import com.mojang.serialization.DataResult;

public final class WorldFolds {
    private static final String COUPLED_AXES = "its axes do not decompose";
    private static final String REVERSED_LOCAL_INDICES = "its seam reverses the local indices inside a chunk";

    public static WorldLoopTransformer of(FlatShape shape) {
        verifyFoldable(shape).getOrThrow(IllegalArgumentException::new);

        return new WorldLoopTransformer(shape.bounds());
    }

    public static DataResult<FlatShape> verifyFoldable(FlatShape shape) {
        return verifyPreservesLocalIndices(shape).flatMap(WorldFolds::verifyDecomposable);
    }

    public static DataResult<FlatShape> verifyPreservesLocalIndices(FlatShape shape) {
        return shape.preservesLocalIndices()
                ? DataResult.success(shape)
                : DataResult.error(() -> refusal(shape, REVERSED_LOCAL_INDICES));
    }

    public static DataResult<FlatShape> verifyDecomposable(FlatShape shape) {
        return shape.decomposesPerAxis()
                ? DataResult.success(shape)
                : DataResult.error(() -> refusal(shape, COUPLED_AXES));
    }

    private static String refusal(FlatShape shape, String reason) {
        return "The wrap engine has no fold for " + shape.identification() + " yet: " + reason;
    }

    private WorldFolds() {
    }
}
