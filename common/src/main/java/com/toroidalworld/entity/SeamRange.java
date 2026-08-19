package com.toroidalworld.entity;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SeamRange {
    public static int manhattan(Entity levelSource, Vec3i from, Vec3i to) {
        WorldLoopTransformer transformer = transformerOf(levelSource);
        if (transformer == null) {
            return from.distManhattan(to);
        }

        return Math.abs(transformer.coords.x.foldDelta(to.getX() - from.getX()))
                + Math.abs(to.getY() - from.getY())
                + Math.abs(transformer.coords.z.foldDelta(to.getZ() - from.getZ()));
    }

    public static double sqr(Entity levelSource, Vec3i from, Vec3i to) {
        WorldLoopTransformer transformer = transformerOf(levelSource);
        if (transformer == null) {
            return from.distSqr(to);
        }

        return transformer.coords.sqrDistToBounds(
                from.getX(), from.getY(), from.getZ(),
                to.getX(), to.getY(), to.getZ());
    }

    public static double sqr(Level levelSource, Vec3i from, Vec3i to) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(levelSource);
        if (transformer == null) {
            return from.distSqr(to);
        }

        return transformer.coords.sqrDistToBounds(
                from.getX(), from.getY(), from.getZ(),
                to.getX(), to.getY(), to.getZ());
    }

    public static double sqr(Entity levelSource, Vec3 from, Position to) {
        WorldLoopTransformer transformer = transformerOf(levelSource);
        if (transformer == null) {
            return from.distanceToSqr(to.x(), to.y(), to.z());
        }

        return transformer.coords.sqrDistToBounds(from.x, from.y, from.z, to.x(), to.y(), to.z());
    }

    public static double sqr(Level levelSource, Vec3 from, Position to) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(levelSource);
        if (transformer == null) {
            return from.distanceToSqr(to.x(), to.y(), to.z());
        }

        return transformer.coords.sqrDistToBounds(from.x, from.y, from.z, to.x(), to.y(), to.z());
    }

    public static boolean closerThan(Entity levelSource, Vec3i from, Vec3i to, double distance) {
        return sqr(levelSource, from, to) < Mth.square(distance);
    }

    public static boolean closerThan(Entity levelSource, Vec3 from, Position to, double distance) {
        return sqr(levelSource, from, to) < Mth.square(distance);
    }

    public static boolean closerThan(Level levelSource, Vec3i from, Vec3i to, double distance) {
        return sqr(levelSource, from, to) < Mth.square(distance);
    }

    public static boolean closerToCenterThan(Entity levelSource, Vec3i from, Position to, double distance) {
        return closerThan(levelSource, Vec3.atCenterOf(from), to, distance);
    }

    private static @Nullable WorldLoopTransformer transformerOf(Entity levelSource) {
        return ((TransformerSource) levelSource).toroidal$wrappedTransformer();
    }

    private SeamRange() {
    }
}
