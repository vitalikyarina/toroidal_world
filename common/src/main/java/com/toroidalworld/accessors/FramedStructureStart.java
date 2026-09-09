package com.toroidalworld.accessors;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.DeckTransformation;
import com.toroidalworld.core.WorldFold;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public interface FramedStructureStart {
    @Nullable StructureStart toroidal$framedBy(WorldGenLevel level, DeckTransformation move);

    default @Nullable StructureStart toroidal$framedToward(WorldGenLevel level, WorldFold fold, ChunkPos center) {
        StructureStart self = (StructureStart) (Object) this;
        if (!self.isValid()) {
            return self;
        }

        ChunkPos startPos = self.getChunkPos();
        ChunkPos nearest = fold.nearestCopy(center, startPos);
        return toroidal$framedBy(level, fold.deckTransformation(startPos, nearest));
    }
}
