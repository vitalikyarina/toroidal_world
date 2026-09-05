package com.toroidalworld.compat.distanthorizons;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toroidalworld.api.ToroidalShape;
import com.seibel.distanthorizons.core.level.IDhLevel;

import net.minecraft.core.Direction;

public final class DhProbes {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NONE = "none";

    private static final Set<String> SEEN_KEY_PERIODS = ConcurrentHashMap.newKeySet();

    public static void keyPeriod(ToroidalShape shape, byte leafDetailLevel) {
        String widthX = widthValue(shape, Direction.Axis.X);
        String widthZ = widthValue(shape, Direction.Axis.Z);
        if (!SEEN_KEY_PERIODS.add(widthX + ":" + widthZ)) {
            return;
        }

        LOGGER.info("[dh-compat] key_period width_x_blocks={} period_x_blocks={} laps_x={}"
                + " width_z_blocks={} period_z_blocks={} laps_z={}",
                widthX, periodValue(shape, Direction.Axis.X, leafDetailLevel),
                lapsValue(shape, Direction.Axis.X, leafDetailLevel),
                widthZ, periodValue(shape, Direction.Axis.Z, leafDetailLevel),
                lapsValue(shape, Direction.Axis.Z, leafDetailLevel));
    }

    static String widthValue(ToroidalShape shape, Direction.Axis axis) {
        return shape.loops(axis) ? String.valueOf(shape.widthBlocks(axis)) : NONE;
    }

    static String periodValue(ToroidalShape shape, Direction.Axis axis, byte leafDetailLevel) {
        return shape.loops(axis) ? String.valueOf(DhFold.periodBlocks(shape, axis, leafDetailLevel)) : NONE;
    }

    static String lapsValue(ToroidalShape shape, Direction.Axis axis, byte leafDetailLevel) {
        return shape.loops(axis)
                ? String.valueOf(DhFold.periodBlocks(shape, axis, leafDetailLevel) / shape.widthBlocks(axis))
                : NONE;
    }

    public static void repoShape(Object repo, IDhLevel level, boolean present) {
        LOGGER.info("[dh-compat] repo_shape repo={} level={} shape={}",
                repo.getClass().getSimpleName(), levelName(level), present ? "present" : "absent");
    }

    private static String levelName(IDhLevel level) {
        return level == null ? NONE : level.getLevelWrapper().getDhIdentifier();
    }

    private DhProbes() {
    }
}
