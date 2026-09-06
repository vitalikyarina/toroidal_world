package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface CarriageEntityFrame {
    @Nullable
    Level toroidal$carriageLevel();

    @Nullable
    ResourceKey<Level> toroidal$carriageDimension();

    void toroidal$bindCarriageDimension(ResourceKey<Level> dimension);
}
