package com.toroidalworld.compat.create.client;

import java.util.Collection;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.compat.create.CreateSeamFold;
import com.toroidalworld.core.FoldedCopies;
import com.toroidalworld.core.WorldFold;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CreateClientFrame {
    public static BlockPos nearestCopy(@Nullable BlockGetter world, BlockPos canonical) {
        Level level = toroidalClientLevel(world);
        if (level == null) {
            return canonical;
        }

        BlockPos anchor = viewer();
        return anchor == null ? canonical : CreateSeamFold.nearestCopy(level, anchor, canonical);
    }

    public static @Nullable BlockPos heldCopy(@Nullable BlockGetter world, BlockPos canonical) {
        Level level = toroidalClientLevel(world);
        if (level == null || CreateSeamFold.transformerOf(level, null) == null) {
            return canonical;
        }

        BlockPos anchor = viewer();
        if (anchor == null) {
            return canonical;
        }

        BlockPos nearest = CreateSeamFold.nearestCopy(level, anchor, canonical);
        return holds(level, nearest) ? nearest : null;
    }

    public static Collection<BlockPos> nearestCopies(@Nullable BlockGetter world, Collection<BlockPos> canonical) {
        Level level = toroidalClientLevel(world);
        if (level == null) {
            return canonical;
        }

        BlockPos anchor = viewer();
        if (anchor == null) {
            return canonical;
        }

        return FoldedCopies.of(canonical, position -> CreateSeamFold.nearestCopy(level, anchor, position));
    }

    public static BlockPos nearestCopy(@Nullable BlockPos anchor, BlockPos target) {
        Level level = Minecraft.getInstance().level;
        if (level == null || anchor == null) {
            return target;
        }

        return CreateSeamFold.nearestCopy(level, anchor, target);
    }

    public static Vec3 nearestCopy(@Nullable Vec3 anchor, Vec3 target) {
        Level level = Minecraft.getInstance().level;
        if (level == null || anchor == null) {
            return target;
        }

        return CreateSeamFold.nearestCopy(level, anchor, target);
    }

    public static Vec3 inFrameOf(@Nullable Vec3 anchor, Vec3 point) {
        Level level = Minecraft.getInstance().level;
        Vec3 viewer = camera();
        if (level == null || anchor == null || viewer == null) {
            return point;
        }

        return CreateSeamFold.inFrameOf(level, viewer, anchor, point);
    }

    public static AABB foldBoxToward(@Nullable Vec3 anchor, AABB box) {
        Level level = Minecraft.getInstance().level;
        if (level == null || anchor == null) {
            return box;
        }

        WorldFold transformer = CreateSeamFold.transformerOf(level, null);
        return transformer == null ? box : transformer.foldBox(anchor, box).value();
    }

    public static @Nullable Vec3 camera() {
        Entity cameraEntity = Minecraft.getInstance().cameraEntity;
        return cameraEntity == null ? null : cameraEntity.getEyePosition();
    }

    public static @Nullable BlockPos viewer() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? null : player.blockPosition();
    }

    private static boolean holds(Level level, BlockPos pos) {
        return level.getChunkSource().getChunk(SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ()), ChunkStatus.FULL, false) != null;
    }

    private static @Nullable Level toroidalClientLevel(@Nullable BlockGetter world) {
        return world instanceof Level level && level.isClientSide ? level : null;
    }

    private CreateClientFrame() {
    }
}
