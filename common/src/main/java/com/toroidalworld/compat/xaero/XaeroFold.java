package com.toroidalworld.compat.xaero;

import org.slf4j.Logger;

import com.toroidalworld.api.ToroidalShape;
import com.toroidalworld.api.ToroidalWorldClientApi;
import com.toroidalworld.client.ClientFrame;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

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

    public static double nearestElementCoord(Direction.Axis axis, double coord) {
        return ClientFrame.nearestToCamera(axis, coord);
    }

    public static int nearestWaypointBlock(Direction.Axis axis, int coord) {
        return (int) Math.round(ClientFrame.nearestToCamera(axis, coord));
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
