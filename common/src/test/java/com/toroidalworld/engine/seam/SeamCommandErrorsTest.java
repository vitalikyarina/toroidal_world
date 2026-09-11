package com.toroidalworld.engine.seam;

import static com.toroidalworld.core.WorldFoldFixture.SQUARE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.CoordinateConstants;
import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.ForeignFrame;
import com.toroidalworld.core.ForeignSpan;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldLoopBounds;
import com.toroidalworld.core.WorldLoopBounds.AxisBounds;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

class SeamCommandErrorsTest {
    private static final int MIN_BLOCK = -512;
    private static final int MAX_BLOCK = 511;
    private static final double FAR_OUTSIDE = 20481032.0;

    private static final int PLOT_RADIUS_CHUNKS = 64;
    private static final int FAR_OUTSIDE_CHUNK = (int) FAR_OUTSIDE / CoordinateConstants.CHUNK_WIDTH;
    private static final ForeignSpan PLOT_CHUNKS = new ForeignSpan(
            FAR_OUTSIDE_CHUNK - PLOT_RADIUS_CHUNKS, FAR_OUTSIDE_CHUNK + PLOT_RADIUS_CHUNKS);

    private static final int PLOT_BLOCK = FAR_OUTSIDE_CHUNK * CoordinateConstants.CHUNK_WIDTH;
    private static final int WIDE_REGION = 1023;
    private static final int REGION_TOP = 63;

    private static final WorldFold TORUS = WorldFolds.of(FlatShape.torus(SQUARE));

    private static final WorldFold FRAMED = WorldFolds.of(
            FlatShape.torus(SQUARE), List.of(new ForeignFrame(PLOT_CHUNKS, PLOT_CHUNKS)));

    private static final WorldFold X_ONLY = WorldFolds.of(FlatShape.cylinder(
            new WorldLoopBounds(new AxisBounds.Looped(-32, 32), AxisBounds.Unbounded.INSTANCE)));

    @Test
    void refusesACoordinateOverTheBounds() {
        CommandSyntaxException refusal = assertThrows(CommandSyntaxException.class,
                () -> SeamCommandErrors.requireInsideWorld(TORUS, Direction.Axis.X, 600.5));

        assertArrayEquals(new Object[] {600L, MIN_BLOCK, MAX_BLOCK}, argsOf(refusal));
    }

    @Test
    void refusesACoordinateUnderTheBounds() {
        CommandSyntaxException refusal = assertThrows(CommandSyntaxException.class,
                () -> SeamCommandErrors.requireInsideWorld(TORUS, Direction.Axis.Z, -512.5));

        assertArrayEquals(new Object[] {-513L, MIN_BLOCK, MAX_BLOCK}, argsOf(refusal));
    }

    @Test
    void resolvesACoordinateInsideTheBounds() {
        assertDoesNotThrow(() -> {
            SeamCommandErrors.requireInsideWorld(TORUS, Direction.Axis.X, (double) MIN_BLOCK);
            SeamCommandErrors.requireInsideWorld(TORUS, Direction.Axis.X, MAX_BLOCK + 0.9);
            SeamCommandErrors.requireInsideWorld(TORUS, Direction.Axis.Z, 0.0);
        });
    }

    @Test
    void refusesNothingOnAnUnboundedAxis() {
        assertDoesNotThrow(() -> SeamCommandErrors.requireInsideWorld(X_ONLY, Direction.Axis.Z, FAR_OUTSIDE));
        assertDoesNotThrow(() -> SeamCommandErrors.requireInsideWorld(WorldFolds.NOOP, Direction.Axis.X, FAR_OUTSIDE));
        assertThrows(CommandSyntaxException.class,
                () -> SeamCommandErrors.requireInsideWorld(X_ONLY, Direction.Axis.X, FAR_OUTSIDE));
    }

    @Test
    void acceptsACoordinateInsideAForeignSpan() {
        assertDoesNotThrow(() -> {
            SeamCommandErrors.requireInsideWorld(FRAMED, Direction.Axis.X, FAR_OUTSIDE);
            SeamCommandErrors.requireInsideWorld(FRAMED, Direction.Axis.Z, FAR_OUTSIDE);
        });

        assertThrows(CommandSyntaxException.class,
                () -> SeamCommandErrors.requireInsideWorld(TORUS, Direction.Axis.X, FAR_OUTSIDE));
        assertThrows(CommandSyntaxException.class,
                () -> SeamCommandErrors.requireInsideWorld(TORUS, Direction.Axis.Z, FAR_OUTSIDE));
    }

    @Test
    void refusesACoordinateOutsideTheWorldAndEveryForeignSpan() {
        CommandSyntaxException refusal = assertThrows(CommandSyntaxException.class,
                () -> SeamCommandErrors.requireInsideWorld(FRAMED, Direction.Axis.X, 600.5));

        assertArrayEquals(new Object[] {600L, MIN_BLOCK, MAX_BLOCK}, argsOf(refusal));
    }

    @Test
    void acceptsARegionInsideAForeignSpan() {
        BoundingBox plotRegion = new BoundingBox(
                PLOT_BLOCK, 0, PLOT_BLOCK, PLOT_BLOCK + WIDE_REGION, REGION_TOP, PLOT_BLOCK + WIDE_REGION);

        assertNull(SeamCommandErrors.refusalForAmbiguousRegion(FRAMED, plotRegion));
        assertDoesNotThrow(() -> SeamCommandErrors.requireUnambiguousRegion(FRAMED, plotRegion));

        assertNotNull(SeamCommandErrors.refusalForAmbiguousRegion(TORUS, plotRegion));
        assertThrows(CommandSyntaxException.class,
                () -> SeamCommandErrors.requireUnambiguousRegion(TORUS, plotRegion));
    }

    @Test
    void skipsARelativeCoordinate() {
        assertDoesNotThrow(() -> SeamCommandErrors.requireInsideWorld(
                TORUS, Direction.Axis.X, new WorldCoordinate(true, FAR_OUTSIDE)));
        assertThrows(CommandSyntaxException.class, () -> SeamCommandErrors.requireInsideWorld(
                TORUS, Direction.Axis.X, new WorldCoordinate(false, FAR_OUTSIDE)));
    }

    private static Object[] argsOf(CommandSyntaxException refusal) {
        Component message = (Component) refusal.getRawMessage();
        return ((TranslatableContents) message.getContents()).getArgs();
    }
}
