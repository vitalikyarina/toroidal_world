package com.toroidalworld.core;

import java.util.Optional;

import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.options.WorldLoopSizes;
import com.toroidalworld.shape.FlatShape;
import com.mojang.serialization.DataResult;

import net.minecraft.core.Direction;

public final class WorldFolds {
    private static final String COUPLED_AXES = "its axes do not decompose";
    private static final String REVERSED_LOCAL_INDICES = "its seam reverses the local indices inside a chunk";

    public static final WorldFold NOOP = of(FlatShape.rectangle());

    public static WorldFold of(FlatShape shape) {
        verifyFoldable(shape).getOrThrow(IllegalArgumentException::new);

        return new WorldLoopTransformer(shape.bounds());
    }

    public static DataResult<FlatShape> verifyFoldable(FlatShape shape) {
        return verifyPreservesLocalIndices(shape).flatMap(WorldFolds::verifyDecomposable);
    }

    public static DataResult<FlatShape> verifyGeneratable(FlatShape shape) {
        Optional<String> narrow = narrowAxis(shape.bounds().x(), Direction.Axis.X)
                .or(() -> narrowAxis(shape.bounds().z(), Direction.Axis.Z));
        return narrow.<DataResult<FlatShape>>map(reason -> DataResult.error(() -> reason))
                .orElseGet(() -> DataResult.success(shape));
    }

    private static Optional<String> narrowAxis(AxisBounds bounds, Direction.Axis axis) {
        if (!(bounds instanceof AxisBounds.Looped looped)
                || looped.chunkWidth() >= WorldLoopSizes.MIN_CHUNK_WIDTH) {
            return Optional.empty();
        }

        return Optional.of("A world looping " + WorldLoopSizes.describe(looped.chunkWidth()) + " on its "
                + axis.getName() + " axis is under the " + WorldLoopSizes.describe(WorldLoopSizes.MIN_CHUNK_WIDTH)
                + " chunk generation needs, and would crash while generating");
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
