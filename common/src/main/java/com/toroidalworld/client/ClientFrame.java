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

public final class ClientFrame {
    public static @Nullable Vec3 nearestToPlayer(@Nullable Vec3 target) {
        WorldFold fold = fold();
        LocalPlayer player = Minecraft.getInstance().player;
        if (fold == null || player == null || target == null) {
            return target;
        }

        return fold.nearestCopy(player.position(), target);
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
        if (level == null || player == null) {
            return canonical;
        }

        WorldFold fold = WorldLoopAttachments.wrappedClientBoundsTransformerOf(level);
        if (fold == null) {
            return canonical;
        }

        BlockPos nearest = fold.nearestCopy(player.blockPosition(), canonical);
        return holds(level, nearest) ? nearest : null;
    }

    private static @Nullable WorldFold fold() {
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
