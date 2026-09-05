package com.toroidalworld.noise;

import com.toroidalworld.core.WorldFold;

import net.minecraft.core.Direction;

enum LapFloor {
    FOUR_CELLS(4L),
    HELD(PeriodicNoiseSampler.HELD_PERIOD);

    final long period;

    LapFloor(long period) {
        this.period = period;
    }

    static LapFloor of(WorldFold transformer) {
        boolean bothLoop = transformer.blockDomain(Direction.Axis.X).loops()
                && transformer.blockDomain(Direction.Axis.Z).loops();
        return bothLoop ? FOUR_CELLS : HELD;
    }
}
