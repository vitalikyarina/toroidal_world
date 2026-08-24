package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.level.Level;

public interface TrackNodeKeyFold {
    // Folding a location Create has already filed as a TrackGraph key strands that entry, so this runs only on
    // a freshly built one.
    void toroidal$foldNodeKey(@Nullable Level level);
}
