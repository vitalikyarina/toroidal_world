package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.ForeignFrames;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.CurrentClientLevel;
import com.toroidalworld.storage.CurrentServer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class CreateSeamFold {
    private static final ServerFoldMemo SERVER_FOLDS = new ServerFoldMemo();

    public static @Nullable WorldFold transformerOf(@Nullable Level level,
            @Nullable ResourceKey<Level> dimension) {
        if (level != null) {
            return WorldLoopAttachments.wrappedTransformerOfReader(level);
        }

        if (dimension == null) {
            return null;
        }

        MinecraftServer server = CurrentServer.get();
        if (server == null) {
            return clientTransformerOf(dimension);
        }

        return SERVER_FOLDS.of(server, dimension, () -> serverTransformerOf(server, dimension));
    }

    private static @Nullable WorldFold serverTransformerOf(MinecraftServer server, ResourceKey<Level> dimension) {
        ServerLevel serverLevel = server.getLevel(dimension);
        return serverLevel == null ? null : WorldLoopAttachments.wrappedTransformerOf(serverLevel);
    }

    private static @Nullable WorldFold clientTransformerOf(ResourceKey<Level> dimension) {
        Level clientLevel = CurrentClientLevel.get();
        if (clientLevel == null || !clientLevel.dimension().equals(dimension)) {
            return null;
        }

        return WorldLoopAttachments.wrappedTransformerOfReader(clientLevel);
    }

    public static BlockPos foldDelta(@Nullable Level level, BlockPos anchor, BlockPos target, BlockPos rawDelta) {
        if (level == null) {
            return rawDelta;
        }

        return delta(WorldLoopAttachments.wrappedTransformerOfReader(level), anchor, target, rawDelta);
    }

    public static BlockPos farEndDelta(@Nullable Level level, BlockPos anchor, BlockPos delta, BlockPos rawFarEnd) {
        if (level == null) {
            return rawFarEnd;
        }

        return farEndDelta(WorldLoopAttachments.wrappedTransformerOfReader(level), anchor, delta, rawFarEnd);
    }

    public static BlockPos worldSeat(@Nullable Level level, BlockPos stored) {
        if (level == null) {
            return stored;
        }

        Vec3 seated = ForeignFrames.seatInWorld(level, Vec3.atCenterOf(stored));
        return BlockPos.containing(seated);
    }

    public static BlockPos nearestCopy(@Nullable Level level, BlockPos anchor, BlockPos target) {
        return nearest(transformerOf(level, null), anchor, target);
    }

    public static Vec3 nearestCopy(@Nullable Level level, Vec3 anchor, Vec3 target) {
        return nearestCopy(transformerOf(level, null), anchor, target);
    }

    public static Vec3 nearestCopy(@Nullable ResourceKey<Level> dimension, Vec3 anchor, Vec3 target) {
        return nearestCopy(transformerOf(null, dimension), anchor, target);
    }

    public static Vec3 nearestCopy(@Nullable Level level, @Nullable ResourceKey<Level> dimension, Vec3 anchor,
            Vec3 target) {
        return nearestCopy(transformerOf(level, dimension), anchor, target);
    }

    private static Vec3 nearestCopy(@Nullable WorldFold transformer, Vec3 anchor, Vec3 target) {
        return transformer == null ? target : transformer.nearestCopy(anchor, target);
    }

    public static BlockPos nearestCopy(@Nullable WorldFold transformer, BlockPos anchor, BlockPos target) {
        return nearest(transformer, anchor, target);
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

    public static Vec3 inFrameOf(WorldFold transformer, Vec3 viewer, Vec3 anchor, Vec3 point) {
        WorldFold.Folded<Vec3> seatedAnchor = transformer.nearestCopyOriented(viewer, anchor);
        if (seatedAnchor.isIdentity() && seatedAnchor.value() == anchor) {
            return point;
        }

        return seatedAnchor.value().add(seatedAnchor.orientation().applyToDelta(point.subtract(anchor)));
    }

    public static Vec3 inFrameOf(@Nullable Level level, Vec3 viewer, Vec3 anchor, Vec3 point) {
        WorldFold transformer = transformerOf(level, null);
        return transformer == null ? point : inFrameOf(transformer, viewer, anchor, point);
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

    public static BlockPos canonical(@Nullable ResourceKey<Level> dimension, BlockPos position) {
        return canonical(transformerOf(null, dimension), position);
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

    public static Vec3 canonical(@Nullable ServerLevel level, Vec3 position) {
        if (level == null) {
            return position;
        }

        return canonical(WorldLoopAttachments.wrappedTransformerOf(level), position);
    }

    static Vec3 canonical(@Nullable WorldFold transformer, Vec3 position) {
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

    static BlockPos farEndDelta(@Nullable WorldFold transformer, BlockPos anchor, BlockPos delta,
            BlockPos rawFarEnd) {
        if (transformer == null) {
            return rawFarEnd;
        }

        BlockPos farEnd = canonical(transformer, anchor.offset(delta));
        BlockPos nearestAnchor = nearest(transformer, farEnd, anchor);
        if (nearestAnchor.equals(anchor)) {
            return rawFarEnd;
        }

        return nearestAnchor.subtract(farEnd);
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
