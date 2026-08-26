package com.toroidalworld.compat;

public final class FullscreenZoomFloor {
    public static final int MIN_WORLD_PIXELS = 64;

    private static final int JOURNEYMAP_REGION_BLOCKS = 512;

    public static int journeyMapZoom(int widthBlocks) {
        return Math.ceilDiv(MIN_WORLD_PIXELS * JOURNEYMAP_REGION_BLOCKS, widthBlocks);
    }

    public static double xaeroScale(int widthBlocks, double scaleMultiplier) {
        return MIN_WORLD_PIXELS / (widthBlocks * scaleMultiplier);
    }

    private FullscreenZoomFloor() {
    }
}
