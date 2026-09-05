package com.toroidalworld.compat.distanthorizons;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.toroidalworld.api.TestShapes;
import com.toroidalworld.api.ToroidalShape;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.Direction;

class DhProbesTest {
    private static final int WIDTH_CHUNKS = 64;
    private static final int WIDTH_BLOCKS = WIDTH_CHUNKS * 16;

    private static ToroidalShape cylinder() {
        AxisBounds.Looped looped = new AxisBounds.Looped(0, WIDTH_CHUNKS);
        return TestShapes.of(WorldFolds.of(
                FlatShape.latticeTorus(new WorldLoopBounds(looped, AxisBounds.Unbounded.INSTANCE), FlatShape.NO_SKEW)));
    }

    @Test
    void theKeyFoldLineSurvivesACylinder() {
        assertDoesNotThrow(() -> DhProbes.keyFold(cylinder(), true));
    }

    @Test
    void theLoopingAxisNamesItsWidthAndTheOtherSaysNone() {
        ToroidalShape shape = cylinder();
        assertEquals(String.valueOf(WIDTH_BLOCKS), DhProbes.widthValue(shape, Direction.Axis.X));
        assertEquals("none", DhProbes.widthValue(shape, Direction.Axis.Z));
    }
}
