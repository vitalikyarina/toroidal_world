package com.toroidalworld.compat.distanthorizons;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.toroidalworld.api.v1.TestShapes;
import com.toroidalworld.api.v1.ToroidalShape;
import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldLoopBounds;
import com.toroidalworld.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.Direction;

class DhProbesTest {
    private static final int WIDTH_CHUNKS = 64;
    private static final int WIDTH_BLOCKS = WIDTH_CHUNKS * 16;

    private static ToroidalShape cylinder() {
        AxisBounds.Looped looped = new AxisBounds.Looped(0, WIDTH_CHUNKS);
        return TestShapes.of(WorldFolds.of(
                FlatShape.torus(new WorldLoopBounds(looped, AxisBounds.Unbounded.INSTANCE))));
    }

    @Test
    void theKeyPeriodLineSurvivesACylinder() {
        assertDoesNotThrow(() -> DhProbes.keyPeriod(cylinder(), DhKeys.LEAF));
    }

    @Test
    void theLoopingAxisNamesItsWidthAndTheOtherSaysNone() {
        ToroidalShape shape = cylinder();
        assertEquals(String.valueOf(WIDTH_BLOCKS), DhProbes.widthValue(shape, Direction.Axis.X));
        assertEquals("none", DhProbes.widthValue(shape, Direction.Axis.Z));
    }
}
