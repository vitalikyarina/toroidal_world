package com.toroidalworld.compat.sable;

import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.server.level.ServerLevel;

public final class SableTrackingRange {
    public static double sqrDistance(ServerLevel level, Vector3dc pose, double x, double y, double z, Operation<Double> original) {
        return sqrDistance(WorldLoopAttachments.wrappedTransformerOf(level), pose, x, y, z, original);
    }

    public static double sqrDistance(@Nullable WorldFold fold, Vector3dc pose, double x, double y, double z, Operation<Double> original) {
        if (fold == null) {
            return original.call(pose, x, y, z);
        }

        return fold.sqrDistance(pose.x(), pose.y(), pose.z(), x, y, z);
    }

    private SableTrackingRange() {
    }
}
