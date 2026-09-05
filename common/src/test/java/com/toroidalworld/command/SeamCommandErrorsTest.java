package com.toroidalworld.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.shape.FlatShape;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

class SeamCommandErrorsTest {
    private static final int MIN_BLOCK = -512;
    private static final int MAX_BLOCK = 511;
    private static final double FAR_OUTSIDE = 20481032.0;

    private static final WorldFold TORUS = WorldFolds.of(FlatShape.torus(new WorldLoopBounds(-32, 32, -32, 32)));

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
