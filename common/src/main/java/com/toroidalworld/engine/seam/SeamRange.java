package com.toroidalworld.engine.seam;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SeamRange {
    public static int manhattan(Entity levelSource, Vec3i from, Vec3i to) {
        WorldFold transformer = transformerOf(levelSource);
        if (transformer == null) {
            return from.distManhattan(to);
        }

        BlockPos anchor = new BlockPos(from);
        return anchor.distManhattan(transformer.nearestCopy(anchor, new BlockPos(to)));
    }

    public static double sqr(@Nullable WorldFold fold, Vec3i from, Vec3i to) {
        if (fold == null) {
            return from.distSqr(to);
        }

        return fold.sqrDistance(
                from.getX(), from.getY(), from.getZ(),
                to.getX(), to.getY(), to.getZ());
    }

    public static double sqr(@Nullable WorldFold fold, Vec3 from, Position to) {
        if (fold == null) {
            return from.distanceToSqr(to.x(), to.y(), to.z());
        }

        return fold.sqrDistance(from.x, from.y, from.z, to.x(), to.y(), to.z());
    }

    public static double sqr(Entity levelSource, Vec3i from, Vec3i to) {
        return sqr(transformerOf(levelSource), from, to);
    }

    public static double sqr(Level levelSource, Vec3i from, Vec3i to) {
        return sqr(WorldLoopAttachments.wrappedTransformerOf(levelSource), from, to);
    }

    public static double sqr(Entity levelSource, Vec3 from, Position to) {
        return sqr(transformerOf(levelSource), from, to);
    }

    public static double sqr(Level levelSource, Vec3 from, Position to) {
        return sqr(WorldLoopAttachments.wrappedTransformerOf(levelSource), from, to);
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

    private static @Nullable WorldFold transformerOf(Entity levelSource) {
        return ((TransformerSource) levelSource).toroidal$wrappedTransformer();
    }

    private SeamRange() {
    }
}
