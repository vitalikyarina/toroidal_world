package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.ForeignFrames;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class CreateSeamFold {
    public static BlockPos foldDelta(@Nullable Level level, BlockPos anchor, BlockPos target, BlockPos rawDelta) {
        if (level == null) {
            return rawDelta;
        }

        return delta(WorldLoopAttachments.wrappedTransformerOfReader(level), anchor, target, rawDelta);
    }

    public static BlockPos worldSeat(@Nullable Level level, BlockPos stored) {
        if (level == null) {
            return stored;
        }

        Vec3 seated = ForeignFrames.seatInWorld(level, Vec3.atCenterOf(stored));
        return BlockPos.containing(seated);
    }

    public static BlockPos foldPosition(@Nullable Level level, BlockPos anchor, BlockPos target) {
        if (level == null) {
            return target;
        }

        return nearest(WorldLoopAttachments.wrappedTransformerOfReader(level), anchor, target);
    }

    public static BlockPos foldPositionToBox(@Nullable Level level, BoundingBox box, BlockPos position) {
        if (level == null) {
            return position;
        }

        return foldPositionToBox(WorldLoopAttachments.wrappedTransformerOfReader(level), box, position);
    }

    static BlockPos foldPositionToBox(@Nullable WorldFold transformer, BoundingBox box, BlockPos position) {
        return nearest(transformer, box.getCenter(), position);
    }

    public static Vec3 foldPointToBox(@Nullable Level level, AABB box, Vec3 point) {
        if (level == null) {
            return point;
        }

        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOfReader(level);
        return transformer == null ? point : transformer.nearestCopy(box.getCenter(), point);
    }

    public static Vec3 foldPoint(@Nullable Level level, Vec3 anchor, Vec3 target) {
        if (level == null) {
            return target;
        }

        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOfReader(level);
        return transformer == null ? target : transformer.nearestCopy(anchor, target);
    }

    public static BlockHitResult canonical(@Nullable ServerLevel level, BlockHitResult hit) {
        if (level == null) {
            return hit;
        }

        return canonical(WorldLoopAttachments.wrappedTransformerOf(level), hit);
    }

    static BlockHitResult canonical(@Nullable WorldFold transformer, BlockHitResult hit) {
        BlockPos raw = hit.getBlockPos();
        BlockPos wrapped = canonical(transformer, raw);
        if (wrapped.equals(raw)) {
            return hit;
        }

        Vec3 offsetInBlock = hit.getLocation().subtract(Vec3.atLowerCornerOf(raw));
        Vec3 location = Vec3.atLowerCornerOf(wrapped).add(offsetInBlock);
        return hit.getType() == HitResult.Type.MISS
                ? BlockHitResult.miss(location, hit.getDirection(), wrapped)
                : new BlockHitResult(location, hit.getDirection(), wrapped, hit.isInside());
    }

    public static BlockPos canonical(@Nullable ServerLevel level, BlockPos position) {
        if (level == null) {
            return position;
        }

        return canonical(WorldLoopAttachments.wrappedTransformerOf(level), position);
    }

    static BlockPos canonical(@Nullable WorldFold transformer, BlockPos position) {
        return transformer == null ? position : transformer.fold(position);
    }

    static BlockPos delta(@Nullable WorldFold transformer, BlockPos anchor, BlockPos target,
            BlockPos rawDelta) {
        BlockPos nearest = nearest(transformer, anchor, target);
        if (nearest.equals(target)) {
            return rawDelta;
        }

        return nearest.subtract(anchor);
    }

    static BlockPos nearest(@Nullable WorldFold transformer, BlockPos anchor, BlockPos target) {
        if (transformer == null) {
            return target;
        }

        return transformer.nearestCopy(anchor, target);
    }

    private CreateSeamFold() {
    }
}
