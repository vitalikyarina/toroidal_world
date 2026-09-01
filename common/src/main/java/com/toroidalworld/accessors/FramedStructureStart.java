package com.toroidalworld.accessors;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.DeckTransformation;

import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public interface FramedStructureStart {
    @Nullable StructureStart toroidal$framedBy(WorldGenLevel level, DeckTransformation move);
}
