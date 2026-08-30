package com.toroidalworld.core;

import java.util.Optional;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface ForeignFrameSource {
    Optional<ForeignFrame> frameOf(Level level);

    default Vec3 seatInWorld(Level level, Vec3 stored) {
        return stored;
    }
}
