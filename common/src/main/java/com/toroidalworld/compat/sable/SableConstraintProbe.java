package com.toroidalworld.compat.sable;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toroidalworld.core.LogRateGate;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SableConstraintProbe {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final LogRateGate SPAN_GATE = new LogRateGate();

    public static void groupSpan(Level level, List<PhysicsPipelineBody> group, Vec3 centroid, boolean centroidOverBounds) {
        WorldFold fold = foldOf(level);
        if (fold == null || !SPAN_GATE.tryPass()) {
            return;
        }

        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        for (PhysicsPipelineBody body : group) {
            Vec3 position = positionOf(body);
            if (position != null) {
                minX = Math.min(minX, position.x);
                maxX = Math.max(maxX, position.x);
                minZ = Math.min(minZ, position.z);
                maxZ = Math.max(maxZ, position.z);
            }
        }

        LOGGER.info("[sable-compat] constraint_group_span bodies={} span_x_blocks={} span_z_blocks={} "
                        + "centroid_x_blocks={} centroid_z_blocks={} centroid_over_bounds={}",
                group.size(), maxX - minX, maxZ - minZ, centroid.x, centroid.z, centroidOverBounds);
    }

    private static @Nullable Vec3 positionOf(@Nullable PhysicsPipelineBody body) {
        if (body instanceof ServerSubLevel subLevel) {
            var position = subLevel.logicalPose().position();
            return new Vec3(position.x(), position.y(), position.z());
        }

        return null;
    }

    private static @Nullable WorldFold foldOf(@Nullable Level level) {
        return level == null ? null : WorldLoopAttachments.wrappedTransformerOfReader(level);
    }

    private SableConstraintProbe() {
    }
}
