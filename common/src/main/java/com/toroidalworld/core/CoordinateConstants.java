package com.toroidalworld.core;

import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;

public final class CoordinateConstants {
    public static final List<Direction.Axis> HORIZONTAL_AXES = List.of(Direction.Axis.X, Direction.Axis.Z);

    public static final int CHUNK_WIDTH = SectionPos.SECTION_SIZE;

    public static final int VIEW_DISTANCE_MARGIN = 3;

    private CoordinateConstants() {
    }
}
