package com.toroidalworld.compat.aeronautics;

import org.joml.Vector3d;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity;
import dev.simulated_team.simulated.util.SimMovementContext;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class MagnetSeamFrame {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double HALF = 0.5;

    public static Vector3d midpoint(DockingConnectorBlockEntity dock1, DockingConnectorBlockEntity dock2, Vector3d average) {
        WorldFold fold = foldOf(dock1.getLevel());
        if (fold == null) {
            return average;
        }

        Vec3 tip2 = projectedTip(dock2);
        Vec3 tip1 = fold.nearestCopy(tip2, projectedTip(dock1));
        Vec3 midpoint = fold.fold(tip2.add(tip1).scale(HALF));
        if (midpoint.x == average.x && midpoint.z == average.z) {
            return average;
        }

        LOGGER.info("[aeronautics-compat] magnet_dock_midpoint raw_x_blocks={} raw_z_blocks={} "
                        + "folded_x_blocks={} folded_z_blocks={}",
                average.x, average.z, midpoint.x, midpoint.z);
        return average.set(midpoint.x, midpoint.y, midpoint.z);
    }

    public static Object seatNearbyMagnet(BlockEntity self, Object nearby) {
        Level level = self.getLevel();
        WorldFold fold = foldOf(level);
        if (fold == null || !(nearby instanceof Vector3d position)) {
            return nearby;
        }

        Vec3 anchor = SimMovementContext.getMovementContext(level, Vec3.atCenterOf(self.getBlockPos())).globalPosition();
        Vec3 raw = new Vec3(position.x, position.y, position.z);
        Vec3 seated = fold.nearestCopy(anchor, raw);
        if (seated.x == raw.x && seated.z == raw.z) {
            return nearby;
        }

        return new Vector3d(seated.x, seated.y, seated.z);
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
