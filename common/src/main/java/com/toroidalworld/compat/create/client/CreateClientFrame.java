package com.toroidalworld.compat.create.client;

import java.util.Collection;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.compat.create.CreateTrackFold;
import com.toroidalworld.core.FoldedCopies;
import com.toroidalworld.core.WorldFold;

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

        return FoldedCopies.of(canonical, position -> CreateTrackFold.nearestCopy(level, anchor, position));
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

        WorldFold transformer = CreateTrackFold.transformerOf(level, null);
        return transformer == null ? box : transformer.foldBox(anchor, box).value();
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
