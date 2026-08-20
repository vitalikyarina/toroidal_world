package com.toroidalworld.compat.create.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.compat.create.CreateTrackFold;
import com.toroidalworld.core.WorldLoopTransformer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CreateClientFrame {
    public static BlockPos nearestCopy(@Nullable BlockGetter world, BlockPos canonical) {
        Level level = toroidalClientLevel(world);
        if (level == null) {
            return canonical;
        }

        BlockPos anchor = viewer();
        return anchor == null ? canonical : CreateTrackFold.nearestCopy(level, anchor, canonical);
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

        List<BlockPos> moved = null;
        for (BlockPos position : canonical) {
            BlockPos nearest = CreateTrackFold.nearestCopy(level, anchor, position);
            if (nearest != position && moved == null) {
                moved = new ArrayList<>(canonical.size());
                for (BlockPos earlier : canonical) {
                    if (earlier == position) {
                        break;
                    }

                    moved.add(earlier);
                }
            }

            if (moved != null) {
                moved.add(nearest);
            }
        }

        return moved == null ? canonical : moved;
    }

    public static Vec3 nearestCopy(@Nullable Vec3 anchor, Vec3 target) {
        Level level = Minecraft.getInstance().level;
        if (level == null || anchor == null) {
            return target;
        }

        return CreateTrackFold.nearestCopy(level, anchor, target);
    }

    public static AABB foldBoxToward(@Nullable Vec3 anchor, AABB box) {
        Level level = Minecraft.getInstance().level;
        if (level == null || anchor == null) {
            return box;
        }

        WorldLoopTransformer transformer = CreateTrackFold.transformerOf(level, null);
        return transformer == null ? box : transformer.foldBoxToward(anchor, box);
    }

    public static @Nullable Vec3 camera() {
        Entity cameraEntity = Minecraft.getInstance().cameraEntity;
        return cameraEntity == null ? null : cameraEntity.getEyePosition();
    }

    private static @Nullable Level toroidalClientLevel(@Nullable BlockGetter world) {
        return world instanceof Level level && level.isClientSide ? level : null;
    }

    private static @Nullable BlockPos viewer() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? null : player.blockPosition();
    }

    private CreateClientFrame() {
    }
}
