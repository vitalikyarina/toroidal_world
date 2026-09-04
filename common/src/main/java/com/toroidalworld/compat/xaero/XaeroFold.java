package com.toroidalworld.compat.xaero;

import org.slf4j.Logger;

import com.toroidalworld.api.ToroidalShape;
import com.toroidalworld.api.ToroidalWorldClientApi;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class XaeroFold {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile Object[] coordReadoutDisplays;

    private static ToroidalShape shape() {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? null : ToroidalWorldClientApi.shapeOf(level).orElse(null);
    }

    public static BlockPos foldWorldNodeSpawn(BlockPos spawn) {
        ToroidalShape shape = shape();
        if (shape == null || spawn == null) {
            return spawn;
        }

        return shape.fold(spawn);
    }

    private static double cameraCoord(Direction.Axis axis) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera == null) {
            return Double.NaN;
        }

        Vec3 position = camera.position();
        return axis == Direction.Axis.X ? position.x : position.z;
    }

    public static double nearestElementCoord(Direction.Axis axis, double coord) {
        ToroidalShape shape = shape();
        if (shape == null) {
            return coord;
        }

        double ref = cameraCoord(axis);
        if (Double.isNaN(ref)) {
            return coord;
        }

        return shape.nearestCoord(axis, ref, coord);
    }

    public static int nearestWaypointBlock(Direction.Axis axis, int coord) {
        ToroidalShape shape = shape();
        if (shape == null) {
            return coord;
        }

        double ref = cameraCoord(axis);
        if (Double.isNaN(ref)) {
            return coord;
        }

        return (int) Math.round(shape.nearestCoord(axis, ref, coord));
    }

    public static BlockPos foldInfoDisplayPos(Object infoDisplay, BlockPos playerPos) {
        ToroidalShape shape = shape();
        if (shape == null || playerPos == null) {
            return playerPos;
        }

        Object[] targets = coordReadoutDisplays();
        boolean isCoordReadout = false;
        for (Object target : targets) {
            if (target == infoDisplay) {
                isCoordReadout = true;
                break;
            }
        }

        if (!isCoordReadout) {
            return playerPos;
        }

        return shape.fold(playerPos);
    }

    private static Object[] coordReadoutDisplays() {
        Object[] resolved = coordReadoutDisplays;
        if (resolved != null) {
            return resolved;
        }

        try {
            Class<?> displays = Class.forName("xaero.hud.minimap.info.BuiltInInfoDisplays");
            resolved = new Object[] {
                    displays.getField("COORDINATES").get(null),
                    displays.getField("OVERWORLD_COORDINATES").get(null),
                    displays.getField("CHUNK_COORDINATES").get(null),
            };
        } catch (ReflectiveOperationException e) {
            LOGGER.info("[xaero-compat] info_fold_targets_missing error={}", e.toString());
            resolved = new Object[0];
        }

        coordReadoutDisplays = resolved;
        return resolved;
    }

    private XaeroFold() {
    }
}
