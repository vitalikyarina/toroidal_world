package com.toroidalworld.compat.create.client;

import java.util.Collection;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.client.ClientFrame;
import com.toroidalworld.compat.create.CreateSeamFold;
import com.toroidalworld.core.FoldedBoxQuery;
import com.toroidalworld.core.FoldedCopies;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.net.TagPositions;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CreateClientFrame {
    public static final TagPositions.Seat VIEWER_SEAT = new TagPositions.Seat() {
        @Override
        public BlockPos seat(BlockPos stored) {
            return inViewerFrame(stored);
        }

        @Override
        public Vec3 seat(Vec3 stored) {
            return ClientFrame.nearestToPlayer(stored);
        }
    };

    public static BlockPos nearestCopy(@Nullable BlockGetter world, BlockPos canonical) {
        return isClientLevel(world) ? inViewerFrame(canonical) : canonical;
    }

    public static Collection<BlockPos> nearestCopies(@Nullable BlockGetter world, Collection<BlockPos> canonical) {
        return isClientLevel(world) ? FoldedCopies.of(canonical, CreateClientFrame::inViewerFrame) : canonical;
    }

    public static BlockPos inViewerFrame(BlockPos canonical) {
        return ClientFrame.nearestToPlayer(canonical);
    }

    public static @Nullable BlockPos heldInViewerFrame(BlockPos canonical) {
        return ClientFrame.heldCopy(canonical);
    }

    public static BlockPos nearestCopy(@Nullable BlockPos anchor, BlockPos target) {
        return ClientFrame.nearestCopy(anchor, target);
    }

    public static Vec3 nearestCopy(@Nullable Vec3 anchor, Vec3 target) {
        return ClientFrame.nearestCopy(anchor, target);
    }

    public static Vec3 inFrameOf(@Nullable Vec3 anchor, Vec3 point) {
        WorldFold fold = ClientFrame.fold();
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (fold == null || anchor == null || camera == null) {
            return point;
        }

        return CreateSeamFold.inFrameOf(fold, camera.getEyePosition(), anchor, point);
    }

    public static AABB foldBoxToward(@Nullable Vec3 anchor, AABB box) {
        return anchor == null ? box : FoldedBoxQuery.toward(ClientFrame.fold(), anchor, box);
    }

    public static boolean isClientLevel(@Nullable BlockGetter world) {
        return world != null && world == Minecraft.getInstance().level;
    }

    private CreateClientFrame() {
    }
}
