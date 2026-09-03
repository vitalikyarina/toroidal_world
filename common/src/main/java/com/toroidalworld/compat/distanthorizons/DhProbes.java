package com.toroidalworld.compat.distanthorizons;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.seibel.distanthorizons.core.level.IDhLevel;

public final class DhProbes {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile long lastRadiusCap = Long.MIN_VALUE;
    private static volatile long lastDetailCap = Long.MIN_VALUE;

    public static void radiusCapped(int configChunks, int capChunks) {
        long pair = ((long) configChunks << 32) | (capChunks & 0xFFFFFFFFL);
        if (pair != lastRadiusCap) {
            lastRadiusCap = pair;
            LOGGER.info("[dh-compat] radius_capped config_chunks={} cap_chunks={}", configChunks, capChunks);
        }
    }

    public static void detailCapped(byte expected, byte cap) {
        long pair = ((long) expected << 32) | (cap & 0xFFFFFFFFL);
        if (pair != lastDetailCap) {
            lastDetailCap = pair;
            LOGGER.info("[dh-compat] detail_capped expected={} cap={}", expected, cap);
        }
    }

    public static void repoShape(Object repo, IDhLevel level, boolean present) {
        String levelName = level == null ? "none" : level.getLevelWrapper().getDhIdentifier();
        LOGGER.info("[dh-compat] repo_shape repo={} level={} shape={}",
                repo.getClass().getSimpleName(), levelName, present ? "present" : "absent");
    }

    private DhProbes() {
    }
}
