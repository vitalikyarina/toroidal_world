package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CreateSeamFold {
    public static BlockPos foldDelta(@Nullable Level level, BlockPos anchor, BlockPos target, BlockPos rawDelta) {
        if (level == null) {
            return rawDelta;
        }

        return delta(WorldLoopAttachments.wrappedTransformerOf(level), anchor, target, rawDelta);
    }

    public static BlockPos foldClientDelta(@Nullable Level level, BlockPos anchor, BlockPos target, BlockPos rawDelta) {
        if (level == null) {
            return rawDelta;
        }

        return delta(WorldLoopAttachments.wrappedClientBoundsTransformerOf(level), anchor, target, rawDelta);
    }

    public static BlockPos foldPosition(@Nullable Level level, BlockPos anchor, BlockPos target) {
        if (level == null) {
            return target;
        }

        return nearest(WorldLoopAttachments.wrappedTransformerOf(level), anchor, target);
    }

    public static BlockPos foldClientPosition(@Nullable Level level, BlockPos anchor, BlockPos target) {
        if (level == null) {
            return target;
        }

        return nearest(WorldLoopAttachments.wrappedClientBoundsTransformerOf(level), anchor, target);
    }

    public static BlockPos foldPositionToBox(@Nullable Level level, BoundingBox box, BlockPos position) {
        if (level == null) {
            return position;
        }

        return nearest(WorldLoopAttachments.wrappedTransformerOf(level), box.getCenter(), position);
    }

    public static Vec3 foldPointToBox(@Nullable Level level, AABB box, Vec3 point) {
        if (level == null) {
            return point;
        }

        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        return transformer == null ? point : transformer.vectors.nearestCopy(box.getCenter(), point);
    }

    public static BlockPos canonical(@Nullable Level level, BlockPos position) {
        if (level == null) {
            return position;
        }

        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        return transformer == null ? position : transformer.blocks.wrap(position);
    }

    private static BlockPos delta(@Nullable WorldLoopTransformer transformer, BlockPos anchor, BlockPos target,
            BlockPos rawDelta) {
        BlockPos nearest = nearest(transformer, anchor, target);
        if (nearest.equals(target)) {
            return rawDelta;
        }

        return nearest.subtract(anchor);
    }

    private static BlockPos nearest(@Nullable WorldLoopTransformer transformer, BlockPos anchor, BlockPos target) {
        if (transformer == null) {
            return target;
        }

        return transformer.blocks.nearestCopy(anchor, target);
    }

    private CreateSeamFold() {
    }
}
