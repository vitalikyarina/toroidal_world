package com.toroidalworld.client.engine;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldLoopAttachments;
import com.toroidalworld.engine.fold.NearestCopy;

import java.util.function.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

// mc/1.21: calls the members unused on main.
public final class ClientFrame {
    public static @Nullable BlockPos nearestCopy(@Nullable BlockPos anchor, @Nullable BlockPos target) {
        if (anchor == null || target == null) {
            return target;
        }

        return NearestCopy.toward(fold(), anchor, target);
    }

    public static @Nullable Vec3 nearestCopy(@Nullable Vec3 anchor, @Nullable Vec3 target) {
        if (anchor == null || target == null) {
            return target;
        }

        return NearestCopy.toward(fold(), anchor, target);
    }

    public static @Nullable ChunkPos nearestCopy(@Nullable ChunkPos anchor, @Nullable ChunkPos target) {
        if (anchor == null || target == null) {
            return target;
        }

        return NearestCopy.toward(fold(), anchor, target);
    }

    public static @Nullable BlockPos nearestToPlayer(@Nullable BlockPos target) {
        LocalPlayer player = Minecraft.getInstance().player;
        return nearestCopy(player == null ? null : player.blockPosition(), target);
    }

    public static @Nullable Vec3 nearestToPlayer(@Nullable Vec3 target) {
        LocalPlayer player = Minecraft.getInstance().player;
        return nearestCopy(player == null ? null : player.position(), target);
    }

    public static @Nullable ChunkPos nearestToPlayer(@Nullable ChunkPos target) {
        LocalPlayer player = Minecraft.getInstance().player;
        return nearestCopy(player == null ? null : player.chunkPosition(), target);
    }

    public static @Nullable Vec3 nearestToCamera(@Nullable Vec3 target) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        return nearestCopy(camera == null ? null : camera.position(), target);
    }

    public static double nearestToCamera(Direction.Axis axis, double coord) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera == null) {
            return coord;
        }

        return NearestCopy.toward(fold(), axis, camera.position().get(axis), coord);
    }

    public static @Nullable BlockPos heldCopy(BlockPos canonical) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null) {
            return canonical;
        }

        return heldCopy(fold(), player == null ? null : player.blockPosition(), canonical, pos -> holds(level, pos));
    }

    public static @Nullable ChunkPos heldCopy(ChunkPos canonical, Predicate<ChunkPos> holds) {
        LocalPlayer player = Minecraft.getInstance().player;
        return heldCopy(fold(), player == null ? null : player.chunkPosition(), canonical, holds);
    }

    public static @Nullable BlockPos heldCopy(@Nullable WorldFold fold, @Nullable BlockPos anchor, BlockPos canonical,
            Predicate<BlockPos> holds) {
        if (fold == null || anchor == null) {
            return canonical;
        }

        BlockPos nearest = fold.nearestCopy(anchor, canonical);
        return holds.test(nearest) ? nearest : null;
    }

    public static @Nullable ChunkPos heldCopy(@Nullable WorldFold fold, @Nullable ChunkPos anchor, ChunkPos canonical,
            Predicate<ChunkPos> holds) {
        if (fold == null || anchor == null) {
            return canonical;
        }

        ChunkPos nearest = fold.nearestCopy(anchor, canonical);
        return holds.test(nearest) ? nearest : null;
    }

    public static boolean isClientLevel(@Nullable BlockGetter world) {
        return world != null && world == Minecraft.getInstance().level;
    }

    public static @Nullable WorldFold fold() {
        return WorldLoopAttachments.wrappedClientBoundsTransformerOf(Minecraft.getInstance().level);
    }

    private static boolean holds(ClientLevel level, BlockPos pos) {
        return level.getChunkSource().hasChunk(
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ()));
    }

    private ClientFrame() {
    }
}
