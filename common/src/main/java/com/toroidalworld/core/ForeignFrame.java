package com.toroidalworld.core;

import net.minecraft.core.Direction;

public record ForeignFrame(ForeignSpan chunksX, ForeignSpan chunksZ) {
    public ForeignSpan chunks(Direction.Axis axis) {
        return switch (axis) {
            case X -> this.chunksX;
            case Z -> this.chunksZ;
            case Y -> throw new IllegalArgumentException("A foreign frame carries no Y axis");
        };
    }

    public ForeignSpan blocks(Direction.Axis axis) {
        return chunks(axis).scaled(CoordinateConstants.CHUNK_WIDTH);
    }
}
