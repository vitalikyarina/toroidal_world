package com.toroidalworld.compat.aeronautics;

import org.jspecify.annotations.Nullable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class NavTargetSeamDistance {
    public static double sqrDistance(@Nullable Level level, Vec3 from, double x, double y, double z,
            Operation<Double> original) {
        WorldFold fold = level == null ? null : WorldLoopAttachments.wrappedTransformerOfReader(level);
        if (fold == null) {
            return original.call(from, x, y, z);
        }

        return fold.sqrDistance(from.x, from.y, from.z, x, y, z);
    }

    private NavTargetSeamDistance() {
    }
}
