package com.toroidalworld.client;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

// mc/1.21: calls the members unused on main.
public final class ClientFrame {
    public static @Nullable BlockPos nearestCopy(@Nullable BlockPos anchor, @Nullable BlockPos target) {
        WorldFold fold = fold();
        if (fold == null || anchor == null || target == null) {
            return target;
        }

        return fold.nearestCopy(anchor, target);
    }

    public static @Nullable Vec3 nearestCopy(@Nullable Vec3 anchor, @Nullable Vec3 target) {
        WorldFold fold = fold();
        if (fold == null || anchor == null || target == null) {
            return target;
        }

        return fold.nearestCopy(anchor, target);
    }

    public static @Nullable BlockPos nearestToPlayer(@Nullable BlockPos target) {
        LocalPlayer player = Minecraft.getInstance().player;
        return nearestCopy(player == null ? null : player.blockPosition(), target);
    }

    public static @Nullable Vec3 nearestToPlayer(@Nullable Vec3 target) {
        LocalPlayer player = Minecraft.getInstance().player;
        return nearestCopy(player == null ? null : player.position(), target);
    }

    public static @Nullable Vec3 nearestToCamera(@Nullable Vec3 target) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        return nearestCopy(camera == null ? null : camera.position(), target);
    }

    public static double nearestToCamera(Direction.Axis axis, double coord) {
        WorldFold fold = fold();
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (fold == null || camera == null) {
            return coord;
        }

        return fold.blockDomain(axis).unwrapAround(camera.position().get(axis), coord);
    }

    public static @Nullable BlockPos heldCopy(BlockPos canonical) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        WorldFold fold = fold();
        if (level == null || player == null || fold == null) {
            return canonical;
        }

        BlockPos nearest = fold.nearestCopy(player.blockPosition(), canonical);
        return holds(level, nearest) ? nearest : null;
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
