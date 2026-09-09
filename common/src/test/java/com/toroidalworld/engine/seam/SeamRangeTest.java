package com.toroidalworld.engine.seam;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldLoopBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

class SeamRangeTest {
    private static final WorldFold WORLD =
            WorldFolds.of(FlatShape.torus(new WorldLoopBounds(-16, 16, -16, 16)));
    private static final WorldFold NO_FOLD = null;

    private static final BlockPos EAST_BLOCK = new BlockPos(250, 70, 0);
    private static final BlockPos WEST_BLOCK = new BlockPos(-250, 64, 8);

    private static final Vec3 EAST_POINT = new Vec3(250.5, 70.0, 0.0);
    private static final Vec3 WEST_POINT = new Vec3(-250.5, 64.0, 8.0);

    @Test
    void aNullFoldGivesThePlainDistance() {
        assertEquals(EAST_BLOCK.distSqr(WEST_BLOCK), SeamRange.sqr(NO_FOLD, EAST_BLOCK, WEST_BLOCK));
        assertEquals(EAST_POINT.distanceToSqr(WEST_POINT), SeamRange.sqr(NO_FOLD, EAST_POINT, WEST_POINT));
    }

    @Test
    void anUnwrappedFoldGivesThePlainDistance() {
        assertEquals(EAST_BLOCK.distSqr(WEST_BLOCK), SeamRange.sqr(WorldFolds.NOOP, EAST_BLOCK, WEST_BLOCK));
        assertEquals(EAST_POINT.distanceToSqr(WEST_POINT), SeamRange.sqr(WorldFolds.NOOP, EAST_POINT, WEST_POINT));
    }

    @Test
    void aPairStraddlingTheSeamMeasuresTheShortWay() {
        assertEquals(12.0 * 12.0 + 6.0 * 6.0 + 8.0 * 8.0, SeamRange.sqr(WORLD, EAST_BLOCK, WEST_BLOCK));
        assertEquals(11.0 * 11.0 + 6.0 * 6.0 + 8.0 * 8.0, SeamRange.sqr(WORLD, EAST_POINT, WEST_POINT));
    }

    @Test
    void theShortWayIsTheSameFromEitherSide() {
        assertEquals(SeamRange.sqr(WORLD, EAST_BLOCK, WEST_BLOCK), SeamRange.sqr(WORLD, WEST_BLOCK, EAST_BLOCK));
        assertEquals(SeamRange.sqr(WORLD, EAST_POINT, WEST_POINT), SeamRange.sqr(WORLD, WEST_POINT, EAST_POINT));
    }
}
