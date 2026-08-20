package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface BezierCurveFold {
    void toroidal$foldCurve(@Nullable Level level, @Nullable ResourceKey<Level> dimension);
}
