package com.toroidalworld.core;

import java.util.Optional;

import net.minecraft.world.level.Level;

public interface ForeignFrameSource {
    Optional<ForeignFrame> frameOf(Level level);
}
