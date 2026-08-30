package com.toroidalworld.compat.aeronautics;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toroidalworld.core.LogRateGate;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SpringSeamFrame {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final LogRateGate SEATED_GATE = new LogRateGate();

    public static Vector3d seat(@Nullable Level level, Vector3dc own, Vector3d partner) {
        WorldFold fold = level == null ? null : WorldLoopAttachments.wrappedTransformerOfReader(level);
        if (fold == null) {
            return partner;
        }

        Vec3 origin = new Vec3(own.x(), own.y(), own.z());
        Vec3 raw = new Vec3(partner.x, partner.y, partner.z);
        Vec3 seated = fold.nearestCopy(origin, raw);
        if (seated.x == raw.x && seated.z == raw.z) {
            return partner;
        }

        probe(origin, raw, seated);
        return new Vector3d(seated.x, seated.y, seated.z);
    }

    private static void probe(Vec3 origin, Vec3 raw, Vec3 seated) {
        if (!SEATED_GATE.tryPass()) {
            return;
        }

        LOGGER.info("[aeronautics-compat] spring_partner_seat own_x_blocks={} raw_partner_x_blocks={} "
                        + "seated_partner_x_blocks={} raw_gap_blocks={} seated_gap_blocks={}",
                origin.x, raw.x, seated.x, raw.distanceTo(origin), seated.distanceTo(origin));
    }

    private SpringSeamFrame() {
    }
}
