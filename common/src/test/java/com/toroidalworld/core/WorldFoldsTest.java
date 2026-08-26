package com.toroidalworld.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.mojang.serialization.DataResult;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.Direction;

class WorldFoldsTest {
    private static final WorldLoopBounds SQUARE = WorldLoopBounds.ofWidth(32);
    private static final WorldLoopBounds X_ONLY =
            new WorldLoopBounds(new AxisBounds.Looped(-16, 16), AxisBounds.Unbounded.INSTANCE);
    private static final WorldLoopBounds Z_ONLY =
            new WorldLoopBounds(AxisBounds.Unbounded.INSTANCE, new AxisBounds.Looped(-16, 16));

    private static List<FlatShape> decomposable() {
        return List.of(
                FlatShape.rectangle(),
                FlatShape.cylinder(X_ONLY),
                FlatShape.cylinder(Z_ONLY),
                FlatShape.latticeTorus(SQUARE, 0));
    }

    private static final FlatShape SKEWED = FlatShape.latticeTorus(SQUARE, 5);

    private static List<FlatShape> mirrored() {
        return List.of(
                FlatShape.mirrored(Z_ONLY, Direction.Axis.X, 3),
                FlatShape.mirrored(SQUARE, Direction.Axis.Z, -7));
    }

    private static List<FlatShape> coupled() {
        return Stream.concat(Stream.of(SKEWED), mirrored().stream()).toList();
    }

    @Test
    void aDecomposableShapeGetsAFoldOverItsOwnBounds() {
        for (FlatShape shape : decomposable()) {
            assertEquals(shape.bounds(), WorldFolds.of(shape).bounds(), shape.toString());
        }
    }

    @Test
    void anUnboundedShapeStillGetsAFoldThatKnowsItDoesNotWrap() {
        assertFalse(WorldFolds.of(FlatShape.rectangle()).isWrapped());
        assertTrue(WorldFolds.of(FlatShape.cylinder(X_ONLY)).isWrapped());
    }

    @Test
    void noopIsTheRectanglesFold() {
        assertFalse(WorldFolds.NOOP.isWrapped());
        assertEquals(WorldLoopBounds.UNBOUNDED, WorldFolds.NOOP.bounds());
        assertTrue(WorldFolds.NOOP.decomposesPerAxis());
    }

    @Test
    void aCoupledShapeIsRefusedAndNamed() {
        for (FlatShape shape : coupled()) {
            IllegalArgumentException refused =
                    assertThrows(IllegalArgumentException.class, () -> WorldFolds.of(shape), shape.toString());
            assertTrue(refused.getMessage().contains(shape.identification().toString()), refused.getMessage());
        }
    }

    @Test
    void verifyPassesADecomposableShapeThrough() {
        for (FlatShape shape : decomposable()) {
            assertSame(shape, WorldFolds.verifyDecomposable(shape).getOrThrow(), shape.toString());
        }
    }

    @Test
    void verifyTurnsACoupledShapeIntoAnErrorRatherThanAThrow() {
        for (FlatShape shape : coupled()) {
            DataResult<FlatShape> result = WorldFolds.verifyDecomposable(shape);
            assertTrue(result.isError(), shape.toString());
            assertTrue(result.error().orElseThrow().message().contains(shape.identification().toString()),
                    result.toString());
        }
    }

    @Test
    void theFixturesSplitExactlyOnTheCapabilityFlag() {
        decomposable().forEach(shape -> assertTrue(shape.decomposesPerAxis(), shape.toString()));
        coupled().forEach(shape -> assertFalse(shape.decomposesPerAxis(), shape.toString()));
    }

    @Test
    void theLocalIndexFloorSeparatesASkewFromAMirror() {
        assertSame(SKEWED, WorldFolds.verifyPreservesLocalIndices(SKEWED).getOrThrow());

        for (FlatShape shape : mirrored()) {
            DataResult<FlatShape> result = WorldFolds.verifyPreservesLocalIndices(shape);
            assertTrue(result.isError(), shape.toString());
            assertTrue(result.error().orElseThrow().message().contains(shape.identification().toString()),
                    result.toString());
        }
    }

    @Test
    void everyShapeThatLosesItsLocalIndicesAlsoFailsToDecompose() {
        for (FlatShape shape : Stream.concat(decomposable().stream(), coupled().stream()).toList()) {
            if (!shape.preservesLocalIndices()) {
                assertFalse(shape.decomposesPerAxis(), shape.toString());
            }
        }
    }

    @Test
    void theFoldableGatePassesEveryShapeTheEngineCanCarry() {
        for (FlatShape shape : decomposable()) {
            assertSame(shape, WorldFolds.verifyFoldable(shape).getOrThrow(), shape.toString());
        }
    }

    @Test
    void theFoldableGateRefusesAMirrorOnTheFloorsOwnGrounds() {
        for (FlatShape shape : mirrored()) {
            assertEquals(
                    WorldFolds.verifyPreservesLocalIndices(shape).error().orElseThrow().message(),
                    WorldFolds.verifyFoldable(shape).error().orElseThrow().message(),
                    shape.toString());
        }
    }

    @Test
    void theFoldableGateStillRefusesASkewOnTheDecompositionGrounds() {
        assertEquals(
                WorldFolds.verifyDecomposable(SKEWED).error().orElseThrow().message(),
                WorldFolds.verifyFoldable(SKEWED).error().orElseThrow().message());
    }
}
