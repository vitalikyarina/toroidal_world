package com.toroidalworld.engine.fold;

import com.toroidalworld.core.WorldFold;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

public final class NearestCopy {
    public static Vec3 toward(@Nullable WorldFold fold, Vec3 anchor, Vec3 target) {
        return fold == null ? target : fold.nearestCopy(anchor, target);
    }

    public static BlockPos toward(@Nullable WorldFold fold, BlockPos anchor, BlockPos target) {
        return fold == null ? target : fold.nearestCopy(anchor, target);
    }

    public static ChunkPos toward(@Nullable WorldFold fold, ChunkPos anchor, ChunkPos target) {
        return fold == null ? target : fold.nearestCopy(anchor, target);
    }

    private NearestCopy() {
    }
}
