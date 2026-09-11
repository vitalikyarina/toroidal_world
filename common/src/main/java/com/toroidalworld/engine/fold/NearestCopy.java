package com.toroidalworld.engine.fold;

import com.toroidalworld.core.DeckTransformation;
import com.toroidalworld.core.WorldFold;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

    public static double toward(@Nullable WorldFold fold, Direction.Axis axis, double anchor, double coord) {
        return fold == null ? coord : fold.blockDomain(axis).unwrapAround(anchor, coord);
    }

    // mc/1.21: calls the transformation forms, unused on main.
    public static DeckTransformation transformationToward(@Nullable WorldFold fold, Vec3 ref, Vec3 target) {
        return fold == null ? DeckTransformation.IDENTITY : fold.nearestCopyTransformation(ref, target);
    }

    public static DeckTransformation transformationToward(@Nullable WorldFold fold, BlockPos ref, BlockPos target) {
        return fold == null ? DeckTransformation.IDENTITY : fold.nearestCopyTransformation(ref, target);
    }

    private NearestCopy() {
    }
}
