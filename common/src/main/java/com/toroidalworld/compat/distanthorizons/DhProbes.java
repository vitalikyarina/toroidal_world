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

    private static final Set<String> SEEN_KEY_FOLDS = ConcurrentHashMap.newKeySet();

    private static volatile long lastRadiusCap = Long.MIN_VALUE;

    public static void radiusCapped(int configChunks, int capChunks) {
        long pair = ((long) configChunks << 32) | (capChunks & 0xFFFFFFFFL);
        if (pair != lastRadiusCap) {
            lastRadiusCap = pair;
            LOGGER.info("[dh-compat] radius_capped config_chunks={} cap_chunks={}", configChunks, capChunks);
        }
    }

    public static void keyFold(ToroidalShape shape, boolean folded) {
        int widthX = shape.widthBlocks(Direction.Axis.X);
        int widthZ = shape.widthBlocks(Direction.Axis.Z);
        if (!SEEN_KEY_FOLDS.add(folded + ":" + widthX + ":" + widthZ)) {
            return;
        }

        LOGGER.info("[dh-compat] key_fold folded={} width_x_blocks={} width_z_blocks={}", folded, widthX, widthZ);
    }

    public static void repoShape(Object repo, IDhLevel level, boolean present) {
        String levelName = level == null ? "none" : level.getLevelWrapper().getDhIdentifier();
        LOGGER.info("[dh-compat] repo_shape repo={} level={} shape={}",
                repo.getClass().getSimpleName(), levelName, present ? "present" : "absent");
    }

    private DhProbes() {
    }
}
