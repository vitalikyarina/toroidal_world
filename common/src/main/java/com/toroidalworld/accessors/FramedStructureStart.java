package com.toroidalworld.accessors;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public interface FramedStructureStart {
    @Nullable StructureStart toroidal$framedBy(WorldGenLevel level, int deltaChunkX, int deltaChunkZ);
}
