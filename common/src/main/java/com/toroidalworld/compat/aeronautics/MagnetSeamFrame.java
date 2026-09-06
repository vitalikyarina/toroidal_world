package com.toroidalworld.compat.aeronautics;

import org.joml.Vector3d;
import org.jspecify.annotations.Nullable;
import com.toroidalworld.core.JomlVectors;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity;
import dev.simulated_team.simulated.util.SimMovementContext;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class MagnetSeamFrame {
    private static final double HALF = 0.5;

    public static Vector3d midpoint(DockingConnectorBlockEntity dock1, DockingConnectorBlockEntity dock2, Vector3d average) {
        WorldFold fold = foldOf(dock1.getLevel());
        if (fold == null) {
            return average;
        }

        Vec3 tip2 = projectedTip(dock2);
        return midpoint(fold, projectedTip(dock1), tip2, average);
    }

    static Vector3d midpoint(@Nullable WorldFold fold, Vec3 tip1, Vec3 tip2, Vector3d average) {
        if (fold == null) {
            return average;
        }

        Vec3 seated = fold.nearestCopy(tip2, tip1);
        Vec3 midpoint = fold.fold(tip2.add(seated).scale(HALF));
        if (midpoint.x == average.x && midpoint.z == average.z) {
            return average;
        }

        return JomlVectors.write(midpoint, average);
    }

    public static Object seatNearbyMagnet(BlockEntity self, Object nearby) {
        Level level = self.getLevel();
        WorldFold fold = foldOf(level);
        if (fold == null || !(nearby instanceof Vector3d)) {
            return nearby;
        }

        Vec3 anchor = SimMovementContext.getMovementContext(level, Vec3.atCenterOf(self.getBlockPos())).globalPosition();
        return seatNearbyMagnet(fold, anchor, nearby);
    }

    static Object seatNearbyMagnet(@Nullable WorldFold fold, Vec3 anchor, Object nearby) {
        if (fold == null || !(nearby instanceof Vector3d position)) {
            return nearby;
        }

        return JomlVectors.seat(fold, anchor, position);
    }

    private static Vec3 projectedTip(DockingConnectorBlockEntity dock) {
        Vec3 tip = dock.getTipPosition();
        SubLevel shell = dock.getLatestSubLevel();
        return shell == null ? tip : shell.logicalPose().transformPosition(tip);
    }

    private static @Nullable WorldFold foldOf(@Nullable Level level) {
        return level == null ? null : WorldLoopAttachments.wrappedTransformerOfReader(level);
    }

    private MagnetSeamFrame() {
    }
}
