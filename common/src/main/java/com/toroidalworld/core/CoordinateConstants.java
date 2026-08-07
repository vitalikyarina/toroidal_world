package com.toroidalworld.core;

import net.minecraft.core.SectionPos;

public class CoordinateConstants {
    public static final int CHUNK_WIDTH = SectionPos.SECTION_SIZE;

    // Chunks held back between the largest allowed view distance and half the world width, so the loaded square never
    // reaches its own far side across the seam.
    public static final int VIEW_DISTANCE_MARGIN = 3;
}
