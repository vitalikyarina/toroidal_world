package com.toroidalworld.compat.sable;

import org.joml.Vector3dc;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SableSeamDistance {
    public static double sqr(Level level, Vector3dc from, Vector3dc to, Operation<Double> original) {
        WorldFold fold = WorldLoopAttachments.wrappedTransformerOfReader(level);
        if (fold == null) {
            return original.call(from, to);
        }

        return fold.sqrDistance(from.x(), from.y(), from.z(), to.x(), to.y(), to.z());
    }

    public static double rectilinear(Level level, Vector3dc from, Vector3dc to, Operation<Double> original) {
        WorldFold fold = WorldLoopAttachments.wrappedTransformerOfReader(level);
        if (fold == null) {
            return original.call(from, to);
        }

        Vec3 delta = fold.foldDelta(new Vec3(from.x(), from.y(), from.z()), new Vec3(to.x(), to.y(), to.z()));
        return Math.max(Math.abs(delta.x), Math.max(Math.abs(delta.y), Math.abs(delta.z)));
    }

    private SableSeamDistance() {
    }
}
