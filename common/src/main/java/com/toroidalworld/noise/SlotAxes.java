package com.toroidalworld.noise;

public record SlotAxes(SlotAxis x, SlotAxis y, SlotAxis z) {
    public static final SlotAxes DEFAULT = new SlotAxes(SlotAxis.X, SlotAxis.NONE, SlotAxis.Z);
}
