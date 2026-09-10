package com.toroidalworld.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.toroidalworld.api.v1.TestShapes;
import com.toroidalworld.api.v1.ToroidalShape;
import com.toroidalworld.core.CoordinateConstants;
import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldLoopBounds;
import com.toroidalworld.core.WorldLoopBounds.AxisBounds;

class FullscreenZoomFloorTest {
    @Test
    void journeyMapKeepsTheWorldAtLeast64PixelsWide() {
        assertEquals(64, FullscreenZoomFloor.journeyMapZoom(512), "a 512-block world: 64 * 512 / 512 = 64 px per region");
        assertEquals(32, FullscreenZoomFloor.journeyMapZoom(1024), "a 1024-block world: 64 * 512 / 1024 = 32");
        assertEquals(256, FullscreenZoomFloor.journeyMapZoom(128), "a 128-block world: 64 * 512 / 128 = 256");
        assertEquals(8, FullscreenZoomFloor.journeyMapZoom(4096), "a 4096-block world: 64 * 512 / 4096 = 8");
        assertEquals(110, FullscreenZoomFloor.journeyMapZoom(300), "a 300-block world: ceil(32768 / 300) = 110");
    }

    @Test
    void xaeroKeepsTheWorldAtLeast64PixelsWide() {
        assertEquals(0.125, FullscreenZoomFloor.xaeroScale(512, 1.0), 1e-12, "a 512-block world at multiplier 1: 64 / 512");
        assertEquals(64.0 / (512 * 1.2676), FullscreenZoomFloor.xaeroScale(512, 1.2676), 1e-12,
                "a 512-block world on a 1369-px screen: 64 / (512 * 1369 / 1080)");
        assertEquals(0.5, FullscreenZoomFloor.xaeroScale(128, 1.0), 1e-12, "a 128-block world at multiplier 1: 64 / 128");
    }

    @Test
    void journeyMapTakesTheNarrowestLoopedAxis() {
        assertEquals(64, FullscreenZoomFloor.journeyMapZoom(torus(1024, 512)), "the 512-block axis sets the floor");
        assertEquals(64, FullscreenZoomFloor.journeyMapZoom(cylinder(512)), "an unbounded axis asks for no floor");
    }

    @Test
    void xaeroTakesTheNarrowestLoopedAxis() {
        assertEquals(0.125, FullscreenZoomFloor.xaeroScale(torus(1024, 512), 1.0), 1e-12,
                "the 512-block axis sets the floor");
        assertEquals(0.125, FullscreenZoomFloor.xaeroScale(cylinder(512), 1.0), 1e-12,
                "an unbounded axis asks for no floor");
    }

    private static ToroidalShape torus(int widthBlocksX, int widthBlocksZ) {
        return shape(looped(widthBlocksX), looped(widthBlocksZ));
    }

    private static ToroidalShape cylinder(int widthBlocksZ) {
        return shape(AxisBounds.Unbounded.INSTANCE, looped(widthBlocksZ));
    }

    private static ToroidalShape shape(AxisBounds x, AxisBounds z) {
        return TestShapes.of(WorldFolds.of(FlatShape.torus(new WorldLoopBounds(x, z))));
    }

    private static AxisBounds looped(int widthBlocks) {
        return new AxisBounds.Looped(0, widthBlocks / CoordinateConstants.CHUNK_WIDTH);
    }
}
