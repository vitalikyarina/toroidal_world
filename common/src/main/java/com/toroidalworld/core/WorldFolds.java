package com.toroidalworld.core;

import java.util.List;

import com.toroidalworld.shape.FlatShape;
import com.mojang.serialization.DataResult;

public final class WorldFolds {
    private static final String COUPLED_AXES = "its axes do not decompose";
    private static final String REVERSED_LOCAL_INDICES = "its seam reverses the local indices inside a chunk";

    public static final WorldFold NOOP = of(FlatShape.rectangle());

    public static WorldFold of(FlatShape shape) {
        return of(shape, List.of());
    }

    public static WorldFold of(FlatShape shape, List<ForeignFrame> foreignFrames) {
        verifyFoldable(shape).getOrThrow(IllegalArgumentException::new);

        return new WorldLoopTransformer(shape.bounds(), foreignFrames);
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
