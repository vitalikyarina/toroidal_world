package com.toroidalworld.compat;

import java.util.List;

import com.toroidalworld.api.ToroidalShape;

import net.minecraft.core.Direction;

public record AxisCopies(boolean loops, int min, int width) {
    private static final List<Integer> LOOPED_LAPS = List.of(-1, 0, 1);
    private static final List<Integer> SINGLE_LAP = List.of(0);

    public static final AxisCopies UNBOUNDED = new AxisCopies(false, 0, 0);

    public static AxisCopies of(ToroidalShape shape, Direction.Axis axis) {
        return shape.loops(axis) ? looped(shape.minBlock(axis), shape.widthBlocks(axis)) : UNBOUNDED;
    }

    public static AxisCopies looped(int min, int width) {
        if (width <= 0) {
            throw new IllegalArgumentException("A looped axis needs a positive width, got " + width);
        }

        return new AxisCopies(true, min, width);
    }

    @Override
    public int min() {
        return looped().min;
    }

    public int max() {
        return looped().min + this.width;
    }

    public List<Integer> laps() {
        return this.loops ? LOOPED_LAPS : SINGLE_LAP;
    }

    public int offset(int lap) {
        return lap * this.width;
    }

    public int clipMin(int spanMin) {
        return this.loops ? Math.max(spanMin, this.min) : spanMin;
    }

    public int clipMax(int spanMax) {
        return this.loops ? Math.min(spanMax, max()) : spanMax;
    }

    private AxisCopies looped() {
        if (!this.loops) {
            throw new IllegalStateException("The axis does not loop — check loops() first");
        }

        return this;
    }
}
