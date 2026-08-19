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

// One statement, for the places where Create hands the client a coordinate the client cannot use as it stands.
//
// Track graph state is canonical on both sides and has to be: the graph is a map on node keys, the client's copy of it
// arrives from the server under those keys, and a rail end at the seam is one physical place however the walk reached
// it. The client's world is the opposite by design — it is told the world is infinite and renders straight through
// the seam, so every block it holds is named in one continuous frame around the player. A canonical coordinate that
// crosses from the first into the second names ground the client does not have, and reads back as air.
//
// So the crossing is translated rather than either side being changed: the copy of a canonical position lying nearest
// the player, which is the copy the client actually holds. On a server this is not asked at all — there the canonical
// frame is the world's own.
//
// The anchor is the player because the player is what decides which copies the client holds; nothing else in the
// client's world moves the frame.
public final class CreateClientFrame {
    public static BlockPos nearestCopy(@Nullable BlockGetter world, BlockPos canonical) {
        Level level = toroidalClientLevel(world);
        if (level == null) {
            return canonical;
        }

        BlockPos anchor = viewer();
        return anchor == null ? canonical : CreateTrackFold.nearestCopy(level, anchor, canonical);
    }

    // The same question asked of a whole set at once, handing the set itself back when nothing in it moved — the walk
    // that asks this runs per step and inland has nothing to fold.
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

    // The same question for a point of geometry rather than a block, with the reference handed in: an overlay folds
    // against whatever it is about to be measured or drawn relative to, which is the camera.
    public static Vec3 nearestCopy(@Nullable Vec3 anchor, Vec3 target) {
        Level level = Minecraft.getInstance().level;
        if (level == null || anchor == null) {
            return target;
        }

        return CreateTrackFold.nearestCopy(level, anchor, target);
    }

    // And for a whole box, which is what a graph's own bounds are before anything asks whether the viewer is near it.
    public static AABB foldBoxToward(@Nullable Vec3 anchor, AABB box) {
        Level level = Minecraft.getInstance().level;
        if (level == null || anchor == null) {
            return box;
        }

        WorldLoopTransformer transformer = CreateTrackFold.transformerOf(level, null);
        return transformer == null ? box : transformer.foldBoxToward(anchor, box);
    }

    // The eye the client is looking through, for a fold whose reference is not among the call's own arguments.
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
